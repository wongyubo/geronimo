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
package org.apache.geronimo.deployment.plugin.local;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.Target;
import javax.management.ObjectName;

import org.apache.geronimo.deployment.plugin.TargetImpl;
import org.apache.geronimo.deployment.plugin.TargetModuleIDImpl;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.kernel.jmx.KernelMBean;

/**
 * @version $Rev$ $Date$
 */
public class RedeployCommand extends AbstractDeployCommand {
    private static final String[] DEPLOY_SIG = {File.class.getName(), File.class.getName()};
    private static final String[] UNINSTALL_SIG = {URI.class.getName()};
    private final TargetModuleID[] modules;

    public RedeployCommand(KernelMBean kernel, TargetModuleID[] moduleIDList, File moduleArchive, File deploymentPlan) {
        super(CommandType.DISTRIBUTE, kernel, moduleArchive, deploymentPlan, null, null, false);
        this.modules = moduleIDList;
    }

    public RedeployCommand(KernelMBean kernel, TargetModuleID[] moduleIDList, InputStream moduleArchive, InputStream deploymentPlan) {
        super(CommandType.START, kernel, null, null, moduleArchive, deploymentPlan, true);
        this.modules = moduleIDList;
    }

    public void run() {
        ObjectName deployer = getDeployerName();
        if (deployer == null) {
            return;
        }

        try {
            if (spool) {
                if (moduleStream != null) {
                    moduleArchive = DeploymentUtil.createTempFile();
                    copyTo(moduleArchive, moduleStream);
                }
                if (deploymentStream != null) {
                    deploymentPlan = DeploymentUtil.createTempFile();
                    copyTo(deploymentPlan, deploymentStream);
                }
            }
            for (int i = 0; i < modules.length; i++) {
                TargetModuleIDImpl module = (TargetModuleIDImpl) modules[i];

                URI configID = URI.create(module.getModuleID());
                kernel.stopConfiguration(configID);

                TargetImpl target = (TargetImpl) module.getTarget();
                ObjectName storeName = target.getObjectName();
                kernel.invoke(storeName, "uninstall", new Object[]{configID}, UNINSTALL_SIG);

                doDeploy(deployer, module.getTarget());
            }
            complete("Completed");
        } catch (Exception e) {
            doFail(e);
        } finally {
            if (spool) {
                if (moduleArchive != null) {
                    moduleArchive.delete();
                }
                if (deploymentPlan != null) {
                    deploymentPlan.delete();
                }
            }
        }
    }
}
