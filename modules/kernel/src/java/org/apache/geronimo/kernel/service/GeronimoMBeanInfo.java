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
package org.apache.geronimo.kernel.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

import org.apache.geronimo.kernel.service.ParserUtil;
import org.apache.geronimo.kernel.service.GeronimoAttributeInfo;

/**
 * Describes a GeronimoMBean.  This extension allows the properties to be mutable during setup,
 * and once the MBean is deployed an imutable copy of will be made.  This class also adds support for multi target
 * POJOs under the MBean.
 *
 * @version $Revision: 1.1 $ $Date: 2003/09/08 04:38:35 $
 */
public final class GeronimoMBeanInfo extends MBeanInfo {
    /**
     * The key for the default target.
     */
    private final static String DEFAULT_TARGET_NAME = "default";

    private static final MBeanConstructorInfo[] NO_CONSTRUCTORS = new MBeanConstructorInfo[0];
    private final boolean immutable;
    private final int hashCode = System.identityHashCode(this);
    private String name;
    private String description;
    private final Map targetClasses = new HashMap();
    private final Set attributes = new HashSet();
    private final Set operations = new HashSet();
    private final Set notifications = new HashSet();
    final Map targets = new HashMap();
    public static final String ALWAYS = "always";
    public static final String NEVER = "never";

    public GeronimoMBeanInfo() {
        super(null, null, null, null, null, null);
        immutable = false;
    }

    GeronimoMBeanInfo(GeronimoMBeanInfo source) {
        super(null, null, null, null, null, null);
        immutable = true;

        //
        // Required
        //
        if (source.targetClasses.get(DEFAULT_TARGET_NAME) == null) {
            throw new IllegalStateException("No default target specified");
        }
        // we can just put all because everything in the targetClasses map is immutable
        targetClasses.putAll(source.targetClasses);

        //
        // Optional
        //
        name = source.name;
        description = source.description;

        //
        // Derived
        //
        String className = null;
        try {
            for (Iterator i = targetClasses.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                className = (String) entry.getValue();
                Object target = ParserUtil.loadClass(className).newInstance();
                targets.put(entry.getKey(), target);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Target class could not be loaded: className=" + className);
        } catch (InstantiationException e) {
            IllegalArgumentException exception = new IllegalArgumentException("Target class could not be loaded: className=" + className);
            exception.initCause(e);
            throw exception;
        } catch (IllegalAccessException e) {
            IllegalArgumentException exception = new IllegalArgumentException("Cound not access target class default constructor: className=" + className);
            exception.initCause(e);
            throw exception;
        }

        //
        // Contained classes
        //
        for (Iterator iterator = source.attributes.iterator(); iterator.hasNext();) {
            GeronimoAttributeInfo attributeInfo = (GeronimoAttributeInfo) iterator.next();
            attributes.add(new GeronimoAttributeInfo(attributeInfo, this));
        }

        for (Iterator iterator = source.operations.iterator(); iterator.hasNext();) {
            GeronimoOperationInfo operationInfo = (GeronimoOperationInfo) iterator.next();
            operations.add(new GeronimoOperationInfo(operationInfo, this));
        }

        for (Iterator iterator = source.notifications.iterator(); iterator.hasNext();) {
            GeronimoNotificationInfo notificationInfo = (GeronimoNotificationInfo) iterator.next();
            notifications.add(new GeronimoNotificationInfo(notificationInfo, this));
        }
    }

    public String getClassName() {
        return getTargetClass();
    }

    public String getTargetClass() {
        return (String) targetClasses.get(DEFAULT_TARGET_NAME);
    }

    public void setTargetClass(String className) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        targetClasses.put(DEFAULT_TARGET_NAME, className);
    }

    public String getTargetClass(String targetName) {
        return (String) targetClasses.get(targetName);
    }

    public void setTargetClass(String targetName, String className) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        if (targetName != null && targetName.length() > 0) {
            targetClasses.put(targetName, className);
        } else {
            targetClasses.put(DEFAULT_TARGET_NAME, className);
        }
    }

    Object getTarget() {
        return targets.get(DEFAULT_TARGET_NAME);
    }

    Object getTarget(String name) {
        return targets.get(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        this.description = description;
    }

    public MBeanConstructorInfo[] getConstructors() {
        // This MBean does not have constructors
        return NO_CONSTRUCTORS;
    }

    public Set getAttributeSet() {
        return Collections.unmodifiableSet(attributes);
    }

    public MBeanAttributeInfo[] getAttributes() {
        return (MBeanAttributeInfo[]) attributes.toArray(new MBeanAttributeInfo[attributes.size()]);
    }

    public void addAttributeInfo(GeronimoAttributeInfo attributeInfo) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        attributes.add(attributeInfo);
    }

    public Set getOperationsSet() {
        return Collections.unmodifiableSet(operations);
    }

    public MBeanOperationInfo[] getOperations() {
        return (MBeanOperationInfo[]) operations.toArray(new MBeanOperationInfo[operations.size()]);
    }

    public void addOperationInfo(GeronimoOperationInfo operationInfo) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        operations.add(operationInfo);
    }

    public Set getNotificationsSet() {
        return Collections.unmodifiableSet(notifications);
    }

    public MBeanNotificationInfo[] getNotifications() {
        return (MBeanNotificationInfo[]) notifications.toArray(new MBeanNotificationInfo[notifications.size()]);
    }

    public void addNotificationInfo(GeronimoNotificationInfo notificationInfo) {
        if (immutable) {
            throw new IllegalStateException("Data is no longer mutable");
        }
        notifications.add(notificationInfo);
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object object) {
        return (this == object);
    }

    public String toString() {
        return "[GeronimoMBeanInfo: name=" + name + " description=" + description + "]";
    }
}
