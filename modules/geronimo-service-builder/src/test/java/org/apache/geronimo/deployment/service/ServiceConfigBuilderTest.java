/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.deployment.service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;

import javax.management.ObjectName;

import junit.framework.TestCase;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.deployment.FooBarBean;
import org.apache.geronimo.deployment.ModuleIDBuilder;
import org.apache.geronimo.deployment.NamespaceDrivenBuilder;
import org.apache.geronimo.deployment.xbeans.ModuleDocument;
import org.apache.geronimo.deployment.xbeans.ModuleType;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.ReferenceCollection;
import org.apache.geronimo.gbean.ReferenceCollectionListener;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.SimpleConfigurationManager;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactManager;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ListableRepository;

/**
 * @version $Rev$ $Date$
 */
public class ServiceConfigBuilderTest extends TestCase {

    private Environment parentEnvironment = new Environment();

    public void testNonService() throws Exception {
        URL url = getClass().getResource("/empty.jar");
        File file = new File(url.getPath());
        JarFile jar = new JarFile(file);
        assertTrue(file.exists());
        ServiceConfigBuilder builder = new ServiceConfigBuilder(parentEnvironment, null, new Jsr77Naming());
        assertNull(builder.getDeploymentPlan(null, jar, new ModuleIDBuilder()));
        jar.close();
    }

    public void testJavaBeanXmlAttribute() throws Exception {
        ReferenceCollection referenceCollection = new MockReferenceCollection();
        JavaBeanXmlAttributeBuilder javaBeanXmlAttributeBuilder = new JavaBeanXmlAttributeBuilder();
        //this is kind of cheating, we rely on the builder to iterate through existing members of the collection.
        referenceCollection.add(javaBeanXmlAttributeBuilder);
        Naming naming = new Jsr77Naming();
        NamespaceDrivenBuilder gbeanBuilder = new GBeanBuilder(referenceCollection, null);
//        ConfigurationBuilder serviceBuilder = new ServiceConfigBuilder(parentEnvironment, null, Collections.singleton(gbeanBuilder), naming);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final URL plan1 = cl.getResource("services/plan1.xml");
        ModuleDocument doc = ModuleDocument.Factory.parse(plan1);
        ModuleType plan = doc.getModule();
        File outFile = File.createTempFile("foo", "bar");
        outFile.delete();
        if (!outFile.mkdirs()) {
            fail("could not create temp dir");
        }
        try {

            Environment environment = EnvironmentBuilder.buildEnvironment(plan.getEnvironment());
            MockRepository mockRepository = new MockRepository();
            ArtifactManager artifactManager = new DefaultArtifactManager();
            ArtifactResolver artifactResolver = new DefaultArtifactResolver(artifactManager, Collections.singleton(mockRepository), null);
            ConfigurationManager configurationManager = new SimpleConfigurationManager(Collections.EMPTY_SET, artifactResolver, Collections.EMPTY_SET);
            AbstractName moduleName = naming.createRootName(environment.getConfigId(), "foo", "bar");
            DeploymentContext context = new DeploymentContext(outFile, null, environment, moduleName, ConfigurationModuleType.CAR, naming, configurationManager, Collections.singleton(mockRepository));

            gbeanBuilder.build(plan, context, context);
            Set gbeanNames = context.getGBeanNames();
            assertEquals(1, gbeanNames.size());
            AbstractName beanName = (AbstractName) gbeanNames.iterator().next();
            GBeanData data = context.getGBeanInstance(beanName);
            FooBarBean fooBarBean = (FooBarBean) data.getAttribute("fooBarBean");
            assertNotNull(fooBarBean);
            assertEquals("foo", fooBarBean.getFoo());
            assertEquals(10, fooBarBean.getBar());
            FooBarBean inner = fooBarBean.getBean();
            assertNotNull(inner);
            assertEquals("foo2", inner.getFoo());
            assertEquals(100, inner.getBar());
            assertNull(inner.getBean());
        } finally {
            recursiveDelete(outFile);
        }
    }

    private void recursiveDelete(File file) {

        File[] list = file.listFiles();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                File file1 = list[i];
                if (file1.isDirectory()) {
                    recursiveDelete(file1);
                }
            }
        }
        file.delete();
    }

    private static class MockRepository implements ListableRepository {
        public boolean contains(Artifact artifact) {
            return true;
        }

        public File getLocation(Artifact artifact) {
            return new File(".");
        }

        public LinkedHashSet getDependencies(Artifact artifact) {
            return new LinkedHashSet();
        }

        public SortedSet list() {
            return new TreeSet();
        }

        public SortedSet list(Artifact query) {
            SortedSet set = new TreeSet();
            if(query.getGroupId() != null && query.getArtifactId() != null && query.getVersion() != null && query.getType() == null) {
                set.add(new Artifact(query.getGroupId(), query.getArtifactId(), query.getVersion(), "jar"));
            }
            return set;
        }
    }
    private static class MockReferenceCollection extends ArrayList implements ReferenceCollection {

        public void addReferenceCollectionListener(ReferenceCollectionListener listener) {

        }

        public void removeReferenceCollectionListener(ReferenceCollectionListener listener) {

        }

        public ObjectName[] getMemberObjectNames() { return new ObjectName[0];}
    }
}