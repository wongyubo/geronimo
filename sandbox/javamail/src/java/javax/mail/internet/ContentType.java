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
//
// This source code implements specifications defined by the Java
// Community Process. In order to remain compliant with the specification
// DO NOT add / change / or delete method signatures!
//
package javax.mail.internet;
// can be in the form major/minor; charset=jobby
/**
 * @version $Revision: 1.1 $ $Date: 2004/01/29 04:20:04 $
 */
public class ContentType {
    private ParameterList _list;
    private String _minor;
    private String _major;
    public ContentType() {
        this("text", "plain", new ParameterList());
    }
    public ContentType(String major, String minor, ParameterList list) {
        _major = major;
        _minor = minor;
        _list = list;
    }
    public ContentType(String type) throws ParseException {
        final int slash = type.indexOf("/");
        final int semi = type.indexOf(";");
        try {
            _major = type.substring(0, slash);
            if (semi == -1) {
                _minor = type.substring(slash + 1);
            } else {
                _minor = type.substring(slash + 1, semi);
                _list = new ParameterList(type.substring(semi + 1));
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ParseException("Type invalid: " + type);
        }
    }
    public String getPrimaryType() {
        return _major;
    }
    public String getSubType() {
        return _minor;
    }
    public String getBaseType() {
        return _major + "/" + _minor;
    }
    public String getParameter(String name) {
        return (_list == null ? null : _list.get(name));
    }
    public ParameterList getParameterList() {
        return _list;
    }
    public void setPrimaryType(String major) {
        _major = major;
    }
    public void setSubType(String minor) {
        _minor = minor;
    }
    public void setParameter(String name, String value) {
        if (_list == null) {
            _list = new ParameterList();
        }
        _list.set(name, value);
    }
    public void setParameterList(ParameterList list) {
        _list = list;
    }
    public String toString() {
        return getBaseType() + (_list == null ? "" : ";" + _list.toString());
    }
    public boolean match(ContentType other) {
        return _major.equals(other._major)
            && (_minor.equals(other._minor)
                || _minor.equals("*")
                || other._minor.equals("*"));
    }
    public boolean match(String contentType) {
        try {
            return match(new ContentType(contentType));
        } catch (ParseException e) {
            return false;
        }
    }
}
