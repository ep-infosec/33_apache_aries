/**
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly.dynamic;

import org.apache.aries.spifly.BaseActivator;
import org.apache.aries.spifly.SpiFlyConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class DynamicWeavingActivator extends BaseActivator implements BundleActivator {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration weavingHookService;

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        WeavingHook wh = new ClientWeavingHook(context, this);
        weavingHookService = context.registerService(WeavingHook.class.getName(), wh, null);

        super.start(context, SpiFlyConstants.SPI_CONSUMER_HEADER);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        weavingHookService.unregister();

        super.stop(context);
    }
}
