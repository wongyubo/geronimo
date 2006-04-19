/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.deployment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.config.SimpleConfigurationManager;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactManager;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.repository.Environment;

/**
 * @version $Rev:385232 $ $Date$
 */
public class DeploymentContext {
    private final File baseDir;
    private final File inPlaceConfigurationDir;
    private final ResourceContext resourceContext;
    private final byte[] buffer = new byte[4096];
    private final Map childConfigurationDatas = new LinkedHashMap();
    private final ConfigurationManager configurationManager;
    private final Configuration configuration;
    private final Naming naming;
    private final List additionalDeployment = new ArrayList();

    public DeploymentContext(File baseDir, File inPlaceConfigurationDir, Environment environment, ConfigurationModuleType moduleType, Naming naming, Collection repositories, Collection stores, ArtifactResolver artifactResolver) throws DeploymentException {
        this(baseDir, inPlaceConfigurationDir, environment,  moduleType, naming, createConfigurationManager(repositories, stores, artifactResolver));
    }

    public DeploymentContext(File baseDir, File inPlaceConfigurationDir, Environment environment, ConfigurationModuleType moduleType, Naming naming, ConfigurationManager configurationManager) throws DeploymentException {
        this.configurationManager = configurationManager;
        if (baseDir == null) throw new NullPointerException("baseDir is null");
        if (environment == null) throw new NullPointerException("environment is null");
        if (moduleType == null) throw new NullPointerException("type is null");

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        this.baseDir = baseDir;

        this.inPlaceConfigurationDir = inPlaceConfigurationDir;

        this.naming = naming;

        this.configuration = createTempConfiguration(environment, moduleType, baseDir, inPlaceConfigurationDir, configurationManager, naming);

        if (null == inPlaceConfigurationDir) {
            resourceContext = new CopyResourceContext(configuration, baseDir);
        } else {
            resourceContext = new InPlaceResourceContext(configuration, inPlaceConfigurationDir);
        }
    }

    private static ConfigurationManager createConfigurationManager(Collection repositories, Collection stores, ArtifactResolver artifactResolver) {
//        ArtifactManager artifactManager = new DefaultArtifactManager();
//        ArtifactResolver artifactResolver = new DefaultArtifactResolver(artifactManager, repositories, null);
        return new SimpleConfigurationManager(stores, artifactResolver, repositories);
    }

