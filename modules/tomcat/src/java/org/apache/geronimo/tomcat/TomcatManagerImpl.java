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
package org.apache.geronimo.tomcat;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.net.URISyntaxException;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import org.apache.geronimo.management.geronimo.WebManager;
import org.apache.geronimo.gbean.GBeanQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.geronimo.j2ee.management.impl.Util;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.config.EditableConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tomcat implementation of the WebManager management API.  Knows how to
 * manipulate other Tomcat objects for management purposes.
 *
 * @version $Rev$ $Date$
 */
public class TomcatManagerImpl implements WebManager {
    private final static Log log = LogFactory.getLog(TomcatManagerImpl.class);
    private final Kernel kernel;

    public TomcatManagerImpl(Kernel kernel) {
        this.kernel = kernel;
    }

    public String getProductName() {
        return "Tomcat";
    }

    /**
     * Creates a new connector, and returns the ObjectName for it.  Note that the connector may well require further
     * customization before being fully functional (e.g. SSL settings for a secure connector).  This may need to be done
     * before starting the resulting connector.
     *
     * @param containerObjectName The ObjectName of the container that the connector should be added to
     * @param uniqueName          A name fragment that's unique to this connector
     * @param protocol            The protocol that the connector should use
     * @param host                The host name or IP that the connector should listen on
     * @param port                The port that the connector should listen on
     * @return The ObjectName of the new connector.
     */
    public String addConnector(String containerObjectName, String uniqueName, String protocol, String host, int port) {
        ObjectName container;
        try {
            container = ObjectName.getInstance(containerObjectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid web container ObjectName '"+containerObjectName+"'");
        }
        ObjectName name = getConnectorName(container, protocol, uniqueName);
        GBeanData connector;
        if(protocol.equals(PROTOCOL_HTTP)) {
            connector = new GBeanData(name, ConnectorGBean.GBEAN_INFO);
        } else if(protocol.equals(PROTOCOL_HTTPS)) {
            connector = new GBeanData(name, HttpsConnectorGBean.GBEAN_INFO);
            GBeanQuery query = new GBeanQuery(null, ServerInfo.class.getName());
            Set set = kernel.listGBeans(query);
            connector.setReferencePattern("ServerInfo", (ObjectName)set.iterator().next());
            //todo: default HTTPS settings
        } else if(protocol.equals(PROTOCOL_AJP)) {
            connector = new GBeanData(name, ConnectorGBean.GBEAN_INFO);
        } else {
            throw new IllegalArgumentException("Invalid protocol '"+protocol+"'");
        }
        connector.setAttribute("protocol", protocol);
        connector.setAttribute("host", host);
        connector.setAttribute("port", new Integer(port));
        connector.setAttribute("maxThreads", new Integer(50));
        connector.setAttribute("acceptQueueSize", new Integer(100));
        connector.setReferencePattern(ConnectorGBean.CONNECTOR_CONTAINER_REFERENCE, container);
        connector.setAttribute("name", uniqueName);
        EditableConfigurationManager mgr = ConfigurationUtil.getEditableConfigurationManager(kernel);
        if(mgr != null) {
            try {
                ObjectName config = Util.getConfiguration(kernel, container);
                mgr.addGBeanToConfiguration(Configuration.getConfigurationID(config), connector, false);
                return name.getCanonicalName();
            } catch (InvalidConfigException e) {
                log.error("Unable to add GBean", e);
                return null;
            } catch (URISyntaxException e) {
                log.error("Should never happen", e);
                return null;
            } finally {
                ConfigurationUtil.releaseConfigurationManager(kernel, mgr);
            }
        } else {
            log.warn("The ConfigurationManager in the kernel does not allow editing");
            return null;
        }
    }

    /**
     * Gets the network containers.
     */
    public String[] getContainers() {
        GBeanQuery query = new GBeanQuery(null, TomcatWebContainer.class.getName());
        Set names = kernel.listGBeans(query);
        String[] result = new String[names.size()];
        int i = 0;
        for (Iterator it = names.iterator(); it.hasNext();) {
            ObjectName name = (ObjectName) it.next();
            result[i++] = name.getCanonicalName();
        }
        return result;
    }

    /**
     * Gets the protocols which this container can configure connectors for.
     */
    public String[] getSupportedProtocols() {
        return new String[]{PROTOCOL_HTTP, PROTOCOL_HTTPS, PROTOCOL_AJP};
    }

    /**
     * Removes a connector.  This shuts it down if necessary, and removes it from the server environment.  It must be a
     * connector that uses this network technology.
     */
    public void removeConnector(String objectName) {
        ObjectName name = null;
        try {
            name = ObjectName.getInstance(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid object name '"+objectName+"': "+e.getMessage());
        }
        try {
            GBeanInfo info = kernel.getGBeanInfo(name);
            boolean found = false;
            Set intfs = info.getInterfaces();
            for (Iterator it = intfs.iterator(); it.hasNext();) {
                String intf = (String) it.next();
                if(intf.equals(TomcatWebConnector.class.getName())) {
                    found = true;
                }
            }
            if(!found) {
                throw new GBeanNotFoundException(name);
            }
            ObjectName config = Util.getConfiguration(kernel, name);
            EditableConfigurationManager mgr = ConfigurationUtil.getEditableConfigurationManager(kernel);
            if(mgr != null) {
                try {
                    mgr.removeGBeanFromConfiguration(Configuration.getConfigurationID(config), name);
                } catch (InvalidConfigException e) {
                    log.error("Unable to add GBean", e);
                } catch (URISyntaxException e) {
                    log.error("Should never happen", e);
                } finally {
                    ConfigurationUtil.releaseConfigurationManager(kernel, mgr);
                }
            } else {
                log.warn("The ConfigurationManager in the kernel does not allow editing");
            }
        } catch (GBeanNotFoundException e) {
            log.warn("No such GBean '"+objectName+"'"); //todo: what if we want to remove a failed GBean?
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * Gets the ObjectNames of any existing connectors for this network technology for the specified protocol.
     *
     * @param protocol A protocol as returned by getSupportedProtocols
     */
    public String[] getConnectors(String protocol) {
        GBeanQuery query = new GBeanQuery(null, TomcatWebConnector.class.getName());
        Set names = kernel.listGBeans(query);
        List result = new ArrayList();
        for (Iterator it = names.iterator(); it.hasNext();) {
            ObjectName name = (ObjectName) it.next();
            try {
                if(kernel.getAttribute(name, "protocol").equals(protocol)) {
                    result.add(name.getCanonicalName());
                }
            } catch (Exception e) {
                log.error("Unable to check the protocol for a connector", e);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public String getAccessLog(String containerObjectName) {
        GBeanQuery query = new GBeanQuery(null, TomcatLogManager.class.getName());
        Set names = kernel.listGBeans(query);
        if(names.size() == 0) {
            return null;
        } else if(names.size() > 1) {
            throw new IllegalStateException("Should not be more than one Tomcat access log manager");
        }
        return ((ObjectName)names.iterator().next()).getCanonicalName();
    }

    /**
     * Gets the ObjectNames of any existing connectors associated with this network technology.
     */
    public String[] getConnectors() {
        GBeanQuery query = new GBeanQuery(null, TomcatWebConnector.class.getName());
        Set names = kernel.listGBeans(query);
        String[] result = new String[names.size()];
        int i=0;
        for (Iterator it = names.iterator(); it.hasNext();) {
            ObjectName name = (ObjectName) it.next();
            result[i++] = name.getCanonicalName();
        }
        return result;
    }

    /**
     * Gets the ObjectNames of any existing connectors for the specified container for the specified protocol.
     *
     * @param protocol A protocol as returned by getSupportedProtocols
     */
    public String[] getConnectorsForContainer(String containerObjectName, String protocol) {
        if(protocol == null) {
            return getConnectorsForContainer(containerObjectName);
        }
        try {
            ObjectName containerName = ObjectName.getInstance(containerObjectName);
            List results = new ArrayList();
            GBeanQuery query = new GBeanQuery(null, TomcatWebConnector.class.getName());
            Set set = kernel.listGBeans(query); // all Jetty connectors
            for (Iterator it = set.iterator(); it.hasNext();) {
                ObjectName name = (ObjectName) it.next(); // a single Jetty connector
                GBeanData data = kernel.getGBeanData(name);
                Set refs = data.getReferencePatterns(ConnectorGBean.CONNECTOR_CONTAINER_REFERENCE);
                for (Iterator refit = refs.iterator(); refit.hasNext();) {
                    ObjectName ref = (ObjectName) refit.next();
                    boolean match = false;
                    if(ref.isPattern()) {
                        Set matches = kernel.listGBeans(ref);
                        if(matches.size() != 1) {
                            log.error("Unable to compare a connector->container reference that's a pattern to a fixed container name: "+ref.getCanonicalName());
                        } else {
                            ref = (ObjectName)matches.iterator().next();
                            if(ref.equals(containerName)) {
                                match = true;
                            }
                        }
                    } else {
                        if(ref.equals(containerName)) {
                            match = true;
                        }
                    }
                    if(match) {
                        try {
                            String testProtocol = (String) kernel.getAttribute(name, "protocol");
                            if(testProtocol != null && testProtocol.equals(protocol)) {
                                results.add(name.getCanonicalName());
                            }
                        } catch (Exception e) {
                            log.error("Unable to look up protocol for connector '"+name+"'",e);
                        }
                        break;
                    }
                }
            }
            return (String[]) results.toArray(new String[results.size()]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to look up connectors for Jetty container '"+containerObjectName+"': "+e);
        }
    }

    /**
     * Gets the ObjectNames of any existing connectors for the specified container.
     */
    public String[] getConnectorsForContainer(String containerObjectName) {
        try {
            ObjectName containerName = ObjectName.getInstance(containerObjectName);
            List results = new ArrayList();
            GBeanQuery query = new GBeanQuery(null, TomcatWebConnector.class.getName());
            Set set = kernel.listGBeans(query); // all Jetty connectors
            for (Iterator it = set.iterator(); it.hasNext();) {
                ObjectName name = (ObjectName) it.next(); // a single Jetty connector
                GBeanData data = kernel.getGBeanData(name);
                Set refs = data.getReferencePatterns(ConnectorGBean.CONNECTOR_CONTAINER_REFERENCE);
                for (Iterator refit = refs.iterator(); refit.hasNext();) {
                    ObjectName ref = (ObjectName) refit.next();
                    if(ref.isPattern()) {
                        Set matches = kernel.listGBeans(ref);
                        if(matches.size() != 1) {
                            log.error("Unable to compare a connector->container reference that's a pattern to a fixed container name: "+ref.getCanonicalName());
                        } else {
                            ref = (ObjectName)matches.iterator().next();
                            if(ref.equals(containerName)) {
                                results.add(name.getCanonicalName());
                                break;
                            }
                        }
                    } else {
                        if(ref.equals(containerName)) {
                            results.add(name.getCanonicalName());
                            break;
                        }
                    }
                }
            }
            return (String[]) results.toArray(new String[results.size()]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to look up connectors for Jetty container '"+containerObjectName+"': "+e);
        }
    }

    private ObjectName getConnectorName(ObjectName container, String protocol, String uniqueName) {
        Hashtable table = new Hashtable();
        table.put(NameFactory.J2EE_APPLICATION, container.getKeyProperty(NameFactory.J2EE_APPLICATION));
        table.put(NameFactory.J2EE_SERVER, container.getKeyProperty(NameFactory.J2EE_SERVER));
        table.put(NameFactory.J2EE_MODULE, container.getKeyProperty(NameFactory.J2EE_MODULE));
        table.put(NameFactory.J2EE_TYPE, container.getKeyProperty(NameFactory.J2EE_TYPE));
        table.put(NameFactory.J2EE_NAME, "TomcatWebConnector-"+protocol+"-"+uniqueName);
        try {
            return ObjectName.getInstance(container.getDomain(), table);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Never should have failed: "+e.getMessage());
        }
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("Tomcat Web Manager", TomcatManagerImpl.class);
        infoFactory.addAttribute("kernel", Kernel.class, false);
        infoFactory.addInterface(WebManager.class);
        infoFactory.setConstructor(new String[] {"kernel"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
