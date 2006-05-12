/**
 *
 * Copyright 2006 The Apache Software Foundation
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

package org.apache.geronimo.deployment.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.config.PersistentConfigurationList;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.system.main.CommandLine;
import org.apache.geronimo.system.serverinfo.ServerInfo;

/**
 * @version $Rev:$ $Date:$
 */
public class LocalServer extends CommandLine {

    public LocalServer(String configListLocation) throws Exception {
        startKernel(Artifact.create("geronimo/j2ee-system//car"));
        Runtime.getRuntime().addShutdownHook(new Thread("Geronimo shutdown thread") {
            public void run() {
                getKernel().shutdown();
            }
        });
        List configs = getConfigurationList(configListLocation);
        loadConfigurations(configs);
    }

    public Kernel getKernel() {
        return super.getKernel();
    }

    protected List getConfigurationList(String path) throws GBeanNotFoundException, IOException {
        ServerInfo serverInfo = (ServerInfo) getKernel().getGBean(ServerInfo.class);
        File configFile = serverInfo.resolve(path);
        List modules = new ArrayList();
        BufferedReader in = new BufferedReader(new FileReader(configFile));
        try {
            String artifactString;
            while ((artifactString = in.readLine()) != null) {
                artifactString = artifactString.trim();
                if (!artifactString.startsWith("#") && artifactString.length() > 0) {
                    Artifact artifact = Artifact.create(artifactString);
                    modules.add(artifact);
                }
            }
        } finally {
            in.close();
        }
        return modules;
    }

}
