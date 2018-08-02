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
package com.sematext.spm.client.unlogger;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.unlogger.testclasses.Factory;

public class VvsELTest {

  @Test
  public void oneChainMethodTest() {
    Object val = VvsEL.eval(new Object() {
      public String texxa() {
        return "Hiiiii";
      }
    }, "texxa()");

    Assert.assertEquals("Hiiiii", val);
  }

  @Test
  public void twoChainMethodTest() {
    Object val = VvsEL.eval(new Object() {
      public Object texxa() {
        return new Object() {
          public Object teyya() {
            return "HiiiiiX";
          }
        };
      }
    }, "texxa()->teyya()");

    Assert.assertEquals("HiiiiiX", val);
  }

  @Test
  public void twoChainMethodWithParamsTest() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("x", 1);
    params.put("y", 2);

    Object val = VvsEL.eval(new Object() {
      public Object texxa() {
        return new Object() {
          public Object teyya(Integer x, Integer y) {
            return x + y;
          }
        };
      }
    }, "texxa()->teyya(java.lang.Integer x, java.lang.Integer y)", params);

    Assert.assertEquals(1 + 2, val);
  }

  private static class Texxa {

    public static Object teyya(Integer x, Integer y) {
      return x + y;
    }
  }

  @Test
  public void oneChainStaticMethodWithParamsTest() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("x", 1);
    params.put("y", 2);

    Object val = VvsEL.evalStatic(Texxa.class.getClassLoader(),
                                  "com.sematext.spm.client.unlogger.VvsELTest$Texxa#teyya(java.lang.Integer x, java.lang.Integer y)", params);

    Assert.assertEquals(1 + 2, val);
  }

  @Test
  public void fieldFirstTest() {
    Object packageVisible = Factory.makePackageVisible(17l);

    Object val = VvsEL.eval(packageVisible, "globalMemStoreLimit");

    Assert.assertEquals(17l, val);
  }

  @Test
  public void fieldSynteticTest() {

    class Tezza {

      class Teyya {
      }

      public Teyya teyya() {
        return new Teyya();
      }
    }

    Tezza root = new Tezza();
    Object val = VvsEL.eval(root.teyya(), "this$1");

    Assert.assertEquals(root, val);
  }

}
