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
package com.sematext.spm.client.unlogger.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sematext.spm.client.unlogger.ConstructorPointcut;
import com.sematext.spm.client.unlogger.Logspect;
import com.sematext.spm.client.unlogger.MethodPointcut;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.WholeClassPointcut;
import com.sematext.spm.client.util.ReflectionUtils;
import com.sematext.spm.client.util.StringUtils;

public final class XMLInstrumentationDescriptorLoader {

  private static interface PointcutElementFactory {
    Pointcut create(Element pointcutElement) throws InstrumentationLoaderException;
  }

  private static enum PointcutElement {
    METHOD("method", new PointcutElementFactory() {
      @Override
      public Pointcut create(Element pointcutElement) throws InstrumentationLoaderException {
        final String signature = pointcutElement.getAttribute("signature");
        checkNotNull(signature, "'signature' attribute should be defined.");
        return MethodPointcut.FACTORY.make(signature);
      }
    }),
    CLASS("class", new PointcutElementFactory() {
      @Override
      public Pointcut create(Element pointcutElement) throws InstrumentationLoaderException {
        final String name = pointcutElement.getAttribute("name");
        checkNotNull(name, "'name' attribute should be defined.");
        return WholeClassPointcut.make(name);
      }
    }),
    CONSTRUCTOR("constructor", new PointcutElementFactory() {
      @Override
      public Pointcut create(Element pointcutElement) throws InstrumentationLoaderException {
        final String signature = pointcutElement.getAttribute("signature");
        checkNotNull(signature, "'signature' attribute should be defined.");
        return ConstructorPointcut.FACTORY.make(signature);
      }
    });
    private final String name;
    private final PointcutElementFactory factory;

    PointcutElement(String name, PointcutElementFactory factory) {
      this.name = name;
      this.factory = factory;
    }

    public static PointcutElementFactory getFactory(String name) {
      for (final PointcutElement element : PointcutElement.values()) {
        if (element.name.equals(name)) {
          return element.factory;
        }
      }
      return null;
    }
  }

  private static void checkNotNull(Object element, String message) throws InstrumentationLoaderException {
    if (element == null) {
      throw new InstrumentationLoaderException(message);
    }
  }

  private static Pointcut processPointcutElement(Element pointcutElement) throws InstrumentationLoaderException {
    final String name = pointcutElement.getTagName();
    PointcutElementFactory factory = PointcutElement.getFactory(name);
    checkNotNull(factory, "unknown pointcut element '" + name + "'");
    return factory.create(pointcutElement);
  }

  private final Class<? extends UnloggableLogger> unlogger;

  public XMLInstrumentationDescriptorLoader(Class<? extends UnloggableLogger> unlogger) {
    this.unlogger = unlogger;
  }

  private Logspect processPointcut(Element pointcut) throws InstrumentationLoaderException {
    final String name = pointcut.getAttribute("name");
    checkNotNull(name, "'name' attribute expected.");

    final String entryPoint = pointcut.getAttribute("entry-point");
    String transactionName = pointcut.getAttribute("transaction-name");
    if (StringUtils.isEmpty(transactionName)) {
      transactionName = null;
    }

    final NodeList pointcutElements = pointcut.getChildNodes();
    if (pointcutElements.getLength() == 0) {
      throw new InstrumentationLoaderException("at least one pointcut element expected");
    }

    final List<Pointcut> pointcuts = new ArrayList<Pointcut>();
    for (int i = 0; i < pointcutElements.getLength(); i++) {
      Node pointcutElement = pointcutElements.item(i);
      if (pointcutElement.getNodeType() == Node.ELEMENT_NODE) {
        pointcuts.add(processPointcutElement((Element) pointcutElement));
      }
    }
    final CustomPointcutOptions options = new CustomPointcutOptions(Boolean.parseBoolean(entryPoint), transactionName);
    return new Logspect(name, unlogger, pointcuts, ReflectionUtils.ClassValue.cv(CustomPointcutOptions.class, options));
  }

  public InstrumentationDescriptor load(final InputStream is) throws InstrumentationLoaderException {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = factory.newDocumentBuilder();
      final Document doc = builder.parse(is);
      doc.getDocumentElement().normalize();

      final String name = doc.getDocumentElement().getAttribute("name");
      checkNotNull(name, "'name' attribute missing.");

      final List<Logspect> logspects = new ArrayList<Logspect>();
      final NodeList pointcutElements = doc.getElementsByTagName("pointcut");
      for (int i = 0; i < pointcutElements.getLength(); i++) {
        Node node = pointcutElements.item(i);
        logspects.add(processPointcut((Element) node));
      }

      return new InstrumentationDescriptor(name, logspects);
    } catch (ParserConfigurationException e) {
      throw new InstrumentationLoaderException("Can't load descriptor.", e);
    } catch (SAXException e) {
      throw new InstrumentationLoaderException("Can't load descriptor.", e);
    } catch (IOException e) {
      throw new InstrumentationLoaderException("Can't load descriptor.", e);
    } catch (Exception e) {
      throw new InstrumentationLoaderException("Can't load descriptor.", e);
    }
  }

}
