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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.installer.PropertiesWithComments;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.testng.SauceBrowserDataProvider;
import com.saucelabs.testng.SauceOnDemandAuthenticationProvider;

/**
 * Abstract integration test which tests the IdP via the testbed using Selenium.
 * <p/>
 * The Maven POM unpacks the IdP and Jetty distributions, adds test views and flows from idp-conf, and adds deployment
 * of the testbed. The testbed provides an in-memory directory server.
 * <p/>
 * The IdP and testbed webapps are run via Jetty's start.jar in a separate {@link Process}, see
 * {@link JettyServerProcess}.
 * <p/>
 * Each concrete subclass is associated with an idp.home directory, which is created by copying the unpacked IdP
 * distribution, see {@link #setUpPaths()}. Consequently, each test method in a class uses the same idp.home directory.
 * This per-class idp.home directory is deleted only if all tests pass. The per-class idp.home directory name is a
 * timestamp whose pattern is defined by {@link #idpHomePattern}.
 * </p>
 * Ports for Jetty and the in-memory directory server will be automatically selected between the range of 20000 - 30000,
 * see {@link #setUpAvailablePorts()}.
 * </p>
 * Test methods should start clients via {@link #startSeleniumClient(BrowserData)} and start the server via
 * {@link #startJettyServer()}.
 * <p/>
 * By default, tests run using a local browser. By default the {@link HtmlUnitDriver} will be used. To override, set the
 * {@link #driver} to the desired {@link WebDriver}. See {@link #startSeleniumClient(BrowserData)} for one way to
 * override.
 * <p/>
 * To run tests using remote browsers provided by Sauce Labs, set the {@link #SELENIUM_IS_REMOTE} system property and
 * set the {@link #SERVER_ADDRESS_PROPERTY} to the publicly accessible IP address of the server to which clients should
 * connect to. You will also probably need to set the {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} to the IP address that
 * the server should be run on, which might be the same as the {@link #SERVER_ADDRESS_PROPERTY}.
 * <p/>
 * With Sauce Labs, the browsers tested are defined by {@link SauceBrowserDataProvider#SAUCE_ONDEMAND_BROWSERS} in the
 * environment, which is a JSON string. See
 * <a href="https://docs.saucelabs.com/ci-integrations/jenkins/">https://docs.saucelabs.com/ci-integrations/jenkins/</a>
 * for details. This is populated by the Jenkins Sauce OnDemand Plugin. If this is not available via the environment,
 * the 'firefox' browser is used by default, see {@link #sauceOnDemandBrowserDataProvider(Method)}. To override the
 * 'firefox' browser, manipulate the {@link #desiredCapabilities} before calling
 * {@link #startSeleniumClient(BrowserData)}, for example:
 * </p>
 * desiredCapabilities.setCapability("platform", "win8");
 * </p>
 * or
 * </p>
 * desiredCapabilities.setCapability(org.openqa.selenium.remote.CapabilityType.Platform,
 * org.openqa.selenium.Platform.WIN8);
 * </p>
 * See {@link org.openqa.selenium.Platform}. Or, configure a new TestNG data provider.
 */
