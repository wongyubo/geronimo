/**
 *
 * Copyright 2004 The Apache Software Foundation
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

package org.apache.geronimo.connector;

import org.apache.geronimo.gbean.DynamicGBeanDelegate;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;

/**
 *
 *
 * @version $Revision: 1.3 $ $Date: 2004/03/09 18:02:02 $
 *
 * */
public class AdminObjectWrapper {

    public static final GBeanInfo GBEAN_INFO;

    private final Class adminObjectClass;

    private final DynamicGBeanDelegate delegate;
    private final Object adminObject;
    private final String name;

    //for use as endpoint
    public AdminObjectWrapper() {
        adminObjectClass = null;
        adminObject = null;
        delegate = null;
        name = null;
    }

    public AdminObjectWrapper(Class adminObjectClass, String name) throws IllegalAccessException, InstantiationException {
        this.adminObjectClass = adminObjectClass;
        adminObject = adminObjectClass.newInstance();
        delegate = new DynamicGBeanDelegate();
        delegate.addAll(adminObject);
        this.name = name;
    }

    public Class getAdminObjectClass() {
        return adminObjectClass;
    }

    public Object getProxy() {
        return adminObject;
    }

    public Object getId() {
        return name;
    }

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(AdminObjectWrapper.class.getName());
        infoFactory.addAttribute("AdminObjectClass", true);
        infoFactory.addAttribute("Name", true);
        infoFactory.setConstructor(new String[] {"AdminObjectClass", "Name"},
                new Class[] {Class.class, String.class});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
