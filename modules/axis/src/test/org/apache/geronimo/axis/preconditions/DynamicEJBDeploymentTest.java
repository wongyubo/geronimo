/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geronimo.axis.preconditions;

import java.io.File;
import java.net.URI;
import java.util.jar.JarFile;

import javax.management.ObjectName;
import javax.naming.Reference;

import org.apache.geronimo.axis.AbstractTestCase;
import org.apache.geronimo.axis.AxisGeronimoUtils;
import org.apache.geronimo.axis.testUtils.AxisGeronimoConstants;
import org.apache.geronimo.axis.testUtils.J2EEManager;
import org.apache.geronimo.axis.testUtils.TestingUtils;
import org.apache.geronimo.gbean.jmx.GBeanMBean;
import org.apache.geronimo.j2ee.deployment.EARConfigBuilder;
import org.apache.geronimo.j2ee.deployment.ResourceReferenceBuilder;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.openejb.deployment.OpenEJBModuleBuilder;

/**
 * <p>This test case show the infomation about openEJB that we assumed. And the
 * simmlier code code is used in the real code. As the OpenEJB is developing and
 * rapidly changing this test case act as a notifier for saying things has chaged</p>
 */
public class DynamicEJBDeploymentTest extends AbstractTestCase {
    private static final String j2eeDomainName = "openejb.server";
    private static final String j2eeServerName = "TestOpenEJBServer";
    private static final ObjectName transactionManagerObjectName = JMXUtil.getObjectName(j2eeDomainName + ":type=TransactionManager");
    private static final ObjectName connectionTrackerObjectName = JMXUtil.getObjectName(j2eeDomainName + ":type=ConnectionTracker");
    private Kernel kernel;
    private J2EEManager j2eeManager;
    private URI defaultParentId;

    /**
     * @param testName
     */
    public DynamicEJBDeploymentTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        defaultParentId = new URI("org/apache/geronimo/Server");
        String str = System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
        if (str == null) {
            str = ":org.apache.geronimo.naming";
        } else {
            str = str + ":org.apache.geronimo.naming";
        }
        System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, str);
        kernel = new Kernel("blah");
        kernel.boot();
        TestingUtils.startJ2EEContinerAndAxisServlet(kernel);
    }

    private ResourceReferenceBuilder resourceReferenceBuilder = TestingUtils.RESOURCE_REFERANCE_BUILDER;
    public void testEJBJarDeploy() throws Exception {
        File jarFile = new File(outDir , "echo-jar/echo-ewsimpl.jar");
        
        URI defaultParentId = new URI("org/apache/geronimo/Server");
        OpenEJBModuleBuilder moduleBuilder = new OpenEJBModuleBuilder(null, defaultParentId, null);
        
        
        EARConfigBuilder earConfigBuilder =
                new EARConfigBuilder(defaultParentId,
                        new ObjectName(j2eeDomainName + ":j2eeType=J2EEServer,name=" + j2eeServerName),
                        transactionManagerObjectName,
                        connectionTrackerObjectName,
                        null,
                        null,
                        null,
                        moduleBuilder,
                        moduleBuilder,
                        null,
                        null,
                        resourceReferenceBuilder,
                        null,
                        null);

        
            File unpackedDir = new File(tempDir, "OpenEJBTest-ear-Unpacked");
            JarFile jarFileModules = null;
            System.out.println("**"+jarFile +"**");
            try {
                jarFileModules = new JarFile(jarFile);
                Object plan = earConfigBuilder.getDeploymentPlan(null, jarFileModules);
                earConfigBuilder.buildConfiguration(plan, jarFileModules, unpackedDir);
            } finally {
                if (jarFileModules != null) {
                    jarFileModules.close();
                }
            }
            ObjectName name = new ObjectName("geronimo.test:name=" + jarFile.getName());
            GBeanMBean gbean = AxisGeronimoUtils.loadConfig(unpackedDir);
            kernel.loadGBean(name,gbean);
            gbean.setAttribute("baseURL",unpackedDir.toURL());
            kernel.startGBean(name);
            
    }

    protected void tearDown() throws Exception {
        TestingUtils.stopJ2EEContinerAndAxisServlet(kernel);
        kernel.shutdown();
    }
}

