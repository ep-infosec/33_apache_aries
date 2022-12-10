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
package org.apache.aries.spifly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ProviderBundleTrackerCustomizerTest {

    private BaseActivator activator = new BaseActivator() {
        @Override
        public void start(BundleContext context) throws Exception {}
    };

    @Test
    public void testAddingRemovedBundle() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        sreg.unregister();
        EasyMock.expectLastCall();
        EasyMock.replay(sreg);

        BundleContext implBC = mockSPIBundleContext(sreg);
        Bundle implBundle = mockSPIBundle(implBC);

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);

        // Call removedBundle();
        customizer.removedBundle(implBundle, null, registrations);
        // sreg.unregister() should have been called.
        EasyMock.verify(sreg);
    }

    @Test
    public void testAddingBundleSPIBundle() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle spiBundle = mockSPIBundle(implBC);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, spiBundle);
        assertNull("The SpiFly bundle itself should be ignored", customizer.addingBundle(spiBundle, null));
    }

    @Test
    public void testAddingNonOptInBundle() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle implBundle = mockSPIBundle(implBC, null);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, null);
        assertNull("Bundle doesn't opt-in so should be ignored", customizer.addingBundle(implBundle, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddingBundleWithBundleClassPath() throws Exception {
        Bundle mediatorBundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(mediatorBundle.getBundleId()).andReturn(42l).anyTimes();
        EasyMock.replay(mediatorBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, mediatorBundle);

        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.eq("org.apache.aries.mytest.MySPI"),
                EasyMock.isA(ServiceFactory.class),
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(EasyMock.createNiceMock(ServiceRegistration.class)).times(3);
        EasyMock.replay(implBC);


        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(SpiFlyConstants.SPI_PROVIDER_HEADER, "*");
        headers.put(Constants.BUNDLE_CLASSPATH, ".,non-jar.jar,embedded.jar,embedded2.jar");
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();

        URL embeddedJar = getClass().getResource("/embedded.jar");
        assertNotNull("precondition", embeddedJar);
        EasyMock.expect(implBundle.getResource("embedded.jar")).andReturn(embeddedJar).anyTimes();
        URL embedded2Jar = getClass().getResource("/embedded2.jar");
        assertNotNull("precondition", embedded2Jar);
        EasyMock.expect(implBundle.getResource("embedded2.jar")).andReturn(embedded2Jar).anyTimes();
        URL dir = new URL("jar:" + embeddedJar + "!/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        EasyMock.expect(implBundle.findEntries((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean())).
            andReturn(null).anyTimes();

        ClassLoader cl = new URLClassLoader(new URL [] {embeddedJar}, getClass().getClassLoader());
        Class<?> clsA = cl.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2a");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2a")).andReturn(clsA).anyTimes();
        Class<?> clsB = cl.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2b");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl2.MySPIImpl2b")).andReturn(clsB).anyTimes();
        ClassLoader cl2 = new URLClassLoader(new URL [] {embedded2Jar}, getClass().getClassLoader());
        Class<?> clsC = cl2.loadClass("org.apache.aries.spifly.impl3.MySPIImpl3");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl3.MySPIImpl3")).andReturn(clsC).anyTimes();
        EasyMock.replay(implBundle);

        assertEquals("Precondition", 0, activator.findProviderBundles("org.apache.aries.mytest.MySPI").size());
        // Call addingBundle();
        List<ServiceRegistration> registrations = customizer.addingBundle(implBundle, null);
        Collection<Bundle> bundles = activator.findProviderBundles("org.apache.aries.mytest.MySPI");
        assertEquals(1, bundles.size());
        assertSame(implBundle, bundles.iterator().next());

        // The bc.registerService() call should now have been made
        EasyMock.verify(implBC);
    }

    @Test
    public void testMultipleProviderServices() throws Exception {
        BundleContext implBC = mockSPIBundleContext(EasyMock.createNiceMock(ServiceRegistration.class));
        Bundle implBundle = mockMultiSPIBundle(implBC);
        Bundle spiBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(spiBundle.getBundleId()).andReturn(25l).anyTimes();
        EasyMock.replay(spiBundle);

        ProviderBundleTrackerCustomizer customizer = new ProviderBundleTrackerCustomizer(activator, spiBundle);
        assertEquals(2, customizer.addingBundle(implBundle, null).size());
    }

    @SuppressWarnings("unchecked")
    private BundleContext mockSPIBundleContext(ServiceRegistration sreg) {
        BundleContext implBC = EasyMock.createMock(BundleContext.class);
        EasyMock.<Object>expect(implBC.registerService(
                EasyMock.anyString(),
                EasyMock.isA(ServiceFactory.class),
                (Dictionary<String,?>) EasyMock.anyObject())).andReturn(sreg).anyTimes();
        EasyMock.replay(implBC);
        return implBC;
    }

    private Bundle mockSPIBundle(BundleContext implBC) throws ClassNotFoundException {
        return mockSPIBundle(implBC, "*");
    }

    private Bundle mockSPIBundle(BundleContext implBC, String spiProviderHeader) throws ClassNotFoundException {
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        if (spiProviderHeader != null)
            headers.put(SpiFlyConstants.SPI_PROVIDER_HEADER, spiProviderHeader);
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();

        // List the resources found at META-INF/services in the test bundle
        URL dir = getClass().getResource("impl1/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        URL res = getClass().getResource("impl1/META-INF/services/org.apache.aries.mytest.MySPI");
        assertNotNull("precondition", res);
        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Collections.singleton(res))).anyTimes();
        Class<?> cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl1.MySPIImpl1");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl1.MySPIImpl1")).andReturn(cls).anyTimes();
        EasyMock.replay(implBundle);
        return implBundle;
    }

    private Bundle mockMultiSPIBundle(BundleContext implBC) throws ClassNotFoundException {
        Bundle implBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(implBundle.getBundleContext()).andReturn(implBC).anyTimes();

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(
                Constants.REQUIRE_CAPABILITY,
                "osgi.extender;filter:='(osgi.extender=osgi.serviceloader.registrar)'"
        );
        headers.put(
            Constants.PROVIDE_CAPABILITY,
            "osgi.serviceloader;osgi.serviceloader='org.apache.aries.mytest.MySPI2';register:='org.apache.aries.spifly.impl4.MySPIImpl4b';foo='bbb'," +
            "osgi.serviceloader;osgi.serviceloader='org.apache.aries.mytest.MySPI2';register:='org.apache.aries.spifly.impl4.MySPIImpl4c';foo='ccc'"
        );
        EasyMock.expect(implBundle.getHeaders()).andReturn(headers).anyTimes();

        // List the resources found at META-INF/services in the test bundle
        URL dir = getClass().getResource("impl4/META-INF/services");
        assertNotNull("precondition", dir);
        EasyMock.expect(implBundle.getResource("/META-INF/services")).andReturn(dir).anyTimes();
        URL resA = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI");
        assertNotNull("precondition", resA);
        URL resB = getClass().getResource("impl4/META-INF/services/org.apache.aries.mytest.MySPI2");
        assertNotNull("precondition", resB);
        EasyMock.expect(implBundle.findEntries("META-INF/services", "*", false)).andReturn(
                Collections.enumeration(Arrays.asList(resA, resB))).anyTimes();
        Class<?> cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4b")).andReturn(cls).anyTimes();
        cls = getClass().getClassLoader().loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c");
        EasyMock.<Object>expect(implBundle.loadClass("org.apache.aries.spifly.impl4.MySPIImpl4c")).andReturn(cls).anyTimes();
        EasyMock.replay(implBundle);
        return implBundle;
    }
}