@Listeners({CustomSauceOnDemandTestListener.class})
public abstract class BaseIntegrationTest
        implements SauceOnDemandSessionIdProvider, SauceOnDemandAuthenticationProvider {

    /** Name of property defining the address that the web server listens on. */
    @Nonnull public final static String PRIVATE_SERVER_ADDRESS_PROPERTY = "server.address.private";

    /** Name of property defining the address that clients should connect to. */
    @Nonnull public final static String SERVER_ADDRESS_PROPERTY = "server.address";

    /** Directory in which distributions will be unpackaged. */
    @Nonnull public final static String TEST_DISTRIBUTIONS_DIRECTORY = "test-distributions";

    /** Name of system property which determines if tests are local. */
    @Nonnull public final static String SELENIUM_IS_LOCAL = "SELENIUM_IS_LOCAL";

    /** Name of system property which determines if tests are remote. */
    @Nonnull public final static String SELENIUM_IS_REMOTE = "SELENIUM_IS_REMOTE";

    /** IP range of Sauce Labs. */
    @Nonnull public final static String SAUCE_LABS_IP_RANGE = "162.222.73.0/24";

    /** Name of property defining the port that the test directory server listens on. */
    @Nonnull public final static String TEST_LDAP_PORT_PROPERTY = "test.ldap.port";

    /** IdP XML security manager property key. */
    @Nonnull public final static String IDP_XML_SECURITY_MANAGER_PROP_NAME = "idp.xml.securityManager";

    /** IdP XML security manager property value for this test. */
    @Nonnull public final static String IDP_XML_SECURITY_MANAGER_PROP_VALUE = "org.apache.xerces.util.SecurityManager";

    /** Property value of consent flows to enable. */
    public final static String ENABLE_CONSENT_FLOW_PROPERTY_VALUE = "terms-of-use|attribute-release";

    /** Title of terms of use page. */
    public final static String TERMS_OF_USE_PAGE_TITLE = "Example Terms of Use";

    /** Title of attribute release page. */
    public final static String ATTRIBUTE_RELEASE_PAGE_TITLE = "Information Release";

    /** ID of email attribute checkbox. */
    public final static String EMAIL_ID = "mail";

    /** ID of eduPersonAffiliation attribute checkbox. */
    public final static String EDU_PERSON_AFFILIATION_ID = "eduPersonScopedAffiliation";

    /** ID of uid attribute checkbox. */
    public final static String UID_ID = "uid";

    /** ID of eduPersonAffiliation attribute checkbox. */
    public final static String EDU_PERSON_PRINCIPAL_NAME_ID = "eduPersonPrincipalName";

    /** ID of radio button to not remember consent. */
    public final static String DO_NOT_REMEMBER_CONSENT_ID = "_shib_idp_doNotRememberConsent";

    /** ID of radio button to remember consent. */
    public final static String REMEMBER_CONSENT_ID = "_shib_idp_rememberConsent";

    /** ID of radio button for global consent. */
    public final static String GLOBAL_CONSENT_ID = "_shib_idp_globalConsent";

    /** Name of form input element containing consent IDs. */
    public final static String CONSENT_IDS_INPUT_NAME = "_shib_idp_consentIds";

    /** Name of form input element to submit form. */
    public final static String SUBMIT_FORM_INPUT_NAME = "_eventId_proceed";

    /** IdP XML security manager value before and after this test. */
    @NonnullAfterInit protected String defaultIdpXMLSecurityManager;

    /** Server process. */
    @NonnullAfterInit protected AbstractServerProcess server;

    /** Additional commands used to start the server process. */
    @NonnullAfterInit protected List<String> serverCommands = new ArrayList<>();

    /** Non-secure address that the web server listens on. Defaults to "localhost". */
    @Nonnull protected String privateAddress = "localhost";

    /** Non-secure port that the web server listens on. Defaults to 8080. */
    @Nonnull protected Integer port = 8080;

    /** Secure address that the web server listens on. Defaults to "localhost". */
    @Nonnull protected String privateSecureAddress = "localhost";

    /** Secure port that the web server listens on. Defaults to 8443. */
    @Nonnull protected Integer securePort = 8443;

    /** Backchannel port that the web server listens on. Defaults to 9443. */
    @Nonnull protected Integer backchannelPort = 9443;

    /** Port that the test LDAP server listens on. Defaults to 10389. */
    @Nonnull protected Integer ldapPort = 10389;

    /** Non-secure address that clients should connect to. Defaults to "localhost". */
    @Nonnull protected String address = "localhost";

    /** Secure address that clients should connect to. Defaults to "localhost". */
    @Nonnull protected String secureAddress = "localhost";

    /** Non-secure web server base URL. Defaults to http://localhost:8080. */
    @NonnullAfterInit protected String baseURL;

    /** Secure web server base URL. Defaults to https://localhost:8443. */
    @NonnullAfterInit protected String secureBaseURL;

    /** Whether to use the secure base URL by default. Defaults to false. */
    @Nonnull protected boolean useSecureBaseURL = false;

    /** Client IP range to allow access from. Defaults to "127.0.0.1/32". */
    @Nonnull protected String clientIPRange = "127.0.0.1/32";

    /** Path to idp.home. */
    @NonnullAfterInit protected Path pathToIdPHome;

    /** Path to conf/idp.properties. */
    @NonnullAfterInit protected Path pathToIdPProperties;

    /** Path to conf/ldap.properties. */
    @NonnullAfterInit protected Path pathToLDAPProperties;

    /** Path to jetty.base. */
    @Nullable protected Path pathToJettyBase;

    /** Path to jetty.home. */
    @Nullable protected Path pathToJettyHome;
    
    /** Path to tomcat.base. */
    @Nullable protected Path pathToTomcatBase;

    /** Path to tomcat.home. */
    @Nullable protected Path pathToTomcatHome;

    /** Pattern used when creating per test idp.home directory. Defaults to yyyyMMdd-HHmmssSS. **/
    @Nullable protected String idpHomePattern = "yyyyMMdd-HHmmssSS";

    /** Parser pool */
    @NonnullAfterInit protected ParserPool parserPool;

    /** XMLObject unmarshaller factory */
    @NonnullAfterInit protected UnmarshallerFactory unmarshallerFactory;

    /** Desired capabilities of the web driver. */
    @Nonnull protected DesiredCapabilities desiredCapabilities = new DesiredCapabilities();

    /** Override desired capabilities of the web driver. */
    @Nullable protected DesiredCapabilities overrideCapabilities;

    /** Web driver. */
    @Nonnull protected WebDriver driver;

    /** Thread local Web driver. */
    @Nonnull protected ThreadLocal<WebDriver> threadLocalWebDriver = new ThreadLocal<WebDriver>();

    /** Thread local Sauce On Demand session ID. */
    @Nonnull protected ThreadLocal<String> threadLocalSessionId = new ThreadLocal<String>();

    /** URL path to start the flow. */
    @Nullable protected String startFlowURLPath;

    /** URL path of login page. */
    @Nullable protected String loginPageURLPath;

    /** URL path of page containing the SAML response at the SP. */
    @Nullable protected String responsePageURLPath;

    /** URL path to start the passive flow. */
    @Nullable protected String isPassiveRequestURLPath;

    /** URL path to start the force authn flow. */
    @Nullable protected String forceAuthnRequestURLPath;

    /** URL path of IdP single logout service endpoint. */
    @Nullable protected String idpLogoutURLPath;

    /** URL path of SP single logout service endpoint. */
    @Nullable protected String spLogoutURLPath;

    /** Sauce Labs authentication. */
    @Nonnull protected SauceOnDemandAuthentication sauceOnDemandAuthentication = new SauceOnDemandAuthentication();

    /** Name of test class concatenated with the test method. **/
    @Nullable protected String testName;

    /** Whether any test method in a class failed. **/
    @Nonnull protected boolean testClassFailed;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    /**
     * Set up paths to the IdP.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public void setUpIdPPaths() throws Exception {

        // Path to the project build directory.
        final Path buildPath = Paths.get(TEST_DISTRIBUTIONS_DIRECTORY);
        log.debug("Path to build directory '{}'", buildPath.toAbsolutePath());
        Assert.assertTrue(buildPath.toAbsolutePath().toFile().exists(), "Path to build directory not found");

        // Path to idp.home from distribution.
        Path pathToDistIdPHome = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*shibboleth-identity-provider-*")) {
            for (Path entry : stream) {
                pathToDistIdPHome = entry;
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Path to distribution idp.home '{}'", pathToDistIdPHome.toAbsolutePath());
        Assert.assertNotNull(pathToDistIdPHome, "Path to distribution idp.home not found");
        Assert.assertTrue(pathToDistIdPHome.toAbsolutePath().toFile().exists(), "Path to dist idp.home not found");

        // Path to per-test idp.home
        final String timestamp = new DateTime().toString(DateTimeFormat.forPattern(idpHomePattern));
        pathToIdPHome = pathToDistIdPHome.getParent().resolve(timestamp);
        log.info("Path to idp.home '{}'", pathToIdPHome.toAbsolutePath());
        Assert.assertFalse(pathToIdPHome.toAbsolutePath().toFile().exists(), "Path to idp.home already exists");

        // Copy idp.home directory from distribution to new per test directory
        final File sourceDir = pathToDistIdPHome.toAbsolutePath().toFile();
        final File destinationDir = pathToIdPHome.toAbsolutePath().toFile();
        FileSystemUtils.copyRecursively(sourceDir, destinationDir);
        Assert.assertTrue(destinationDir.exists(), "Path to idp.home not found");

        // Set idp.home system property, replace '\' with '/' for Windows
        System.setProperty("idp.home", pathToIdPHome.toAbsolutePath().toString().replace('\\', '/'));

        // Path to conf/idp.properties
        pathToIdPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "idp.properties");
        Assert.assertTrue(pathToIdPProperties.toFile().exists(), "Path to conf/idp.properties not found");

        // Path to conf/ldap.properties
        pathToLDAPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "ldap.properties");
        Assert.assertTrue(pathToLDAPProperties.toFile().exists(), "Path to conf/ldap.properties not found");
    }

    /**
     * Set up paths to Tomcat if they exist.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public void setUpTomcatPaths() throws Exception {

        // Path to the project build directory.
        final Path buildPath = Paths.get(TEST_DISTRIBUTIONS_DIRECTORY);
        log.debug("Path to build directory '{}'", buildPath.toAbsolutePath());
        Assert.assertTrue(buildPath.toAbsolutePath().toFile().exists(), "Path to build directory not found");

        // Path to Tomcat distribution
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*apache-tomcat-*")) {
            for (Path entry : stream) {
                pathToTomcatHome = entry;
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Path to tomcat.home '{}'", pathToTomcatHome);

        if (pathToTomcatHome != null) {
            log.debug("Path to tomcat.home '{}'", pathToTomcatHome.toAbsolutePath());
            Assert.assertTrue(pathToTomcatHome.toAbsolutePath().toFile().exists(), "Path to tomcat.home not found");

            // Path to tomcat.base
            pathToTomcatBase = pathToIdPHome.resolve(Paths.get("tomcat-base"));
            log.debug("Path to tomcat.base '{}'", pathToTomcatBase.toAbsolutePath());
            Assert.assertNotNull(pathToTomcatBase, "Path to tomcat.base not found");
            Assert.assertTrue(pathToTomcatBase.toAbsolutePath().toFile().exists(), "Path to tomcat.base not found");

            // Make tmp directories exist
            Assert.assertTrue(pathToTomcatBase.resolve("temp").toFile().exists(), "Path to temp/ not found");
            
            // Modify setenv.sh and setenv.bat with per-test idp.home directory
            final Path pathToSetenvSh = pathToTomcatBase.resolve(Paths.get("bin", "setenv.sh"));
            Assert.assertTrue(pathToSetenvSh.toAbsolutePath().toFile().exists(), "Path to setenv.sh not found");
            final Path pathToSetenvBat = pathToTomcatBase.resolve(Paths.get("bin", "setenv.bat"));
            Assert.assertTrue(pathToSetenvBat.toAbsolutePath().toFile().exists(), "Path to setenv.bat not found");
            final String oldTextSetenvSh = "-Didp.home=/opt/shibboleth-idp";
            final String newTextSetenvSh = "-Didp.home=" + Matcher.quoteReplacement(pathToIdPHome.toAbsolutePath().toString());
            replaceFile(pathToSetenvSh, oldTextSetenvSh, newTextSetenvSh);
            replaceFile(pathToSetenvBat, oldTextSetenvSh, newTextSetenvSh);

            // Modify context descriptor with per-test idp.home directory
            final Path pathToIdpXML = pathToTomcatBase.resolve(Paths.get("conf", "Catalina", "localhost", "idp.xml"));
            Assert.assertTrue(pathToIdpXML.toAbsolutePath().toFile().exists(), "Path to idp.xml not found");
            replaceFile(pathToIdpXML, "/war/idp.war\"", "/webapp/\"");
        }
    }

    /**
     * Set up paths to Jetty if they exist.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public void setUpJettyPaths() throws Exception {

        // Path to the project build directory.
        final Path buildPath = Paths.get(TEST_DISTRIBUTIONS_DIRECTORY);
        log.debug("Path to build directory '{}'", buildPath.toAbsolutePath());
        Assert.assertTrue(buildPath.toAbsolutePath().toFile().exists(), "Path to build directory not found");

        // Path to Jetty distribution
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*jetty-distribution-*")) {
            for (Path entry : stream) {
                pathToJettyHome = entry;
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Path to jetty.home '{}'", pathToJettyHome);

        if (pathToJettyHome != null) {
            log.debug("Path to jetty.home '{}'", pathToJettyHome.toAbsolutePath());
            Assert.assertTrue(pathToJettyHome.toAbsolutePath().toFile().exists(), "Path to jetty.home not found");

            // Path to jetty.base
            pathToJettyBase = pathToIdPHome.resolve(Paths.get("jetty-base"));
            log.debug("Path to jetty.base '{}'", pathToJettyBase.toAbsolutePath());
            Assert.assertNotNull(pathToJettyBase, "Path to jetty.base not found");
            Assert.assertTrue(pathToJettyBase.toAbsolutePath().toFile().exists(), "Path to jetty.base not found");

            // Make tmp directories exist
            Assert.assertTrue(pathToJettyBase.resolve("tmp").toFile().exists(), "Path to jetty.base/tmp/ not found");
            
            // Add testbed webapp
            final Path startIni = pathToJettyBase.resolve("start.ini");
            log.debug("Path to start.ini '{}'", startIni.toAbsolutePath());
            Assert.assertTrue(startIni.toAbsolutePath().toFile().exists(), "Path to start.ini not found");
            replaceFile(startIni, "\\Z", System.lineSeparator() + "testbed.xml");
        }
    }

    /**
     * Set up addresses the web server listens on and clients connect to.
     * <p/>
     * If the {@link #SERVER_ADDRESS_PROPERTY} system property exists, use it as the non-secure and secure server
     * address.
     * <p/>
     * If the {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} system property exists, use it as the non-secure and secure
     * private server address.
     * <p>
     * The private server address may be different than the server address, for example, when the server is behind NAT.
     * <p/>
     * If the {@link #SERVER_ADDRESS_PROPERTY} system property exists but {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} does
     * not, use it as both the non-secure and secure (1) server and (2) private server address.
     */
    @BeforeClass
    public void setUpAddresses() {
        final String envPrivateServerAddress = System.getProperty(PRIVATE_SERVER_ADDRESS_PROPERTY);
        log.debug("System property '{}' is '{}'", PRIVATE_SERVER_ADDRESS_PROPERTY, envPrivateServerAddress);

        final String envPublicServerAddress = System.getProperty(SERVER_ADDRESS_PROPERTY);
        log.debug("System property '{}' is '{}'", SERVER_ADDRESS_PROPERTY, envPublicServerAddress);

        if (envPublicServerAddress != null) {
            address = envPublicServerAddress;
            secureAddress = envPublicServerAddress;
            if (envPrivateServerAddress == null) {
                privateAddress = address;
                privateSecureAddress = secureAddress;
            }
        }

        if (envPrivateServerAddress != null) {
            privateAddress = envPrivateServerAddress;
            privateSecureAddress = envPrivateServerAddress;
        }
    }

    /**
     * Set up available ports between 20000 and 30000 for the web server to listen on as well as a port for the test
     * LDAP server;
     */
    @BeforeClass(enabled = true)
    public void setUpAvailablePorts() {
        if (Boolean.getBoolean("8080")) {
            return;
        }
        
        final SortedSet<Integer> ports = SocketUtils.findAvailableTcpPorts(4, 20000, 30000);
        final Iterator<Integer> iterator = ports.iterator();

        port = iterator.next();
        log.info("Selecting port '{}' for non-secure endpoints", port);

        securePort = iterator.next();
        log.info("Selecting port '{}' for secure endpoints", securePort);

        backchannelPort = iterator.next();
        log.info("Selecting port '{}' for backchannel endpoint", backchannelPort);

        ldapPort = iterator.next();
        log.info("Selecting port '{}' for LDAP", ldapPort);
        serverCommands.add("-D" + TEST_LDAP_PORT_PROPERTY + "=" + Integer.toString(ldapPort));
    }

    /**
     * Set up endpoint URLs using the {@link #address} and {@link #secureAddress}.
     */
    @BeforeClass(dependsOnMethods = {"setUpAddresses", "setUpAvailablePorts"})
    public void setUpBaseURLs() {
        final URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setScheme("http");
        urlBuilder.setHost(address);
        urlBuilder.setPort(port);
        baseURL = urlBuilder.buildURL();
        log.info("URL '{}' is the base URL which clients should connect to.", baseURL);

        final URLBuilder secureUrlBuilder = new URLBuilder();
        secureUrlBuilder.setScheme("https");
        secureUrlBuilder.setHost(secureAddress);
        secureUrlBuilder.setPort(securePort);
        secureBaseURL = secureUrlBuilder.buildURL();
        log.info("URL '{}' is the secure base URL which clients should connect to.", secureBaseURL);
    }

    /**
     * Set up endpoints by replacing "localhost" in configuration files.
     * 
     * <ul>
     * <li>Configure access in conf/access-control.xml</li>
     * <li>Configure LDAP port in conf/ldap.properties</li>
     * <li>Configure Jetty endpoints in jetty-base/start.d/idp.ini</li>
     * <li>Configure metadata endpoints in metadata/example-metadata.xml</li>
     * </ul>
     * 
     * @throws Exception if an error occurs.
     */
    @BeforeClass(dependsOnMethods = {"setUpBaseURLs", "setUpIdPPaths", "setUpJettyPaths", "setUpTomcatPaths", "setUpSauceLabsClientIPRange"})
    public void setUpEndpoints() throws Exception {

        // Access control from non-localhost.

        if (!clientIPRange.equalsIgnoreCase("127.0.0.1/32")) {
            replaceIdPHomeFile(Paths.get("conf", "access-control.xml"), "127\\.0\\.0\\.1/32", clientIPRange);
        }

        // LDAP port.
        replaceLDAPProperty("idp.authn.LDAP.ldapURL", "ldap://localhost:" + ldapPort);

        // Jetty endpoints.
        if (pathToJettyBase != null) {
            final Path pathToJettyIdPIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
            replaceProperty(pathToJettyIdPIni, "jetty.host", privateSecureAddress);
            replaceProperty(pathToJettyIdPIni, "jetty.https.port", Integer.toString(securePort));
            replaceProperty(pathToJettyIdPIni, "jetty.backchannel.port", Integer.toString(backchannelPort));
            replaceProperty(pathToJettyIdPIni, "jetty.nonhttps.host", privateAddress);
            replaceProperty(pathToJettyIdPIni, "jetty.nonhttps.port", Integer.toString(port));
        }
        
        // Tomcat endpoints.
        if (pathToTomcatBase != null) {
            final Path pathToCatalinaProperties = pathToTomcatBase.resolve(Paths.get("conf", "catalina.properties"));
            replaceFile(pathToCatalinaProperties, "tomcat.host=.*", "tomcat.host=" + privateSecureAddress);
            replaceFile(pathToCatalinaProperties, "tomcat.https.port=.*", "tomcat.https.port=" + Integer.toString(securePort));
            replaceFile(pathToCatalinaProperties, "tomcat.backchannel.port=.*", "tomcat.backchannel.port=" + Integer.toString(backchannelPort));
            replaceFile(pathToCatalinaProperties, "tomcat.nonhttps.host=.*", "tomcat.nonhttps.host=" + privateAddress);
            replaceFile(pathToCatalinaProperties, "tomcat.nonhttps.port=.*", "tomcat.nonhttps.port=" + Integer.toString(port));
        }

        // Metadata.
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "http://localhost:8080", baseURL);
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "https://localhost:8443", secureBaseURL);
    }

    /**
     * Set up debug logging for the IdP.
     * 
     * @throws Exception
     */
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpIdPPaths"})
    public void setUpDebugLogging() throws Exception {
        final Path pathToLogbackXML = Paths.get("conf", "logback.xml");

        // Set IdP logging to DEBUG from INFO.
        final String oldText = "<variable name=\"idp.loglevel.idp\" value=\"INFO\" />";
        final String newText = "<variable name=\"idp.loglevel.idp\" value=\"DEBUG\" />";
        replaceIdPHomeFile(pathToLogbackXML, oldText, newText);

        logUnencryptedSAML();

        // Add logging when starting the server.
        serverCommands.add("-Dlogback.configurationFile=" + pathToIdPHome.resolve(pathToLogbackXML).toAbsolutePath().toString());
    }

    /**
     * Set up example metadata provider for the IdP.
     * 
     * @throws Exception
     */
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpIdPPaths"})
    public void setUpExampleMetadataProvider() throws Exception {
        final Path pathToMetadataProvidersXML = Paths.get("conf", "metadata-providers.xml");

        final String oldText = "</MetadataProvider>";
        final String newText =
                "<MetadataProvider id=\"URLMD\" xsi:type=\"FilesystemMetadataProvider\" metadataFile=\"%{idp.home}/metadata/example-metadata.xml\" />"
                        + System.lineSeparator() + "</MetadataProvider>";
        replaceIdPHomeFile(pathToMetadataProvidersXML, oldText, newText);
    }

    /**
     * Add StorageServlet to IdP webapp.
     * 
     * @throws Exception
     */
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpIdPPaths"})
    public void setUpStorageServlet() throws Exception {

        final Path pathToIdPWebXML = pathToIdPHome.resolve(Paths.get("webapp", "WEB-INF", "web.xml"));
        Assert.assertTrue(pathToIdPWebXML.toAbsolutePath().toFile().exists(), "Path to IdP web.xml not found");

        final String oldText = "</web-app>";

        final StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("<!-- The /storage app space. Interact with storage services via HTTP. -->\n");
        builder.append("<servlet>\n");
        builder.append("    <servlet-name>storage</servlet-name>\n");
        builder.append("    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>\n");
        builder.append("    <init-param>\n");
        builder.append("        <param-name>contextConfigLocation</param-name>\n");
        builder.append("        <param-value>classpath:/system/conf/storage-context.xml</param-value>\n");
        builder.append("     </init-param>\n");
        builder.append("     <load-on-startup>1</load-on-startup>\n");
        builder.append("</servlet>\n");
        builder.append("<servlet-mapping>\n");
        builder.append("    <servlet-name>storage</servlet-name>\n");
        builder.append("    <url-pattern>/storage/*</url-pattern>\n");
        builder.append("</servlet-mapping>\n");
        builder.append("</web-app>\n");

        replaceFile(pathToIdPWebXML, oldText, builder.toString());
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to {@link #IDP_XML_SECURITY_MANAGER_PROP_VALUE}.
     * Save the previous value.
     */
    @BeforeClass
    public void setIdPXMLSecurityManager() {
        defaultIdpXMLSecurityManager = System.getProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME);
        System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, IDP_XML_SECURITY_MANAGER_PROP_VALUE);
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to the previous value.
     */
    @AfterClass
    public void restoreIdPXMLSecurityManager() {
        if (defaultIdpXMLSecurityManager != null) {
            System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, defaultIdpXMLSecurityManager);
        }
    }

    /**
     * Initialize XMLObject support classes.
     * 
     * @throws InitializationException
     */
    @BeforeClass(dependsOnMethods = {"setIdPXMLSecurityManager"})
    public void initializeXMLbjectSupport() throws InitializationException {
        InitializationService.initialize();
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    }

    /**
     * Do not use STARTTLS for LDAP connection to test in-memory directory server.
     *
     * @throws Exception
     */
    @BeforeClass(dependsOnMethods = {"setUpIdPPaths"})
    public void disableLDAPSTARTTLS() throws Exception {
        replaceLDAPProperty("idp.authn.LDAP.useStartTLS", "false");
    }

    /**
     * Start the web driver.
     * 
     * If the test is local, as defined by {@link #isLocal()}, then start a {@link HtmlUnitDriver}. Otherwise, start a
     * {@link RemoteWebDriver} on Sauce Labs.
     * 
     * Note : this method must be called in each test.
     * 
     * @param browserData the browser data
     * @throws Exception if an error occurs
     */
    public void startSeleniumClient(@Nullable final BrowserData browserData) throws Exception {
        setUpDesiredCapabilities(browserData);
        if (BaseIntegrationTest.isRemote()) {
            log.debug("Setting up remote web driver with desired capabilities '{}'", desiredCapabilities);
            setUpSauceDriver();
        } else {
            log.debug("Setting up local web driver with desired capabilities '{}'", desiredCapabilities);
            if (Boolean.getBoolean("firefox")) {
                setUpFirefoxDriver();
            } else {
                setUpHtmlUnitDriver();
            }
        }
        log.debug("Started web driver '{}' with desired capabilities '{}'", driver, desiredCapabilities);
    }

    /**
     * Quit the web driver.
     */
    @AfterMethod(enabled = true)
    public void stopSeleniumClient() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Start the IdP server. Uses Jetty by default, and Tomcat if the system property 'tomcat' is true.
     * 
     * Note : this method must be called in each test to allow for customization of the IdP configuration before the
     * server is started.
     * 
     * @throws ComponentInitializationException
     */
    public void startServer() throws ComponentInitializationException {
        if (Boolean.valueOf(System.getProperty("tomcat"))) {
            startTomcatServer();
        } else {
            startJettyServer();
        }
    }

    /**
     * Start the Jetty server.
     * 
     * @throws ComponentInitializationException if the server cannot be initialized
     */
    public void startJettyServer() throws ComponentInitializationException {

        serverCommands.add("-Djava.io.tmpdir=" + pathToJettyBase.resolve("tmp").toAbsolutePath());

        server = new JettyServerProcess();
        server.setServletContainerBasePath(pathToJettyBase);
        server.setServletContainerHomePath(pathToJettyHome);
        server.setAdditionalCommands(serverCommands);
        server.setStatusPageURL(getBaseURL() + StatusTest.statusPath);
        server.initialize();
        server.start();
    }

    /**
     * Start the Tomcat server.
     * 
     * @throws ComponentInitializationException if the server cannot be initialized
     */
    public void startTomcatServer() throws ComponentInitializationException {
        server = new TomcatServerProcess();
        server.setServletContainerBasePath(pathToTomcatBase);
        server.setServletContainerHomePath(pathToTomcatHome);
        server.setAdditionalCommands(serverCommands);
        server.setStatusPageURL(getBaseURL() + StatusTest.statusPath);
        server.initialize();
        server.start();
    }

    /**
     * Stop the server.
     */
    @AfterMethod
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Replace a property in conf/idp.properties.
     * 
     * @param key property key
     * @param value property value
     * @throws IOException if an I/O error occurs
     */
    public void replaceIdPProperty(@Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value)
            throws IOException {
        replaceProperty(pathToIdPProperties, key, value);
    }

    /**
     * Replace a property in conf/ldap.properties.
     * 
     * @param key property key
     * @param value property value
     * @throws IOException if an I/O error occurs
     */
    public void replaceLDAPProperty(@Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value)
            throws IOException {
        replaceProperty(pathToLDAPProperties, key, value);
    }

    /**
     * Replace a property in a properties file.
     * 
     * @param pathToPropertyFile path to the property file
     * @param key property key
     * @param value property value
     * @throws IOException if an I/O error occurs
     */
    public void replaceProperty(@Nonnull final Path pathToPropertyFile,
            @Nonnull @NotEmpty final String key,
            @Nonnull @NotEmpty final String value) throws IOException {
        Constraint.isNotNull(pathToPropertyFile, "Path to property file cannot be null nor empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Replacement property key cannot be null nor empty");
        Constraint.isNotNull(StringSupport.trimOrNull(value), "Replacement property value cannot be null nor empty");

        log.debug("Replacing property '{}' with '{}' in file '{}'", key, value, pathToPropertyFile);

        final FileSystemResource propertyResource =
                new FileSystemResource(pathToPropertyFile.toAbsolutePath().toString());

        final PropertiesWithComments pwc = new PropertiesWithComments();
        pwc.load(propertyResource.getInputStream());
        pwc.replaceProperty(key, value);
        pwc.store(propertyResource.getOutputStream());
    }

    /**
     * Replace contents of a conf file, whose path is relative to idp.home.
     * 
     * The regular expression is replaced with the replacement string and the file is over-written.
     * 
     * @param relativePath path to file relative to idp.home
     * @param regex regular expression to be replaced
     * @param replacement string to be substituted for each match
     * @throws IOException if the file cannot be overwritten
     */
    public void replaceIdPHomeFile(@Nonnull final Path relativePath,
            @Nonnull @NotEmpty final String regex,
            @Nonnull @NotEmpty final String replacement) throws IOException {
        replaceFile(pathToIdPHome.resolve(relativePath), regex, replacement);
    }

    /**
     * Replace contents of a file.
     * 
     * The regular expression is replaced with the replacement string and the file is over-written.
     * 
     * See {@link String#replaceAll(String, String)} and {@link Files#write(Path, byte[], java.nio.file.OpenOption...).
     * 
     * @param pathToFile path to the file
     * @param regex regular expression to be replaced
     * @param replacement string to be substituted for each match
     * @throws IOException if the file cannot be overwritten
     */
    public static void replaceFile(@Nonnull final Path pathToFile, @Nonnull @NotEmpty final String regex,
            @Nonnull @NotEmpty final String replacement) throws IOException {
        LoggerFactory.getLogger(BaseIntegrationTest.class).debug("Replacing regex '{}' with '{}' in file '{}'", regex,
                replacement, pathToFile);

        Assert.assertNotNull(pathToFile, "Path not found");
        Assert.assertTrue(pathToFile.toAbsolutePath().toFile().exists(), "Path does not exist");

        final Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(pathToFile), charset);
        content = content.replaceAll(regex, replacement);
        Files.write(pathToFile, content.getBytes(charset));
    }

    /**
     * Uncomment a commented regex from a file.
     * 
     * @param pathToFile path to the file
     * @param toUncomment regular expression to be uncommented
     * @throws IOException if the file cannot be overwritten
     */
    public void uncommentFile(@Nonnull final Path pathToFile, @Nonnull @NotEmpty final String toUncomment)
            throws IOException {
        final String commented = "\\<\\!--\\s+" + toUncomment + "\\s+--\\>";
        replaceFile(pathToFile, commented, toUncomment);
    }

    /**
     * Restore conf/idp.properties from dist/conf/idp.properties.dist.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void restoreIdPProperties() throws IOException {
        final Path pathToIdPPropertiesDist =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "dist", "conf", "idp.properties.dist");
        Assert.assertTrue(pathToIdPPropertiesDist.toFile().exists());

        Files.copy(pathToIdPPropertiesDist, pathToIdPProperties, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restore conf/relying-party.xml from dist/conf/relying-party.xml.dist.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void restoreRelyingPartyXML() throws IOException {
        final Path pathToRelyingParty =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "relying-party.xml");
        Assert.assertTrue(pathToRelyingParty.toFile().exists());

        final Path pathToRelyingPartyDist =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "dist", "conf", "relying-party.xml.dist");
        Assert.assertTrue(pathToRelyingPartyDist.toFile().exists());

        Files.copy(pathToRelyingPartyDist, pathToRelyingParty, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Enable per attribute consent.
     *
     * @throws IOException
     */
    public void enablePerAttributeConsent() throws IOException {
        replaceIdPProperty("idp.consent.allowPerAttribute", "true");
    }

    /**
     * Activate terms-of-use flow and include attribute statement.
     *
     * @throws Exception
     */
    public void enableCustomRelyingPartyConfiguration() throws Exception {
        final Path pathToRelyingPartyXML = Paths.get("conf", "relying-party.xml");

        final String oldIncludeAttributeStatementText =
                "<bean parent=\"Shibboleth.SSO\" p:postAuthenticationFlows=\"attribute-release\" />";
        final String newIncludeAttributeStatementText =
                "<bean parent=\"Shibboleth.SSO\" p:includeAttributeStatement=\"true\" p:postAuthenticationFlows=\"attribute-release\" />";
        replaceIdPHomeFile(pathToRelyingPartyXML, oldIncludeAttributeStatementText, newIncludeAttributeStatementText);

        final String oldPostAuthenticationFlowsText =
                "<bean parent=\"SAML2.SSO\" p:postAuthenticationFlows=\"attribute-release\" />";
        final String newPostAuthenticationFlowsText =
                "<bean parent=\"SAML2.SSO\" p:postAuthenticationFlows=\"#{ {'terms-of-use', 'attribute-release'} }\" />";
        replaceIdPHomeFile(pathToRelyingPartyXML, oldPostAuthenticationFlowsText, newPostAuthenticationFlowsText);
    }

    /**
     * Enable logout in conf/idp.properties.
     * 
     * @throws Exception
     */
    public void enableLogout() throws Exception {
        // server-side storage of user sessions
        replaceIdPProperty("idp.session.StorageService", "shibboleth.StorageService");

        // track information about SPs logged into
        replaceIdPProperty("idp.session.trackSPSessions", "true");

        // support lookup by SP for SAML logout
        replaceIdPProperty("idp.session.secondaryServiceIndex", "true");
    }

    /**
     * Enable CAS protocol for the default relying party.
     *
     * @throws IOException
     */
    public void enableCASProtocol() throws IOException {
        final Path pathToRelyingPartyXML = Paths.get("conf", "relying-party.xml");

        final String regex = "<ref bean=\"Liberty.SSOS\" />";
        final String replacement = regex + "\n" + "<ref bean=\"CAS.LoginConfiguration\" />\n"
                + "<ref bean=\"CAS.ProxyConfiguration\" />\n" + "<ref bean=\"CAS.ValidateConfiguration\" />";
        replaceIdPHomeFile(pathToRelyingPartyXML, regex, replacement);
    }

    /**
     * Add localhost to CAS service definition.
     * 
     * @throws IOException
     */
    public void enableLocalhostCASServiceDefinition() throws IOException {
        final Path pathToCASProtocolXML = Paths.get("conf", "cas-protocol.xml");

        final String regex = "</list>";
        final String replacement = "<bean class=\"net.shibboleth.idp.cas.service.ServiceDefinition\"\n"
                + "c:regex=\"https?://localhost(:\\\\d+)?/.*\"\n" + "p:group=\"test-services\"\n"
                + "p:authorizedToProxy=\"false\" />\n" + "</list>";
        replaceIdPHomeFile(pathToCASProtocolXML, regex, replacement);
    }

    /**
     * Release eduPersonAffiliation by adding a wildcard regex to attribute-filter.xml.
     * 
     * @throws IOException
     */
    public void enableLocalhostCASAttributes() throws IOException {
        final Path pathToLogbackXML = Paths.get("conf", "attribute-filter.xml");

        final String oldText = "<Rule xsi:type=\"Requester\" value=\"https://another.example.org/shibboleth\" />";
        final String newText = "<Rule xsi:type=\"RequesterRegex\" regex=\"https?://localhost(:\\\\d+)?/.*\" />";
        replaceIdPHomeFile(pathToLogbackXML, oldText, newText);
    }

    /**
     * Log unencrypted SAML.
     */
    public void logUnencryptedSAML() {
        final Path pathToLogbackXML = Paths.get("conf", "logback.xml");
        final String toUncomment = "<logger name=\"org.opensaml.saml.saml2.encryption.Encrypter\" level=\"DEBUG\" />";
        try {
            uncommentFile(pathToIdPHome.resolve(pathToLogbackXML), toUncomment);
        } catch (IOException e) {
            Assert.fail("Unable to log unencrypted SAML", e);
        }
    }

    /**
     * Populate the test name as the name of test class concatenated with the test method.
     * 
     * @param method the test method
     */
    @BeforeMethod
    public void setUpTestName(@Nonnull final Method method) {
        testName = method.getDeclaringClass().getName() + "." + method.getName();
    }

    /**
     * Set up HtmlUnitDriver web driver.
     */
    @BeforeMethod(enabled = false)
    public void setUpHtmlUnitDriver() throws IOException {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
    }

    /**
     * Set up Firefox web driver.
     */
    @BeforeMethod(enabled = false)
    public void setUpFirefoxDriver() throws IOException {
        final ProfilesIni allProfiles = new ProfilesIni();
        final FirefoxProfile profile = allProfiles.getProfile("FirefoxShibtest");
        driver = new FirefoxDriver(profile);

        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(1024, 768));
    }

    /**
     * Set up remote web driver to Sauce Labs.
     * 
     * Prefers credentials from system properties/environment variables as provided by Jenkins over ~/.sauce-ondemand,
     * see {@link SauceOnDemandAuthentication}.
     * 
     * @throws IOException
     */
    @BeforeMethod(enabled = false, dependsOnMethods = {"setUpTestName"})
    public void setUpSauceDriver() throws IOException {
        final SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();
        final String username = authentication.getUsername();
        final String accesskey = authentication.getAccessKey();
        final URL url = new URL("http://" + username + ":" + accesskey + "@ondemand.saucelabs.com:80/wd/hub");
        final RemoteWebDriver remoteWebDriver = new RemoteWebDriver(url, desiredCapabilities);
        threadLocalWebDriver.set(remoteWebDriver);
        driver = threadLocalWebDriver.get();
        threadLocalSessionId.set(remoteWebDriver.getSessionId().toString());
    }

    /**
     * Set up the desired capabilities.
     * 
     * Prefer capabilities as provided by Jenkins, defaults to Firefox.
     * 
     * Sets the test name to be displayed by Sauce Labs at
     * <a href="https://saucelabs.com/tests">https://saucelabs.com/tests</a>.
     * 
     * The desired capabilities will be overridden by the {@link #overrideCapabilities} if non-null.
     * 
     * @param browserData the browser data
     */
    public void setUpDesiredCapabilities(@Nullable final BrowserData browserData) {

        // name of test displayed on Sauce Labs
        desiredCapabilities.setCapability("name", testName);

        if (browserData != null) {
            // browser name
            if (browserData.getBrowser() != null) {
                desiredCapabilities.setBrowserName(browserData.getBrowser());
            }
            // browser version
            if (browserData.getVersion() != null) {
                desiredCapabilities.setCapability("version", browserData.getVersion());
            }
            // browser OS
            if (browserData.getOS() != null) {
                desiredCapabilities.setCapability(CapabilityType.PLATFORM,
                        Platform.extractFromSysProperty(browserData.getOS()));
            }
        }

        // Override desired capabilities.
        if (overrideCapabilities != null) {
            log.debug("Override desired capabilities with '{}'", overrideCapabilities);
            desiredCapabilities.merge(overrideCapabilities);
        }

        log.debug("Desired capabilities '{}'", desiredCapabilities);
        Reporter.log("Desired capabilities '" + desiredCapabilities + "'", true);
    }

    /**
     * A data provider which supplies {@link BrowserData} to test methods.
     * 
     * Prefer browser/OS/version triplets as provided by Jenkins via
     * {@link SauceBrowserDataProvider#SAUCE_ONDEMAND_BROWSERS} in the environment, if not present, defaults to
     * 'firefox'.
     * 
     * Wraps {@link SauceBrowserDataProvider#sauceBrowserDataProvider(Method)} to avoid the IllegalArgumentException
     * when the environment does not contain the desired property/variable.
     * 
     * @param testMethod the test method
     * @return data provider which supplies {@link BrowserData} to test methods
     */
    @DataProvider(name = "sauceOnDemandBrowserDataProvider", parallel = false)
    public static Iterator<Object[]> sauceOnDemandBrowserDataProvider(@Nonnull final Method testMethod) {
        final List<Object[]> data = new ArrayList<Object[]>();

        try {
            final Iterator<Object[]> iterator = SauceBrowserDataProvider.sauceBrowserDataProvider(testMethod);
            while (iterator.hasNext()) {
                final BrowserData browserData = new BrowserData();
                final Object[] array = iterator.next();
                if (array[0] != null) {
                    browserData.setBrowser(array[0].toString());
                }
                if (array[1] != null) {
                    browserData.setVersion(array[1].toString());
                }
                if (array[2] != null) {
                    browserData.setOS(array[2].toString());
                }
                data.add(new Object[] {browserData});
            }
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(BaseIntegrationTest.class).debug(
                    "Browser data provider did not find '{}' in environment, defaulting to 'firefox'",
                    SauceBrowserDataProvider.SAUCE_ONDEMAND_BROWSERS);
            data.add(new Object[] {new BrowserData().setBrowser("firefox")});
        }
        for (final Object[] array : data) {
            LoggerFactory.getLogger(BaseIntegrationTest.class).debug("Browser data provider '{}'", array);
        }

        return data.iterator();
    }

    /**
     * Whether Selenium is remote, as defined by the value of the {@link #SELENIUM_IS_REMOTE} property, which should be
     * either "true" or "false". Defaults to false.
     * 
     * @see com.saucelabs.testng.SauceOnDemandTestListener
     * 
     * @return whether Selenium is remote
     */
    public static boolean isRemote() {
        return (System.getProperty(SELENIUM_IS_REMOTE, "false").equalsIgnoreCase("true")) ? true : false;
    }

    /**
     * Whether the driver is Internet Explorer. Looks for desired capabilities with browser name of
     * {@link BrowserType#IE}.
     * 
     * @return whether the driver is Internet Explorer.
     */
    public boolean isInternetExplorer() {
        return desiredCapabilities.getBrowserName().equals(BrowserType.IE);
    }

    /**
     * Set up the client IP range used in conf/access-control.xml to Sauce Labs {@link #SAUCE_LABS_IP_RANGE} if Selenium
     * is not local.
     */
    @BeforeClass(enabled = true)
    public void setUpSauceLabsClientIPRange() {
        if (BaseIntegrationTest.isRemote()) {
            clientIPRange = SAUCE_LABS_IP_RANGE;
            log.info("Setting client IP range to '{}'", clientIPRange);
        }
    }

    /**
     * Set {@link #testClassFailed} to false before running tests in a class.
     */
    @BeforeClass
    public void setUpTestClassFailed() {
        testClassFailed = false;
    }

    /**
     * Set {@link #testClassFailed} to true if any test method failed.
     * 
     * @param result the TestNG test result
     */
    @AfterMethod
    public void failTestClass(@Nonnull final ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            testClassFailed = true;
        }
    }

    /**
     * Delete the per-test idp.home directory if there were no failures in the test class.
     */
    @AfterClass(enabled = true)
    public void deletePerTestIdPHomeDirectory() {
        if (testClassFailed) {
            log.debug("There was a test class failure, not deleting per-test idp.home directory '{}'",
                    pathToIdPHome.toAbsolutePath());
        } else if (!Boolean.getBoolean("keepTests")) {
            log.debug("Deleting per-test idp.home directory '{}'", pathToIdPHome.toAbsolutePath());
            FileSystemUtils.deleteRecursively(pathToIdPHome.toAbsolutePath().toFile());
        }
    }

    /** {@inheritDoc} */
    public SauceOnDemandAuthentication getAuthentication() {
        return sauceOnDemandAuthentication;
    }

    /** {@inheritDoc} */
    public String getSessionId() {
        return threadLocalSessionId.get();
    }

    /**
     * Get the web server base URL. For example, "http://localhost:8080" or "https://localhost:8443" if secure.
     * 
     * Whether the URL returned is secure or not is determined by {@link #useSecureBaseURL}.
     * 
     * @return the web server base URL
     */
    @NonnullAfterInit
    public String getBaseURL() {
        return getBaseURL(useSecureBaseURL);
    }

    /**
     * Get the web server base URL. For example, "http://localhost:8080" or "https://localhost:8443" if secure.
     * 
     * @param secure whether the URL should be secure
     * @return the web server base URL
     */
    @NonnullAfterInit
    public String getBaseURL(boolean secure) {
        return (secure == false) ? baseURL : secureBaseURL;
    }

    /**
     * Get the source of the last page loaded.
     * 
     * Handle Internet Explorer via {@link #cleanupPageSourceIE(String)}.
     * 
     * @return the source of the last page loaded or <code>null</code>
     */
    @Nullable
    public String getPageSource() {
        String pageSource = null;

        if (driver instanceof HtmlUnitDriver) {
            pageSource = driver.getPageSource();
        } else {
            pageSource = driver.findElement(By.tagName("body")).getText();
        }

        if (isInternetExplorer()) {
            pageSource = cleanupPageSourceIE(pageSource);
        }

        return pageSource;
    }

    /**
     * Strip default stylesheet strings.
     * 
     * <a href="https://msdn.microsoft.com/en-us/library/ms754529%28v=VS.85%29.aspx">Displaying XML Files in a
     * Browser</a>
     * 
     * @param pageSource page source with extraneous formatting
     * @return pageSource page source without extraneous formatting
     */
    public String cleanupPageSourceIE(@Nonnull final String pageSource) {
        log.trace("Page source:\n{}", pageSource);

        // Strip leading whitespace.
        String newPageSource = pageSource.replaceAll("^\\s+", "");

        // Strip leading " -".
        newPageSource = newPageSource.replaceAll("\n-\\s+", "\n");

        log.trace("New page source:\n{}", newPageSource);
        return newPageSource;
    }

    /**
     * Start the flow by accessing the URL composed of {@link #getBaseURL()} and {@link #startFlowURLPath}.
     */
    public void startFlow() {
        driver.get(getBaseURL() + startFlowURLPath);
    }

    /**
     * Wait for the login page at the URL composed of {@link #getBaseURL()} and {@link #loginPageURLPath}.
     */
    public void waitForLoginPage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(getBaseURL() + loginPageURLPath);
            }
        });
    }

    /**
     * Wait for page containing SAML response at URL composed of {@link #getBaseURL()} and {@link #responsePageURLPath}.
     */
    public void waitForResponsePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().equals(getBaseURL() + responsePageURLPath);
            }
        });
    }

    /**
     * <ul>
     * <li>Input username</li>
     * <li>Input password</li>
     * <li>Submit form.</li>
     * </ul>
     */
    public void login() {
        final WebElement username = driver.findElement(By.name("j_username"));
        final WebElement password = driver.findElement(By.name("j_password"));
        username.sendKeys("jdoe");
        password.sendKeys("changeit");
        submitForm();
    }

    /**
     * Wait for page with title {@link #TERMS_OF_USE_PAGE_TITLE}.
     */
    public void waitForTermsOfUsePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(TERMS_OF_USE_PAGE_TITLE);
            }
        });
    }

    /**
     * Accept terms of use by clicking the {@link #CONSENT_IDS_INPUT_NAME} checkbox.
     */
    public void acceptTermsOfUse() {
        final WebElement element = driver.findElement(By.name(CONSENT_IDS_INPUT_NAME));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Wait for page with title {@link #ATTRIBUTE_RELEASE_PAGE_TITLE}.
     */
    public void waitForAttributeReleasePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(ATTRIBUTE_RELEASE_PAGE_TITLE);
            }
        });
    }

    /**
     * Get and wait for testbed page at {@link #getBaseURL()}.
     */
    public void getAndWaitForTestbedPage() {
        driver.get(getBaseURL());
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().equals(getBaseURL() + "/");
            }
        });
    }

    /**
     * Wait for IdP logout page at URL composed of {@link #getBaseURL()} and {@link #idpLogoutURLPath}.
     */
    public void waitForLogoutPage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(getBaseURL() + idpLogoutURLPath);
            }
        });
    }

    /**
     * Wait for the page whose URL starts with the given prefix.
     * 
     * @param prefix the URL prefix
     */
    public void waitForPageWithURL(@Nonnull final String prefix) {
        Assert.assertNotNull(prefix);
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(prefix);
            }
        });
    }

    /**
     * Select web element with id {@link #REMEMBER_CONSENT_ID}.
     */
    public void releaseAllAttributes() {
        final WebElement element = driver.findElement(By.id(REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select release of email attribute only by selecting web element with id {@link #EMAIL_ID} and not selecting web
     * element with id {@link #EDU_PERSON_AFFILIATION_ID}.
     */
    public void releaseEmailAttributeOnly() {
        final WebElement email = driver.findElement(By.id(EMAIL_ID));
        if (!email.isSelected()) {
            email.click();
        }

        final WebElement eduPersonAffiliation = driver.findElement(By.id(EDU_PERSON_AFFILIATION_ID));
        if (eduPersonAffiliation.isSelected()) {
            eduPersonAffiliation.click();
        }

        final WebElement eduPersonPrincipalName = driver.findElement(By.id(EDU_PERSON_PRINCIPAL_NAME_ID));
        if (eduPersonPrincipalName.isSelected()) {
            eduPersonPrincipalName.click();
        }

        final WebElement uid = driver.findElement(By.id(UID_ID));
        if (uid.isSelected()) {
            uid.click();
        }
    }

    /**
     * Select web element with id {@link #REMEMBER_CONSENT_ID}.
     */
    public void rememberConsent() {
        final WebElement element = driver.findElement(By.id(REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select web element with id {@link #DO_NOT_REMEMBER_CONSENT_ID}.
     */
    public void doNotRememberConsent() {
        final WebElement element = driver.findElement(By.id(DO_NOT_REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select web element with id {@link #GLOBAL_CONSENT_ID}.
     */
    public void globalConsent() {
        final WebElement element = driver.findElement(By.id(GLOBAL_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Submit form by clicking element with name {@link #SUBMIT_FORM_INPUT_NAME}.
     */
    public void submitForm() {
        driver.findElement(By.name(SUBMIT_FORM_INPUT_NAME)).click();
    }

}
