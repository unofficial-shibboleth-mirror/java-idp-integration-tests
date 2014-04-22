/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.util.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * Abstract integration test which starts a Jetty server and an in-memory directory server.
 */
public abstract class BaseIntegrationTest {

    /** The IdP XML security manager property key. */
    @Nonnull public final static String IDP_XML_SECURITY_MANAGER_PROP_NAME = "idp.xml.securityManager";

    /** The IdP XML security manager property value for this test. */
    @Nonnull public final static String IDP_XML_SECURITY_MANAGER_PROP_VALUE = "org.apache.xerces.util.SecurityManager";

    /** Path to LDIF file to be imported into directory server. */
    @Nonnull public final static String LDIF_FILE = "src/test/resources/test/ldap.ldif";

    /** Spring application context in which Jetty is run. */
    @NonnullAfterInit protected GenericXmlApplicationContext applicationContext;

    /** In-memory directory server. */
    @NonnullAfterInit protected InMemoryDirectoryServer directoryServer;

    /** The IdP XML security manager value before and after this test. */
    @NonnullAfterInit protected String idpXMLSecurityManager;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    /**
     * Set the 'idp.home' system property as determined by {@link #pathToIdPHome()}.
     * 
     * @throws IllegalArgumentException if the path to idp.home is {@code null}
     */
    @BeforeClass public static void setupPathToIdPHome() {
        Path pathToIdPHome = pathToIdPHome();
        Assert.notNull(pathToIdPHome, "Path to idp.home not found.");
        System.setProperty("idp.home", pathToIdPHome.toFile().getAbsolutePath());
    }

    /**
     * Get the path to idp.home. First, look for the 'idp.home' directory created by Maven. Second, look for the
     * java-identity-provider project.
     * 
     * @return the path to idp.home or null if not found
     */
    @Nullable public static Path pathToIdPHome() {

        Path pathToIdPHomeMaven = pathToIdPHomeMaven();
        if (pathToIdPHomeMaven.toFile().exists()) {
            return pathToIdPHomeMaven;
        }

        Path pathToIdPHomeEclipse = pathToIdPHomeEclipse();
        if (pathToIdPHomeEclipse.toFile().exists()) {
            return pathToIdPHomeEclipse;
        }

        return null;
    }

    /**
     * Get the path to idp.home in the java-identity-provider project.
     * 
     * @return path to idp.home
     */
    @Nonnull public static Path pathToIdPHomeEclipse() {
        // The parent path of the current directory
        Path parentPath = Paths.get("").toAbsolutePath().getParent();

        // Path to java-identity-provider
        Path pathToIdP = parentPath.resolve(Paths.get("java-identity-provider"));

        // Path to idp-conf/src/main/resources
        Path pathToIdPHome = pathToIdP.resolve(Paths.get("idp-conf", "src", "main", "resources"));

        return pathToIdPHome;
    }

    /**
     * Get the path to idp.home created by Maven.
     * 
     * @return path to idp.home
     */
    @Nonnull public static Path pathToIdPHomeMaven() {
        return Paths.get("idp.home").toAbsolutePath();
    }

    /**
     * Set the 'idp.war.path' system property as determined by {@link #pathToIdPWar()}.
     * 
     * @throws IllegalArgumentException if the path to idp.war.path is {@code null}
     */
    @BeforeClass public static void setupPathToIdPWar() {
        Path pathToIdPWar = pathToIdPWar();
        Assert.notNull(pathToIdPWar, "Path to idp.war not found.");
        System.setProperty("idp.war.path", pathToIdPWar.toFile().getAbsolutePath());
    }

    /**
     * Get the path to idp.war. First, look for the 'idp.home' directory created by Maven. Second, look for the
     * java-identity-provider project.
     * 
     * @return the path to idp.war or null if not found
     */
    @Nullable public static Path pathToIdPWar() {

        Path pathToIdPWarMaven = pathToIdPWarMaven();
        if (pathToIdPWarMaven.toFile().exists()) {
            return pathToIdPWarMaven;
        }

        Path pathToIdPWarEclipse = pathToIdPWarEclipse();
        if (pathToIdPWarEclipse.toFile().exists()) {
            return pathToIdPWarEclipse;
        }

        return null;
    }

    /**
     * Get the path to idp.war in the java-identity-provider project.
     * 
     * @return path to idp.war
     */
    @Nonnull public static Path pathToIdPWarEclipse() {
        // The parent path of the current directory
        Path parentPath = Paths.get("").toAbsolutePath().getParent();

        // Path to java-identity-provider
        Path pathToIdP = parentPath.resolve(Paths.get("java-identity-provider"));

        // Path to idp-war/src/main/webapp
        Path pathToIdPWar = pathToIdP.resolve(Paths.get("idp-war", "src", "main", "webapp"));

        return pathToIdPWar;
    }

    /**
     * Get the path to idp.war created by Maven.
     * 
     * @return the path to idp.war
     */
    @Nonnull public static Path pathToIdPWarMaven() {
        return Paths.get("idp.home", "war", "idp.war").toAbsolutePath();
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to {@link #IDP_XML_SECURITY_MANAGER_PROP_VALUE}.
     * Save the previous value.
     */
    @BeforeClass public void setIdPXMLSecurityManager() {
        idpXMLSecurityManager = System.getProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME);
        System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, IDP_XML_SECURITY_MANAGER_PROP_VALUE);
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to the previous value.
     */
    @AfterClass public void unsetIdPXMLSecurityManager() {
        if (idpXMLSecurityManager != null) {
            System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, idpXMLSecurityManager);
        }
    }

    /**
     * Starts an UnboundID in-memory directory server. Leverages LDIF found at {@value #LDIF_FILE}.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     */
    @BeforeMethod public void startDirectoryServer() throws LDAPException {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=org", "ou=system");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 10389));
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        directoryServer = new InMemoryDirectoryServer(config);
        directoryServer.importFromLDIF(true, LDIF_FILE);
        directoryServer.startListening();
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterMethod public void stopDirectoryServer() {
        directoryServer.shutDown(true);
    }

    /**
     * Start the Jetty server via Spring.
     */
    @BeforeMethod(dependsOnMethods = {"startDirectoryServer"}) public void startJettyServer() {
        applicationContext = new GenericXmlApplicationContext("/conf/idp-server.xml");
    }

    /**
     * Stop the Jetty server.
     */
    @AfterMethod public void stopJettyServer() {
        if (applicationContext != null) {
            applicationContext.stop();
        }
    }
}
