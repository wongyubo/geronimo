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

package org.apache.geronimo.system.resolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactManager;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.repository.ListableRepository;
import org.apache.geronimo.system.serverinfo.ServerInfo;

/**
 * @version $Rev$ $Date$
 */
public class ExplicitDefaultArtifactResolver extends DefaultArtifactResolver {

    public ExplicitDefaultArtifactResolver(String versionMapLocation,
            ArtifactManager artifactManager,
            Collection repositories,
            ServerInfo serverInfo ) throws IOException {
        super(artifactManager, repositories, buildExplicitResolution(versionMapLocation, serverInfo));
    }

    private static Map buildExplicitResolution(String versionMapLocation, ServerInfo serverInfo) throws IOException {
        if (versionMapLocation == null) {
            return null;
        }
        File location = serverInfo == null? new File(versionMapLocation): serverInfo.resolve(versionMapLocation);
        FileInputStream in = new FileInputStream(location);
        Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        return propertiesToArtifactMap(properties);
    }

    private static Map propertiesToArtifactMap(Properties properties) {
        Map explicitResolution = new HashMap();
        for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            String resolvedString = (String) entry.getValue();
            //split the string ourselves since we wish to allow blank artifactIds.
            String[] parts = key.split("/", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid id: " + key);
            }
            Artifact source = new Artifact(parts[0], parts[1], (String)null, parts[3]);
            Artifact resolved = Artifact.create(resolvedString);
            explicitResolution.put(source,resolved);
        }
        return explicitResolution;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(ExplicitDefaultArtifactResolver.class, "ArtifactResolver");
        infoFactory.addAttribute("versionMapLocation", String.class, true, true);
        infoFactory.addReference("ArtifactManager", ArtifactManager.class, "ArtifactManager");
        infoFactory.addReference("Repositories", ListableRepository.class, "Repository");
        infoFactory.addReference("ServerInfo", ServerInfo.class, "GBean");
        infoFactory.addInterface(ArtifactResolver.class);

        infoFactory.setConstructor(new String[]{
                "versionMapLocation",
                "ArtifactManager",
                "Repositories",
                "ServerInfo"
        });


        GBEAN_INFO = infoFactory.getBeanInfo();

    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
