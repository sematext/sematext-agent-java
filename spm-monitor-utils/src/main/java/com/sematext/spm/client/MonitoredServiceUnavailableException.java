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
package com.sematext.spm.client;

/**
 * Special case of StatsCollectionFailedException, so surrounding logic may shape recovery mechanism, logging, etc.
 * according to different cause of stats collection failure.
 */
public class MonitoredServiceUnavailableException extends StatsCollectionFailedException {
  private static final long serialVersionUID = -1120911672286642905L;

  public MonitoredServiceUnavailableException(String message) {
    super(message);
  }

}
