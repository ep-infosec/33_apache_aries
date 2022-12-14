/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.quiesce.manager;
 
import org.apache.aries.quiesce.participant.QuiesceParticipant;
import org.osgi.framework.Bundle;

/**
 * Callback that allows a {@link QuiesceParticipant} to alert the {@link QuiesceManager} that
 * bundles are quiesced (from the point of view of the participant)
 */
public interface QuiesceCallback
{
  /**
   * Notify the quiesce manager that the given bundles are quiesced 
   * (from the point of view of the calling participant)
   * @param bundlesQuiesced
   */
  public void bundleQuiesced(Bundle ... bundlesQuiesced);
}