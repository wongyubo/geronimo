/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.apache.geronimo.kernel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.InstanceAlreadyExistsException;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.jmx.GBeanMBean;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.LocalConfigStore;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.gbean.jmx.DependencyService;

/**
 * The core of a Geronimo instance.
 * A Kernel is responsible for managing the Configurations that comprise a
 * Geronimo system and exposing them using JMX. Each Kernel is associated
 * with an MBeanServer that is used to register the Configurations themselves
 * and the MBeans they define.
 *
 * Dependencies between MBeans are handled by a dedicated DependencyService
 * that is responsible for tracking those dependencies and ensuring that the
 * dependent objects follow the appropriate lifecycle and receive appropriate
 * notifications.
 *
 * The Kernel also provides a ConfigurationStore which is used to stage
 * installed Configurations (providing a local filesystem based classpath) and
 * used hold the persistent state of each Configuration. This allows
 * Configurations to restart in he event of system failure.
 *
 * @version $Revision: 1.11 $ $Date: 2004/01/26 05:55:27 $
 */
public class Kernel implements Serializable, KernelMBean, NotificationBroadcaster {

    /**
     * The JMX name used by a Kernel to register itself when it boots.
     */
    public static final ObjectName KERNEL = JMXUtil.getObjectName("geronimo.boot:role=Kernel");

    /**
     * The JMX name of the DependencyService.
     */
    public static final ObjectName DEPENDENCY_SERVICE = JMXUtil.getObjectName("geronimo.boot:role=DependencyService2");

    /**
     * The JMX name of the ConfigurationStore.
     */
    public static final ObjectName CONFIG_STORE = JMXUtil.getObjectName("geronimo.boot:role=ConfigurationStore");

    private final String domainName;
    private final GBeanInfo storeInfo;
    private final File configStore;

    private transient Log log;
    private transient boolean running;
    private transient MBeanServer mbServer;
    private transient GBeanMBean storeGBean;
    private transient ConfigurationStore store;

    /**
     * Construct a Kernel using the specified JMX domain and supply the
     * information needed to create the ConfigurationStore.
     * @param domainName the domain name to be used for the JMX MBeanServer
     * @param storeInfo the info for the GBeanMBean to be used for the ConfigurationStore
     * @param configStore a local directory to be used by the ConfigurationStore;
     *                    this must be present and writable when the kernel is booted
     */
    public Kernel(String domainName, GBeanInfo storeInfo, File configStore) {
        this.domainName = domainName;
        this.storeInfo = storeInfo;
        this.configStore = configStore;
    }

    /**
     * Construct a Kernel which does not have a config store.
     * @param domainName
     */
    public Kernel(String domainName) {
        this(domainName, null, null);
    }

    /**
     * Get the MBeanServer used by this kernel
     * @return the MBeanServer used by this kernel
     */
    public MBeanServer getMBeanServer() {
        return mbServer;
    }

    public static ObjectName getConfigObjectName(URI configID) throws MalformedObjectNameException {
        return new ObjectName("geronimo.config:name=" + ObjectName.quote(configID.toString()));
    }

    /**
     * Install the CAR at the supplied URL into this kernel's store
     * @param source the URL of a CAR format archive
     * @throws java.io.IOException if the CAR could not be read
     * @throws org.apache.geronimo.kernel.config.InvalidConfigException if there is a configuration problem with the CAR
     */
    public void install(URL source) throws  IOException, InvalidConfigException {
        if (store == null) {
            throw new UnsupportedOperationException("Kernel does not have a ConfigurationStore");
        }
        store.install(source);
    }

    /**
     * Load the specified Configuration from the store into this Kernel
     * @param configID the unique id of the Configuration to load
     * @return the JMX ObjectName the Kernel registered the Configuration under
     * @throws org.apache.geronimo.kernel.config.NoSuchConfigException if the store does not contain the specified Configuratoin
     * @throws java.io.IOException if the Configuration could not be read from the store
     * @throws org.apache.geronimo.kernel.config.InvalidConfigException if the Configuration is not valid
     * @throws java.lang.UnsupportedOperationException if this kernel does not have a store
     */
    public ObjectName load(URI configID) throws NoSuchConfigException, IOException, InvalidConfigException {
        if (!running) {
            throw new IllegalStateException("Kernel is not running");
        }
        if (store == null) {
            throw new UnsupportedOperationException("Kernel does not have a ConfigurationStore");
        }

        GBeanMBean config = store.getConfig(configID);
        URL baseURL = store.getBaseURL(configID);
        return load(config, baseURL);
    }

