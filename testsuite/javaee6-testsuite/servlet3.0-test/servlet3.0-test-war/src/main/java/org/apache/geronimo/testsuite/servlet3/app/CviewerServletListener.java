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

package org.apache.geronimo.testsuite.servlet3.app;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

@WebListener()
public class CviewerServletListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext sc = sce.getServletContext();
		// Regist servlet
		ServletRegistration sr = sc.addServlet("ClassViewer",
				"org.apache.geronimo.testsuite.servlet3.app.CViewerServlet");
		sr.addMapping("/ClassViewer");

		// Register Filter
		FilterRegistration fr = sc.addFilter("CViewerFilter",
				"org.apache.geronimo.testsuite.servlet3.app.CviewerFilter");
		fr.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true,
				"ClassViewer");

	}

	public void contextDestroyed(ServletContextEvent sce) {
//		throw new UnsupportedOperationException("Not supported yet.");
	}
}
