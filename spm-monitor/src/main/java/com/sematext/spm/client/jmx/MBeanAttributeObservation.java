/*
 * Licensed to Sematext Group, Inc
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Sematext Group, Inc licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sematext.spm.client.jmx;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.openmbean.CompositeDataSupport;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.unlogger.StrictMatcher;
import com.sematext.spm.client.unlogger.StrictMatcher.Result;

/**
 * Defines observation for MBean's attribute
 */
public abstract class MBeanAttributeObservation extends AttributeObservation<MBeanServerConnection> {
  private static final Log LOG = LogFactory.getLog(MBeanAttributeObservation.class);

  private static final String COMPLEX_ATTRIBUTE_MARKER = "complex:";
  private static final String EVAL_ATTRIBUTE_MARKER = "jmx_eval:";
  private static final String REFLECT_ATTRIBUTE_MARKER = "reflect:";
  private static final String CONST_ATTRIBUTE_MARKER = "const:";
  private static final Pattern FORMULA_EVAL_PATTERN = Pattern.compile("(.*?)([+-/*])(.*?)");
  private static final StrictMatcher CONST_EVAL_PATTERN = StrictMatcher.rootPattern("((\\$\\{.*?\\}-?)+)")
      .reparseGroupAsMap(1, "\\$\\{(.*?)\\}-?", 1, 1).make();

  private enum Eval {
    COMPLEX(COMPLEX_ATTRIBUTE_MARKER) {
      @Override
      protected Object concreteEval(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                    ObjectInstance discoveredObject, String attributeName, boolean optional)
          throws Exception {
        int indexOfLastPoint = attributeName.lastIndexOf('.');
        String complexAttributeName = attributeName.substring(0, indexOfLastPoint);
        String propertyName = attributeName.substring(indexOfLastPoint + 1);

        Object attr = mbeanServer.getAttribute(discoveredObject.getObjectName(), complexAttributeName);

        CompositeDataSupport complexAttribute = (CompositeDataSupport) attr;
        return complexAttribute.get(propertyName);
      }
    },
    REFLECT(REFLECT_ATTRIBUTE_MARKER) {
      @Override
      protected Object concreteEval(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                    ObjectInstance discoveredObject, String attributeName, boolean optional)
          throws Exception {

        final String parts[] = attributeName.split("[.]");
        final String reflectAttributeName = parts[0];
        final String invokeMethodName = parts[1];

        Object attr = mbeanServer.getAttribute(discoveredObject.getObjectName(), reflectAttributeName);

        final Method method = attr.getClass().getMethod(invokeMethodName);

        return method.invoke(attr);
      }
    },
    EVAL(EVAL_ATTRIBUTE_MARKER) {
      @Override
      protected Object concreteEval(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                    ObjectInstance discoveredObject, String attributeName, boolean optional)
          throws Exception {
        Result result = CONST_EVAL_PATTERN.match(attributeName);
        if (result != null) {
          return evalAsConst(context, result);
        }

        Matcher matcher = FORMULA_EVAL_PATTERN.matcher(attributeName);
        if (matcher.matches()) {
          return evalAsFormula(mbeanServer, context, discoveredObject, matcher, optional);
        }
        throw new IllegalStateException(
            "Now, only one type of formulas like (a*b) is supported, or constant evaluation as ${value}");
      }

      private Object evalAsFormula(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                   ObjectInstance discoveredObject, Matcher matcher, boolean optional)
          throws Exception {
        String left = matcher.group(1);
        String op = matcher.group(2);
        String right = matcher.group(3);

        Double leftVal = evalDouble(mbeanServer, context, discoveredObject, left, optional);
        Double rightVal = evalDouble(mbeanServer, context, discoveredObject, right, optional);
        if (leftVal == null || rightVal == null) {
          return null;
        }
        // We treat internal values as doubles,
        // but we cast return values to long
        // due to CounterAttribute/CounterValueHolder
        // expect long.
        if ("+".equals(op)) {
          return (long) (leftVal + rightVal);
        }
        if ("-".equals(op)) {
          return (long) (leftVal - rightVal);
        }

        if ("/".equals(op)) {
          return (long) (leftVal / rightVal);
        }

        if ("*".equals(op)) {
          return (long) (leftVal * rightVal);
        }

        // Can't happen
        throw new IllegalStateException();
      }

      private Object evalAsConst(Map<String, ?> context,
                                 Result result) {
        StringBuilder res = new StringBuilder();
        String delim = "";
        for (String key : (Set<String>) result.get(1, LinkedHashMap.class).keySet()) {
          res.append(delim);
          res.append(context.get(key));
          delim = "-";
        }
        return res.toString();
      }

      private Double evalDouble(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                ObjectInstance discoveredObject, String attributeName, boolean optional)
          throws Exception {
        Object val;
        if (attributeName.startsWith(CONST_ATTRIBUTE_MARKER)) {
          val = attributeName.substring(CONST_ATTRIBUTE_MARKER.length());
        } else {
          val = evaluate(mbeanServer, context, discoveredObject, attributeName, optional);
        }

        return val == null ? null : Double.valueOf(String.valueOf(val));
      }
    },
    SIMPLE(null) {
      @Override
      protected Object concreteEval(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                    ObjectInstance discoveredObject, String attributeName, boolean optional)
          throws Exception {
        return mbeanServer.getAttribute(discoveredObject.getObjectName(), attributeName);
      }
    };

