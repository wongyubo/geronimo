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
package org.apache.geronimo.tomcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebUserDataPermission;

import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;

import org.apache.geronimo.security.deploy.DefaultPrincipal;
import org.apache.geronimo.security.deploy.Principal;
import org.apache.geronimo.security.deploy.Realm;
import org.apache.geronimo.security.deploy.Role;
import org.apache.geronimo.security.deploy.Security;


/**
 * Tests the JACC security for Tomcat
 *
 * @version $Revision$ $Date$
 */
public class JACCSecurityTest extends AbstractWebModuleTest {

    ObjectName appName = null;

    /**
     * Test the explicit map feature.  Only Alan should be able to log in.
     *
     * @throws Exception thrown if an error in the test occurs
     */
    public void testExplicitMapping() throws Exception {

        Set constraints = new HashSet();

        SecurityConstraint sc = new SecurityConstraint();
        sc.setAuthConstraint(true);
        sc.addAuthRole("content-administrator");
        sc.addAuthRole("auto-administrator");
        SecurityCollection coll = new SecurityCollection("Admin Role");
        coll.addPattern("/protected/*");
        sc.addCollection(coll);
        constraints.add(sc);

        sc = new SecurityConstraint();
        sc.setAuthConstraint(false);
        coll = new SecurityCollection("NO ACCESS");
        coll.addPattern("/auth/logon.html");
        sc.addCollection(coll);
        constraints.add(sc);

        Security securityConfig = new Security();
        securityConfig.setUseContextHandler(false);

        DefaultPrincipal defaultPrincipal = new DefaultPrincipal();
        defaultPrincipal.setRealmName("demo-properties-realm");
        Principal principal = new Principal();
        principal.setClassName("org.apache.geronimo.security.realm.providers.GeronimoUserPrincipal");
        principal.setPrincipalName("izumi");
        defaultPrincipal.setPrincipal(principal);

        securityConfig.setDefaultPrincipal(defaultPrincipal);

        Role role = new Role();
        role.setRoleName("content-administrator");
        principal = new Principal();
        principal.setClassName("org.apache.geronimo.security.realm.providers.GeronimoGroupPrincipal");
        principal.setPrincipalName("it");
        Realm realm = new Realm();
        realm.setRealmName("demo-properties-realm");
        realm.getPrincipals().add(principal);
        role.getRealms().put(realm.getRealmName(), realm);

        securityConfig.getRoleMappings().put(role.getRoleName(), role);

        PermissionCollection uncheckedPermissions = new Permissions();

        PermissionCollection excludedPermissions = new Permissions();
        excludedPermissions.add(new WebResourcePermission("/auth/login.html", ""));
        excludedPermissions.add(new WebUserDataPermission("/auth/login.html", ""));

        Map rolePermissions = new HashMap();
        Set permissions = new HashSet();
        permissions.add(new WebUserDataPermission("/protected/*", ""));
        permissions.add(new WebResourcePermission("/protected/*", ""));
        rolePermissions.put("content-administrator", permissions);
        rolePermissions.put("auto-administrator", permissions);

        Set securityRoles = new HashSet();
        securityRoles.add("content-administrator");
        securityRoles.add("auto-administrator");

        startWebApp(securityConfig, constraints, uncheckedPermissions, excludedPermissions, rolePermissions, securityRoles);

        //Begin the test
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8080/securetest/protected/hello.txt").openConnection();
        connection.setInstanceFollowRedirects(false);
        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());

        //Be sure we have been given the login page
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        assertEquals("<!-- Login Page -->", reader.readLine());
        reader.close();

        String cookie = connection.getHeaderField("Set-Cookie");
        cookie = cookie.substring(0, cookie.lastIndexOf(';'));
        String location = "http://localhost:8080/securetest/protected/j_security_check?j_username=alan&j_password=starcraft";

        connection = (HttpURLConnection) new URL(location).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", cookie);
        connection.setInstanceFollowRedirects(false);
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, connection.getResponseCode());

        connection = (HttpURLConnection) new URL("http://localhost:8080/securetest/protected/hello.txt").openConnection();
        connection.setRequestProperty("Cookie", cookie);
        connection.setInstanceFollowRedirects(false);
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        assertEquals("Hello World", reader.readLine());
        connection.disconnect();

        //Now lets try it with izumi
        connection = (HttpURLConnection) new URL("http://localhost:8080/securetest/protected/hello.txt").openConnection();
        connection.setInstanceFollowRedirects(false);
        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());

        cookie = connection.getHeaderField("Set-Cookie");
        cookie = cookie.substring(0, cookie.lastIndexOf(';'));

        //Be sure we have been given the login page
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        assertEquals("<!-- Login Page -->", reader.readLine());
        reader.close();

        location = "http://localhost:8080/securetest/protected/j_security_check?j_username=izumi&j_password=violin";

        connection = (HttpURLConnection) new URL(location).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", cookie);
        connection.setInstanceFollowRedirects(false);
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, connection.getResponseCode());

        try {
            connection = (HttpURLConnection) new URL("http://localhost:8080/securetest/protected/hello.txt").openConnection();
            connection.setRequestProperty("Cookie", cookie);
            connection.setInstanceFollowRedirects(false);
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            fail("Should throw an IOException for HTTP 403 response");
        } catch (IOException e) {
        }

        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, connection.getResponseCode());
        connection.disconnect();


        stopWebApp();
    }

    protected void startWebApp(Security securityConfig,
                               Set securityConstraints,
                               PermissionCollection uncheckedPermissions,
                               PermissionCollection excludedPermissions,
                               Map rolePermissions,
                               Set securityRoles) throws Exception {

        appName = setUpSecureAppContext(securityConfig, securityConstraints, uncheckedPermissions,
                                        excludedPermissions, rolePermissions, securityRoles);


    }

    protected void stopWebApp() throws Exception {
        stop(appName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setUpSecurity();
    }

    protected void tearDown() throws Exception {
        tearDownSecurity();
        super.tearDown();
    }

}