    private static Configuration createTempConfiguration(Environment environment, ConfigurationModuleType moduleType, File baseDir, File inPlaceConfigurationDir, ConfigurationManager configurationManager, Naming naming) throws DeploymentException {
        try {
            configurationManager.loadConfiguration(new ConfigurationData(moduleType, null, null, null, environment, baseDir, inPlaceConfigurationDir, naming));
            return configurationManager.getConfiguration(environment.getConfigId());
        } catch (Exception e) {
            throw new DeploymentException("Unable to create configuration for deployment", e);
        }
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public Artifact getConfigID() {
        return configuration.getId();
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getInPlaceConfigurationDir() {
        return inPlaceConfigurationDir;
    }

    public Naming getNaming() {
        return naming;
    }

    public GBeanData addGBean(String name, GBeanInfo gbeanInfo) throws GBeanAlreadyExistsException {
        if (name == null) throw new NullPointerException("name is null");
        if (gbeanInfo == null) throw new NullPointerException("gbean is null");
        GBeanData gbean = new GBeanData(gbeanInfo);
        configuration.addGBean(name, gbean);
        return gbean;
    }

    public void addGBean(GBeanData gbean) throws GBeanAlreadyExistsException {
        if (gbean == null) throw new NullPointerException("gbean is null");
        if (gbean.getAbstractName() == null) throw new NullPointerException("gbean.getAbstractName() is null");
        configuration.addGBean(gbean);
    }

    public Set getGBeanNames() {
        return new HashSet(configuration.getGBeans().keySet());
    }

    /**
     * @deprecated use findGBeans(pattern)
     */
    public Set listGBeans(AbstractNameQuery pattern) {
        return findGBeans(pattern);
    }

    public AbstractName findGBean(AbstractNameQuery pattern) throws GBeanNotFoundException {
        return configuration.findGBean(pattern);
    }

    public AbstractName findGBean(Set patterns) throws GBeanNotFoundException {
        return configuration.findGBean(patterns);
    }

    public LinkedHashSet findGBeans(AbstractNameQuery pattern) {
        return configuration.findGBeans(pattern);
    }

    public LinkedHashSet findGBeans(Set patterns) {
        return configuration.findGBeans(patterns);
    }

    public GBeanData getGBeanInstance(AbstractName name) throws GBeanNotFoundException {
        Map gbeans = configuration.getGBeans();
        GBeanData gbeanData = (GBeanData) gbeans.get(name);
        if (gbeanData == null) {
            throw new GBeanNotFoundException(name);
        }
        return gbeanData;
    }

    /**
     * Add a packed jar file into the deployment context and place it into the
     * path specified in the target path.  The newly added packed jar is added
     * to the classpath of the configuration.
     *
     * @param targetPath where the packed jar file should be placed
     * @param jarFile the jar file to copy
     * @throws IOException if there's a problem copying the jar file
     */
    public void addIncludeAsPackedJar(URI targetPath, JarFile jarFile) throws IOException {
        resourceContext.addIncludeAsPackedJar(targetPath, jarFile);
    }

    /**
     * Add a ZIP file entry into the deployment context and place it into the
     * path specified in the target path.  The newly added entry is added
     * to the classpath of the configuration.
     *
     * @param targetPath where the ZIP file entry should be placed
     * @param zipFile the ZIP file
     * @param zipEntry the ZIP file entry
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        resourceContext.addInclude(targetPath, zipFile, zipEntry);
    }

    /**
     * Add a file into the deployment context and place it into the
     * path specified in the target path.  The newly added file is added
     * to the classpath of the configuration.
     *
     * @param targetPath where the file should be placed
     * @param source     the URL of file to be copied
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, URL source) throws IOException {
        resourceContext.addInclude(targetPath, source);
    }

    /**
     * Add a file into the deployment context and place it into the
     * path specified in the target path.  The newly added file is added
     * to the classpath of the configuration.
     *
     * @param targetPath where the file should be placed
     * @param source     the file to be copied
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, File source) throws IOException {
        resourceContext.addInclude(targetPath, source);
    }

    /**
     * Import the classpath from a jar file's manifest.  The imported classpath
     * is crafted relative to <code>moduleBaseUri</code>.
     *
     * @param moduleFile    the jar file from which the manifest is obtained.
     * @param moduleBaseUri the base for the imported classpath
     * @throws DeploymentException if there is a problem with the classpath in
     *                             the manifest
     */
    public void addManifestClassPath(JarFile moduleFile, URI moduleBaseUri) throws DeploymentException {
        Manifest manifest;
        try {
            manifest = moduleFile.getManifest();
        } catch (IOException e) {
            throw new DeploymentException("Could not read manifest: " + moduleBaseUri);
        }

        if (manifest == null) {
            return;
        }
        String manifestClassPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        if (manifestClassPath == null) {
            return;
        }

        for (StringTokenizer tokenizer = new StringTokenizer(manifestClassPath, " "); tokenizer.hasMoreTokens();) {
            String path = tokenizer.nextToken();

            URI pathUri;
            try {
                pathUri = new URI(path);
            } catch (URISyntaxException e) {
                throw new DeploymentException("Invalid manifest classpath entry: module=" + moduleBaseUri + ", path=" + path);
            }

            if (!pathUri.getPath().endsWith(".jar")) {
                throw new DeploymentException("Manifest class path entries must end with the .jar extension (J2EE 1.4 Section 8.2): module=" + moduleBaseUri);
            }
            if (pathUri.isAbsolute()) {
                throw new DeploymentException("Manifest class path entries must be relative (J2EE 1.4 Section 8.2): moduel=" + moduleBaseUri);
            }

            try {
                URI targetUri = moduleBaseUri.resolve(pathUri);
                if (targetUri.getPath().endsWith("/")) throw new IllegalStateException("target path must not end with a '/' character: " + targetUri);
                configuration.addToClassPath(targetUri.toString());
            } catch (IOException e) {
                throw new DeploymentException(e);
            }
        }
    }

    public void addClass(URI targetPath, String fqcn, byte[] bytes) throws IOException, URISyntaxException {
        if (!targetPath.getPath().endsWith("/")) throw new IllegalStateException("target path must end with a '/' character: " + targetPath);

        String classFileName = fqcn.replace('.', '/') + ".class";

        File targetFile = getTargetFile(new URI(targetPath.toString() + classFileName));
        addFile(targetFile, new ByteArrayInputStream(bytes));

        configuration.addToClassPath(targetPath.toString());
    }

    public void addFile(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        resourceContext.addFile(targetPath, zipFile, zipEntry);
    }

    public void addFile(URI targetPath, URL source) throws IOException {
        resourceContext.addFile(targetPath, source);
    }

    public void addFile(URI targetPath, File source) throws IOException {
        resourceContext.addFile(targetPath, source);
    }

    public void addFile(URI targetPath, String source) throws IOException {
        resourceContext.addFile(targetPath, source);
    }

    private void addFile(File targetFile, InputStream source) throws IOException {
        targetFile.getParentFile().mkdirs();
        OutputStream out = null;
        try {
            out = new FileOutputStream(targetFile);
            int count;
            while ((count = source.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        } finally {
            DeploymentUtil.close(out);
        }
    }

    public File getTargetFile(URI targetPath) {
        return resourceContext.getTargetFile(targetPath);
    }

    public ClassLoader getClassLoader() throws DeploymentException {
        return configuration.getConfigurationClassLoader();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void flush() throws IOException{
        resourceContext.flush();
    }

    public void close() throws IOException, DeploymentException {
        if (configurationManager != null) {
            try {
                configurationManager.unloadConfiguration(configuration.getId());
            } catch (NoSuchConfigException ignored) {
                //ignore
            }
        }
    }

    public void addChildConfiguration(String moduleName, ConfigurationData configurationData) {
        childConfigurationDatas.put(moduleName, configurationData);
    }

    public ConfigurationData getConfigurationData() {
        ConfigurationData configurationData = new ConfigurationData(configuration.getModuleType(),
                new LinkedHashSet(configuration.getClassPath()),
                new ArrayList(configuration.getGBeans().values()),
                childConfigurationDatas,
                configuration.getEnvironment(),
                baseDir,
                inPlaceConfigurationDir,
                naming);

        for (Iterator iterator = additionalDeployment.iterator(); iterator.hasNext();) {
            ConfigurationData ownedConfiguration = (ConfigurationData) iterator.next();
            configurationData.addOwnedConfigurations(ownedConfiguration.getId());
        }
        
        return configurationData;
    }

    public void addAdditionalDeployment(ConfigurationData configurationData) {
        additionalDeployment.add(configurationData);
    }

    public List getAdditionalDeployment() {
        return additionalDeployment;
    }
}
