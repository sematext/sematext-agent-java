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
package com.sematext.spm.client.observation;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public class SimpleExpressionCalculation implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(SimpleExpressionCalculation.class);
  private Operand operand1;
  private Operand operand2;
  private AttributeOperation operation;

  private interface Operand {
    Number getNumber(Map<String, Object> metrics);
  }

  private class EmptyOperand implements Operand {
    public Number getNumber(Map<String, Object> metrics) {
      return null;
    }

    @Override
    public String toString() {
      return "EmptyOperand{}";
    }
  }

  private class MetricOperand implements Operand {
    private String attributeOperandName;

    public MetricOperand(String attributeOperandName) {
      this.attributeOperandName = attributeOperandName;
    }

    public Number getNumber(Map<String, Object> metrics) {
      return (Number) metrics.get(attributeOperandName);
    }

    @Override
    public String toString() {
      return "MetricOperand{" +
          "attributeOperandName='" + attributeOperandName + '\'' +
          '}';
    }
  }

  private class ConstantOperand implements Operand {
    private Number operand;

    public ConstantOperand(Number operand) {
      this.operand = operand;
    }

    public Number getNumber(Map<String, Object> metrics) {
      return operand;
    }

    @Override
    public String toString() {
      return "ConstantOperand{" +
          "operand=" + operand +
          '}';
    }
  }

  public SimpleExpressionCalculation(String expression) {
    if (expression.startsWith("eval:")) {
      expression = expression.substring("eval:".length()).trim();
    }

    String[] expressionSplit = expression.split("[*/+-]");
    if (expressionSplit.length > 2) {
      throw new IllegalArgumentException("Expression not properly formed: " + expression);
    }
    String operator = expression.replace(expressionSplit[0], "");
    operator = expressionSplit.length == 1 ? "" : operator.replace(expressionSplit[1], "").trim();

    if (operator.equals("")) {
      operation = AttributeOperation.COPY;
    } else if (operator.equals("+")) {
      operation = AttributeOperation.ADD;
    } else if (operator.equals("-")) {
      operation = AttributeOperation.SUBTRACT;
    } else if (operator.equals("*")) {
      operation = AttributeOperation.MULTIPLY;
    } else if (operator.equals("/")) {
      operation = AttributeOperation.DIVIDE;
    } else {
      throw new IllegalArgumentException(
          "Expression operator not properly formed: " + expression + ", operator was: " + operator);
    }

    if (expressionSplit.length == 1) {
      operand1 = buildOperand(expressionSplit[0].trim());
      operand2 = buildOperand(null);
    } else {
      operand1 = buildOperand(expressionSplit[0].trim());
      operand2 = buildOperand(expressionSplit[1].trim());
    }
  }

  private Operand buildOperand(String operandValue) {
    if (StringUtils.isBlank(operandValue)) {
      return new EmptyOperand();
    }
    operandValue = extractOperandName(operandValue);
    Number operand1NumberValue = tryGetNumber(operandValue);
    if (operand1NumberValue != null) {
      return new ConstantOperand(operand1NumberValue);
    } else {
      return new MetricOperand(operandValue);
    }
  }

  private Number tryGetNumber(String candidate) {
    try {
      return Long.valueOf(candidate.trim());
    } catch (NumberFormatException lfe) {
      try {
        return Double.valueOf(candidate.trim());
      } catch (NumberFormatException dfe) {
        return null;
      }
    }
  }

  private String extractOperandName(String name) {
    String tmpName = name.trim();
    if (tmpName.startsWith("${")) {
      tmpName = tmpName.substring(2);
      if (tmpName.indexOf("}") == -1) {
        throw new IllegalArgumentException("Incorrectly formed operand name in: " + name);
      } else {
        tmpName = tmpName.substring(0, tmpName.indexOf("}"));
      }
    }

    return tmpName;
  }

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {
    Number operandValue1 = operand1.getNumber(metrics);
    Number operandValue2 = operand2.getNumber(metrics);

    /*if (operand2 instanceof MetricOperand) {
      LOG.info("Operand1:" + operand1 + " value " + operandValue1);
      LOG.info("Operand2:" + operand2 + " value " + operandValue2);
    }*/

    return operation.eval(operandValue1, operandValue2);
  }

  enum AttributeOperation implements Operation {
    COPY {
      @Override
      public Number eval(Number operand1, Number operand2) {
        if (operand1 instanceof Long || operand1 instanceof Double) {
          return operand1;
        }
        throw new UnsupportedOperationException(
            "Expression not supported for input type " + operand1.getClass().getName());
      }
    },
    DIVIDE {
      @Override
      public Number eval(Number operand1, Number operand2) {
        if (operand1 == null || operand2 == null) {
          return null;
        }

        if (operand1 instanceof Long && operand2 instanceof Long) {
          return Long.valueOf(operand1.longValue() / operand2.longValue());
        }
        if ((operand1 instanceof Long || operand1 instanceof Double) &&
            (operand2 instanceof Long || operand2 instanceof Double)) {
          return Double.valueOf(operand1.doubleValue() / operand2.doubleValue());
        }
        throw new UnsupportedOperationException(
            "Expression not supported for input type " + operand1.getClass().getName());
      }
    },
    MULTIPLY {
      @Override
      public Number eval(Number operand1, Number operand2) {
        if (operand1 == null || operand2 == null) {
          return null;
        }

        if (operand1 instanceof Long && operand2 instanceof Long) {
          return Long.valueOf(operand1.longValue() * operand2.longValue());
        }
        if ((operand1 instanceof Long || operand1 instanceof Double) &&
            (operand2 instanceof Long || operand2 instanceof Double)) {
          return Double.valueOf(operand1.doubleValue() * operand2.doubleValue());
        }
        throw new UnsupportedOperationException(
            "Expression not supported for input type " + operand1.getClass().getName());
      }
    },
    ADD {
      @Override
      public Number eval(Number operand1, Number operand2) {
        if (operand1 == null || operand2 == null) {
          return null;
        }

        if (operand1 instanceof Long && operand2 instanceof Long) {
          return Long.valueOf(operand1.longValue() + operand2.longValue());
        }
        if ((operand1 instanceof Long || operand1 instanceof Double) &&
            (operand2 instanceof Long || operand2 instanceof Double)) {
          return Double.valueOf(operand1.doubleValue() + operand2.doubleValue());
        }
        throw new UnsupportedOperationException(
            "Expression not supported for input type " + operand1.getClass().getName());
      }
    },
    SUBTRACT {
      @Override
      public Number eval(Number operand1, Number operand2) {
        if (operand1 == null || operand2 == null) {
          return null;
        }

        if (operand1 instanceof Long && operand2 instanceof Long) {
          return Long.valueOf(operand1.longValue() - operand2.longValue());
        }
        if ((operand1 instanceof Long || operand1 instanceof Double) &&
            (operand2 instanceof Long || operand2 instanceof Double)) {
          return Double.valueOf(operand1.doubleValue() - operand2.doubleValue());
        }
        throw new UnsupportedOperationException(
            "Expression not supported for input type " + operand1.getClass().getName());
      }
    }
  }
}

interface Operation {
  Number eval(Number operand1, Number operand2);
}