    private final String marker;

    private Eval(String marker) {
      this.marker = marker;
    }

    protected boolean isMatch(String attributeName) {
      return marker == null ? true : attributeName.startsWith(marker);
    }

    /**
     * Implemented by concrete strategy
     *
     * @param mbeanServer
     * @param discoveredObject
     * @param attributeName
     * @return
     * @throws Exception
     */
    protected abstract Object concreteEval(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                           ObjectInstance discoveredObject, String attributeName, boolean optional)
        throws Exception;

    public final Object eval(final MBeanServerConnection mbeanServer, final Map<String, ?> context,
                             final ObjectInstance discoveredObject, final String attributeName, boolean optional)
        throws Exception {
      return concreteEval(mbeanServer, context, discoveredObject,
                          marker == null ? attributeName : attributeName.substring(marker.length()), optional);
    }

    private static Eval find(String eval) {
      for (Eval e : values()) {
        if (e.isMatch(eval)) {
          return e;
        }
      }
      return SIMPLE;
    }

    public static Object evaluate(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                  ObjectInstance discoveredObject, String attributeName, boolean optional)
        throws Exception {
      try {
        return find(attributeName).eval(mbeanServer, context, discoveredObject, attributeName, optional);
      } catch (AttributeNotFoundException ex) {
        if (LOG.isDebugEnabled()) {
          if (!optional) {
            StringBuilder attributesInfo = new StringBuilder();
            MBeanAttributeInfo[] attributes = mbeanServer.getMBeanInfo(discoveredObject.getObjectName())
                .getAttributes();
            for (MBeanAttributeInfo attribute : attributes) {
              attributesInfo.append("\n").append(attribute.toString());
            }
            LOG.debug("Can't read jmx attribute for bean " + discoveredObject.getObjectName().toString()
                          + " , existing attributes: " + attributesInfo + ", message: " + ex.getMessage());
          }
        }

        return null;
      }
    }

  }

  public MBeanAttributeObservation() {
  }

  public MBeanAttributeObservation(MBeanAttributeObservation original, String newAttributeName) {
    super(original, newAttributeName);
  }

  public abstract MBeanAttributeObservation getCopy(String newAttributeName);

  @Override
  // TODO Check for alternative solution to var-args
  public Object getValue(ObservationBean<?, ?> parentObservation, MBeanServerConnection data, Map<String, ?> context,
                         Object... additionalParams)
      throws StatsCollectionFailedException {
    final ObjectInstance instance = (ObjectInstance) additionalParams[0];
    Object measurement = readAttributeUsingPattern(data, context, instance);

    if (measurement == null) {
      return null;
    }

    return getMetricValue(parentObservation, measurement);
  }

  /**
   * Reads one attribute (obtained with getName() method) from a bean fetched from JMX matching name pattern in
   * observation.getObjectName(). In case more than one match is found, IllegalStateException is thrown
   *
   * @param mbeanServer
   * @param observation
   * @return
   * @throws StatsCollectionFailedException
   */
  private Object readAttributeUsingPattern(MBeanServerConnection mbeanServer, Map<String, ?> context,
                                           ObjectInstance observation) throws StatsCollectionFailedException {
    return readAttributeUsingPattern(mbeanServer, context, observation, getAttributeName(), isOptional());
  }

  /**
   * Reads one attribute (obtained with name provided with param attributeName) from a bean fetched from JMX matching
   * name pattern in observation.getObjectName(). In case more than one match is found, IllegalStateException is thrown.
   * <p/>
   * Parameter attributeName can be used for fetching of complex attributes by prefixing attribute name with "complex:"
   * and suffixing it with ".somePropertyName", like "complex:HeapMemoryUsage.used" which reads property "used" of
   * complex attribute (HeapMemoryUsage).
   * <p/>
   * Additionally, with "jmx_eval:" prefix simple evaluation rules can be specified to compute "virtual" columns. Like
   * eval:(a*b)
   *
   * @param mbeanServer
   * @param context
   * @param attributeName
   * @return
   * @throws StatsCollectionFailedException
   */
  private static Object readAttributeUsingPattern(final MBeanServerConnection mbeanServer, Map<String, ?> context,
                                                  final ObjectInstance discoveredObject, final String attributeName,
                                                  boolean optional) throws StatsCollectionFailedException {
    try {
      // for complex attributes, special logic
      return Eval.evaluate(mbeanServer, context, discoveredObject, attributeName, optional);
    } catch (Exception e) {
      LOG.error("Failed to fetch data for, objectName: " + discoveredObject.getObjectName() + ", attributeName: " +
                    attributeName + ", error: " + e.getMessage());
      throw new StatsCollectionFailedException("Failed to fetch data for, objectName: " +
                                                   discoveredObject.getObjectName() + ", attributeName: "
                                                   + attributeName, e);
    }
  }

}