    /**
     * Load the supplied Configuration into the Kernel and define its root using the specified URL.
     * @param config the GBeanMBean representing the Configuration
     * @param rootURL the URL to be used to resolve relative paths in the configuration
     * @return the JMX ObjectName the Kernel registered the Configuration under
     * @throws org.apache.geronimo.kernel.config.InvalidConfigException if the Configuration is not valid
     */
    public ObjectName load(GBeanMBean config, URL rootURL) throws InvalidConfigException {
        URI configID;
        try {
            configID = (URI) config.getAttribute("ID");
        } catch (Exception e) {
            throw new InvalidConfigException("Cannot get config ID", e);
        }
        ObjectName configName;
        try {
            configName = getConfigObjectName(configID);
        } catch (MalformedObjectNameException e) {
            throw new InvalidConfigException("Cannot convert ID to ObjectName: ", e);
        }
        load(config, rootURL, configName);
        return configName;
    }

    /**
     * Load the supplied Configuration into the Kernel and override the default JMX name.
     * This method should be used with discretion as it is possible to create
     * Configurations that cannot be located by management or monitoring tools.
     * @param config the GBeanMBean representing the Configuration
     * @param rootURL the URL to be used to resolve relative paths in the configuration
     * @param configName the JMX ObjectName to register the Configuration under
     * @throws org.apache.geronimo.kernel.config.InvalidConfigException if the Configuration is not valid
     */
    public void load(GBeanMBean config, URL rootURL, ObjectName configName) throws InvalidConfigException {
        if (!running) {
            throw new IllegalStateException("Kernel is not running");
        }
        try {
            mbServer.registerMBean(config, configName);
        } catch (Exception e) {
            throw new InvalidConfigException("Unable to register configuraton", e);
        }

        try {
            config.setAttribute("BaseURL", rootURL);
        } catch (Exception e) {
            try {
                mbServer.unregisterMBean(configName);
            } catch (Exception e1) {
                // ignore
            }
            throw new InvalidConfigException("Cannot set BaseURL", e);
        }

        log.info("Loaded Configuration " + configName);
    }

    /**
     * Unload the specified Configuration from the Kernel
     * @param configName the JMX name of the Configuration that should be unloaded
     * @throws org.apache.geronimo.kernel.config.NoSuchConfigException if the specified Configuration is not loaded
     */
    public void unload(ObjectName configName) throws NoSuchConfigException {
        if (!running) {
            throw new IllegalStateException("Kernel is not running");
        }
        try {
            mbServer.unregisterMBean(configName);
        } catch (InstanceNotFoundException e) {
            throw new NoSuchConfigException("No config registered: " + configName, e);
        } catch (MBeanRegistrationException e) {
            throw (IllegalStateException) new IllegalStateException("Error deregistering configuration " + configName).initCause(e);
        }
        log.info("Unloaded Configuration " + configName);
    }

    /**
     * Load a specific GBean into this kernel.
     * This is intended for applications that are embedding the kernel.
     * @param name the name to register the GBean under
     * @param gbean the GBean to register
     * @throws InstanceAlreadyExistsException if the name is already used
     * @throws InvalidConfigException if there is a problem during registration
     */
    public void loadGBean(ObjectName name, GBeanMBean gbean) throws InstanceAlreadyExistsException, InvalidConfigException {
        try {
            mbServer.registerMBean(gbean, name);
        } catch (MBeanRegistrationException e) {
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        } catch (NotCompliantMBeanException e) {
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        }
    }

    /**
     * Start a specific GBean.
     * @param name the GBean to start
     * @throws InstanceNotFoundException if the GBean could not be found
     */
    public void startGBean(ObjectName name) throws InstanceNotFoundException, InvalidConfigException {
        try {
            mbServer.invoke(name, "start", null, null);
        } catch (MBeanException e) {
            // start is not supposed to throw anything
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        } catch (ReflectionException e) {
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        }
    }

