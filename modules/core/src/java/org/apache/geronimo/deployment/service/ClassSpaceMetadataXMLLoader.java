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
package org.apache.geronimo.deployment.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.geronimo.deployment.DeploymentException;
import org.apache.geronimo.deployment.scanner.FileSystemScanner;
import org.apache.geronimo.deployment.scanner.Scanner;
import org.apache.geronimo.deployment.scanner.WebDAVScanner;
import org.apache.geronimo.deployment.scanner.URLInfo;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 *
 * @version $Revision: 1.2 $ $Date: 2003/08/12 07:10:16 $
 */
public class ClassSpaceMetadataXMLLoader {
    public ClassSpaceMetadata loadXML(Element element) throws DeploymentException {
        ClassSpaceMetadata md = new ClassSpaceMetadata();
        try {
            md.setName(new ObjectName(element.getAttribute("name")));
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException(e);
        }
        List urls = md.getUrls();
        NodeList nl = element.getElementsByTagName("codebase");
        try {
            for (int i = 0; i < nl.getLength(); i++) {
                Element codebaseElement = (Element) nl.item(i);
                URL codebase;
                codebase = new URL(codebaseElement.getAttribute("url"));
                NodeList archiveNodes = codebaseElement.getElementsByTagName("archive");
                if (archiveNodes.getLength() == 0) {
                    // no archives present, use codebase itself
                    urls.add(codebase);
                } else {
                    for (int j = 0; j < archiveNodes.getLength(); j++) {
                        Element archiveElement = (Element) archiveNodes.item(j);
                        String archive = archiveElement.getAttribute("name");
                        if ("*".equals(archive)) {
                            // scan location and add all
                            Collection c = scan(codebase);
                            for (Iterator k = c.iterator(); k.hasNext();) {
                                URLInfo urlInfo = (URLInfo) k.next();
                                urls.add(urlInfo.getUrl());
                            }
                        } else {
                            // add explicit archive
                            URL url = new URL(codebase, archive);
                            urls.add(url);
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new DeploymentException(e);
        }
        return md;
    }

    private Collection scan(URL codebase) throws DeploymentException {
        Scanner scanner;
        String protocol = codebase.getProtocol();
        if ("file".equals(protocol)) {
            File root = new File(codebase.getFile());
            scanner = new FileSystemScanner(root, false);
        } else if ("http".equals(protocol) || "https".equals(protocol)) {
            scanner = new WebDAVScanner(codebase, false);
        } else {
            throw new DeploymentException("Unknown protocol for scan of codebase url " + codebase);
        }
        try {
            return scanner.scan();
        } catch (IOException e) {
            throw new DeploymentException(e);
        }
    }
}
