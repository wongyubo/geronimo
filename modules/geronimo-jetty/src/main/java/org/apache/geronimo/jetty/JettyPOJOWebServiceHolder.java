/**
 *
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
package org.apache.geronimo.jetty;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.security.jacc.PolicyContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.webservices.POJOWebServiceServlet;
import org.apache.geronimo.webservices.WebServiceContainer;
import org.apache.geronimo.webservices.WebServiceContainerInvoker;
import org.apache.geronimo.webservices.WebServiceContainerFactory;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHttpRequest;


/**
 * This is intended to hold the web service stack for an axis POJO web service.
 * It is starting life as a copy of JettyServletHolder.
 *
 * @version $Rev$ $Date$
 */
public class JettyPOJOWebServiceHolder extends ServletHolder implements GBeanLifecycle {
    private WebServiceContainer webServiceContainer;
    private Set servletMappings;
    private JettyServletRegistration context;
    private String pojoClassName;

    //todo consider interface instead of this constructor for endpoint use.
    public JettyPOJOWebServiceHolder() {

    }

    public JettyPOJOWebServiceHolder(String pojoClassName,
                                     String servletName,
                                     Map initParams,
                                     Integer loadOnStartup,
                                     Set servletMappings,
                                     WebServiceContainerFactory webServiceContainerFactory,
                                     ServletHolder previous,    //dependency for startup ordering
                                     JettyServletRegistration context) throws Exception {
        super(context == null ? null : context.getServletHandler(), servletName, POJOWebServiceServlet.class.getName(), null);
        //context will be null only for use as "default servlet info holder" in deployer.

        this.pojoClassName = pojoClassName;
        this.context = context;
        this.webServiceContainer = webServiceContainerFactory == null? null: webServiceContainerFactory.getWebServiceContainer();
        if (context != null) {
            putAll(initParams);
            if (loadOnStartup != null) {
                setInitOrder(loadOnStartup.intValue());
            }
            this.servletMappings = servletMappings;
        }
    }

    //todo how do we stop/destroy the servlet?
    //todo is start called twice???

    public String getServletName() {
        return getName();
    }

    /**
     * Service a request with this servlet.  Set the ThreadLocal to hold the
     * current JettyServletHolder.
     */
    public void handle(ServletRequest request, ServletResponse response)
            throws ServletException, UnavailableException, IOException {

        //  TODO There has to be some way to get this in on the Servlet's init method.
//        request.setAttribute(POJOWebServiceServlet.WEBSERVICE_CONTAINER, webServiceContainer);

        JettyServletHolder.setCurrentServletName(getServletName());
        PolicyContext.setHandlerData(ServletHttpRequest.unwrap(request));

        super.handle(request, response);
    }

    public void doStart() throws Exception {
        if (context != null) {
            Class pojoClass = context.getWebClassLoader().loadClass(pojoClassName);

            /* DMB: Hack! I really just want to override initServlet and give a reference of the WebServiceContainer to the servlet before we call init on it.
             * But this will have to do instead....
             */
            ServletContext servletContext = this.context.getServletHandler().getServletContext();

            // Make up an ID for the WebServiceContainer
            // put a reference the ID in the init-params
            // put the WebServiceContainer in the webapp context keyed by its ID
            String webServicecontainerID = getServletName() + WebServiceContainerInvoker.WEBSERVICE_CONTAINER + webServiceContainer.hashCode();
            put(WebServiceContainerInvoker.WEBSERVICE_CONTAINER, webServicecontainerID);
            servletContext.setAttribute(webServicecontainerID, webServiceContainer);

            // Same for the POJO Class
            String pojoClassID = getServletName() + POJOWebServiceServlet.POJO_CLASS + pojoClass.hashCode();
            put(POJOWebServiceServlet.POJO_CLASS, pojoClassID);
            servletContext.setAttribute(pojoClassID, pojoClass);

            //this now starts the servlet in the appropriate context
            //TODO check that we should not call this a servlet for jsr-77 benefit.
            context.registerServletHolder(this, getServletName(), this.servletMappings, null);
//            start();
        }
    }

    public void doStop() throws Exception {
    }

    public void doFail() {
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(JettyPOJOWebServiceHolder.class, NameFactory.SERVLET_WEB_SERVICE_TEMPLATE);
        //todo replace with interface
        infoBuilder.addInterface(ServletHolder.class);

        infoBuilder.addAttribute("pojoClassName", String.class, true);
        infoBuilder.addAttribute("servletName", String.class, true);
        infoBuilder.addAttribute("initParams", Map.class, true);
        infoBuilder.addAttribute("loadOnStartup", Integer.class, true);
        infoBuilder.addAttribute("servletMappings", Set.class, true);
        infoBuilder.addReference("WebServiceContainerFactory", WebServiceContainerFactory.class);
        infoBuilder.addReference("Previous", ServletHolder.class, NameFactory.SERVLET);
        infoBuilder.addReference("JettyServletRegistration", JettyServletRegistration.class);

        infoBuilder.setConstructor(new String[]{"pojoClassName",
                                                "servletName",
                                                "initParams",
                                                "loadOnStartup",
                                                "servletMappings",
                                                "WebServiceContainerFactory",
                                                "Previous",
                                                "JettyServletRegistration"});

        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }


}
