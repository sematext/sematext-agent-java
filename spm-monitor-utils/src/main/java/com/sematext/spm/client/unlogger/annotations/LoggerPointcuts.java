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
package com.sematext.spm.client.unlogger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoggerPointcuts {

  /**
   * @return
   */
  String name();

  /**
   * Pointcuts, format described in {@link MethodPointcut}
   *
   * @return
   */
  String[] methods() default {};

  /**
   * @return
   */
  String[] constructors() default {};

  /**
   * @return
   */
  WholeClass[] classes() default {};

  /**
   * @return
   */
  String[] methodAnnotations() default {};

  /**
   * Class name patterns to ignore.
   *
   * @return
   */
  String[] ignorePatterns() default {};

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface WholeClass {
    String className();

    Visibility[] visibility() default { Visibility.PUBLIC };

    Inheritance inheretance() default Inheritance.NONE;

    public enum Visibility {
      PUBLIC, PROTECTED, PACKAGE, PRIVATE;
    }

    public enum Inheritance {
      UP, DOWN, NONE, ALL;
    }
  }
}