    /**
     * Stop a specific GBean.
     * @param name the GBean to stop
     * @throws InstanceNotFoundException if the GBean could not be found
     */
    public void stopGBean(ObjectName name) throws InstanceNotFoundException, InvalidConfigException {
        try {
            mbServer.invoke(name, "stop", null, null);
        } catch (MBeanException e) {
            // stop is not supposed to throw anything
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        } catch (ReflectionException e) {
            throw new InvalidConfigException("Invalid GBean configuration for " + name, e);
        }
    }

    /**
     * Unload a specific GBean.
     * This is intended for applications that are embedding the kernel.
     * @param name the name of the GBean to unregister
     * @throws InstanceNotFoundException if the GBean could not be found
     */
    public void unloadGBean(ObjectName name) throws InstanceNotFoundException {
        try {
            mbServer.unregisterMBean(name);
        } catch (MBeanRegistrationException e) {
            throw (IllegalStateException) new IllegalStateException("Error unloading GBean " + name).initCause(e);
        }
    }

    /**
     * Boot this Kernel, triggering the instanciation of the MBeanServer and
     * the registration of the DependencyService and ConfigurationStore
     * @throws java.lang.Exception if the boot fails
     */
    public void boot() throws Exception {
        if (running) {
            return;
        }
        log = LogFactory.getLog(Kernel.class.getName());
        log.info("Starting boot");
        mbServer = MBeanServerFactory.createMBeanServer(domainName);
        mbServer.registerMBean(this, KERNEL);
        mbServer.registerMBean(new DependencyService(), DEPENDENCY_SERVICE);
        if (storeInfo != null) {
            storeGBean = new GBeanMBean(storeInfo);
            storeGBean.setAttribute("root", configStore);
            mbServer.registerMBean(storeGBean, CONFIG_STORE);
            storeGBean.start();
            store = (ConfigurationStore) storeGBean.getTarget();
        }
        running = true;
        log.info("Booted");
    }

    /**
     * Shut down this kernel instance, unregistering the MBeans and releasing
     * the MBeanServer.
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        log.info("Starting kernel shutdown");

        store = null;
        try {
            if (storeGBean != null) {
                storeGBean.stop();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (storeGBean != null) {
                mbServer.unregisterMBean(CONFIG_STORE);
            }
        } catch (Exception e) {
            // ignore
        }
        storeGBean = null;

        try {
            mbServer.unregisterMBean(DEPENDENCY_SERVICE);
        } catch (Exception e) {
            // ignore
        }
        try {
            mbServer.unregisterMBean(KERNEL);
        } catch (Exception e) {
            // ignore
        }
        MBeanServerFactory.releaseMBeanServer(mbServer);
        mbServer = null;
        synchronized (this) {
            notify();
        }
        log.info("Kernel shutdown complete");
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Static entry point allowing a Kernel to be run from the command line.
     * Arguments are:
     * <li>the filename of the directory to use for the configuration store.
     *     This will be created if it does not exist.</li>
     * <li>the id of a configuation to load</li>
     * Once the Kernel is booted and the configuration is loaded, the process
     * will remain running until the shutdown() method on the kernel is
     * invoked or until the JVM exits.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: " + Kernel.class.getName() + " <config-store-dir> <config-id>");
            System.exit(1);
        }
        File storeDir = new File(args[0]);
        URI configID = null;
        try {
            configID = new URI(args[1]);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String domain = "geronimo";
        if (storeDir.exists()) {
            if (!storeDir.isDirectory() || !storeDir.canWrite()) {
                System.err.println("Store location is not a writable directory: " + storeDir);
                System.exit(1);
            }
        } else {
            if (!storeDir.mkdirs()) {
                System.err.println("Could not create store directory: " + storeDir);
                System.exit(1);
            }
        }

        final Kernel kernel = new Kernel(domain, LocalConfigStore.GBEAN_INFO, storeDir);
        Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Thread") {
            public void run() {
                kernel.shutdown();
            }
        });
        try {
            kernel.boot();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        try {
            kernel.load(configID);
        } catch (Exception e) {
            kernel.shutdown();
            e.printStackTrace();
            System.exit(2);
        }
        while (kernel.isRunning()) {
            try {
                synchronized (kernel) {
                    kernel.wait();
                }
            } catch (InterruptedException e) {
                // continue
            }
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[0];
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    }

}
