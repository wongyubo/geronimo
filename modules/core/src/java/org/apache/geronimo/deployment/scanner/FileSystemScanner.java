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
package org.apache.geronimo.deployment.scanner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 *
 *
 * @version $Revision: 1.4 $ $Date: 2003/08/12 07:10:15 $
 */
public class FileSystemScanner implements Scanner {
    private final File root;
    private final boolean recurse;
    private final FileFilter filter;

    public FileSystemScanner(File root, boolean recurse, FileFilter filter) {
        this.root = root;
        this.recurse = recurse;
        this.filter = filter;
    }

    private static final FileFilter DEFAULT_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return !pathname.isHidden();
        }
    };

    public FileSystemScanner(File root, boolean recurse) {
        this(root, recurse, DEFAULT_FILTER);
    }

    public synchronized Set scan() throws IOException {
        Set result = new HashSet();
        LinkedList toScan = new LinkedList();
        toScan.addFirst(root);
        while (!toScan.isEmpty()) {
            File dir = (File) toScan.removeFirst();
            File[] files = dir.listFiles(filter);
            if (files == null) {
                // this is not a directory any more ...
                continue;
            }

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                URLType type = URLType.getType(file);
                if (type == URLType.COLLECTION) {
                    if (recurse) {
                        // add this to the end of the list so we go breadth first
                        toScan.addLast(file);
                    }
                } else {
                    result.add(new URLInfo(file.toURL(), type));
                }
            }
        }
        return result;
    }
}
