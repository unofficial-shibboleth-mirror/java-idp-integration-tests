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
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
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

import com.saucelabs.common.SauceOnDemandAuthentication;

/**
 * Abstract integration test which starts a Jetty server and an in-memory directory server.
 */
public abstract class BaseIntegrationTest {

    /** Name of property defining the host that the web server listens on. */
    @Nonnull public final static String SERVER_ADDRESS_PROPERTY = "server.address";

    /** Directory in which distributions will be unpackaged. */
    @Nonnull public final static String TEST_DISTRIBUTIONS_DIRECTORY = "test-distributions";
    
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

    /** Jetty server process. */
    @NonnullAfterInit protected JettyServerProcess server;
    
    /** Additional commands used to start the Jetty server process. */
    @NonnullAfterInit protected List<String> serverCommands = new ArrayList<>();
    
    /** Non-secure web server base URL. Defaults to http://localhost:8080. */
    @NonnullAfterInit protected String baseURL;

    /** Non-secure port that the web server listens on. Defaults to 8080. */
    @Nonnull protected Integer port = 8080;

    /** Non-secure address that the web server listens on. Defaults to "localhost". */
    @Nonnull protected String address = "localhost";

    /** Secure web server base URL. Defaults to https://localhost:8443. */
    @NonnullAfterInit protected String secureBaseURL;

    /** Secure address that the web server listens on. Defaults to "localhost". */
    @Nonnull protected String secureAddress = "localhost";

    /** Secure port that the web server listens on. Defaults to 8443. */
    @Nonnull protected Integer securePort = 8443;

    /** Backchannel port that the web server listens on. Defaults to 9443. */
    @Nonnull protected Integer backchannelPort = 9443;

    /** Port that the test LDAP server listens on. Defaults to 10389. */
    @Nonnull protected Integer ldapPort = 10389;
    
    /** Whether to use the secure base URL by default. Defaults to false. */
    @Nonnull protected boolean useSecureBaseURL = false; 

    /** Path to idp.home. */
    @NonnullAfterInit protected Path pathToIdPHome;

    /** Path to conf/idp.properties. */
    @NonnullAfterInit protected Path pathToIdPProperties;

    /** Path to conf/ldap.properties. */
    @NonnullAfterInit protected Path pathToLDAPProperties;

    /** Path to jetty.base. */
    @NonnullAfterInit protected Path pathToJettyBase;

    /** Path to jetty.home. */
    @NonnullAfterInit protected Path pathToJettyHome;
    
    /** Pattern used when creating per test idp.home directory. Defaults to yyyyMMdd-HHmmssSS. **/
    @Nullable protected String idpHomePattern = "yyyyMMdd-HHmmssSS";

    /** Parser pool */
    @NonnullAfterInit protected ParserPool parserPool;

    /** XMLObject unmarshaller factory */
    @NonnullAfterInit protected UnmarshallerFactory unmarshallerFactory;

    /** Desired capabilities of the web driver. */
    @Nullable protected DesiredCapabilities desiredCapabilities;

    /** Web driver. */
    @Nonnull protected WebDriver driver;

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

    /** Name of test class concatenated with the test method. **/
    @Nullable protected String testName;
    
    /** Whether any test method in a class failed. **/
    @Nonnull protected boolean testClassFailed;

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    /**
     * Initialize XMLObject support classes.
     * 
     * @throws InitializationException
     */
    @BeforeClass public void initializeXMLbjectSupport() throws InitializationException {
        InitializationService.initialize();
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    }

    /**
     * Setup paths to the IdP and Jetty.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass public void setUpPaths() throws Exception {

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
        log.debug("Path to jetty.home '{}'", pathToJettyHome.toAbsolutePath());
        Assert.assertNotNull(pathToJettyHome, "Path to jetty.home not found");
        Assert.assertTrue(pathToJettyHome.toAbsolutePath().toFile().exists(), "Path to jetty.home not found");

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
        log.debug("Path to idp.home '{}'", pathToIdPHome.toAbsolutePath());
        Assert.assertFalse(pathToIdPHome.toAbsolutePath().toFile().exists(), "Path to idp.home already exists");
        
        // Copy idp.home directory from distribution to new per test directory
        final File sourceDir = pathToDistIdPHome.toAbsolutePath().toFile();
        final File destinationDir = pathToIdPHome.toAbsolutePath().toFile();
        FileSystemUtils.copyRecursively(sourceDir, destinationDir);
        Assert.assertTrue(destinationDir.exists(), "Path to idp.home not found");

        // Set idp.home system property
        System.setProperty("idp.home", pathToIdPHome.toAbsolutePath().toString());

        // Path to jetty.base
        pathToJettyBase = pathToIdPHome.resolve(Paths.get("jetty-base"));
        log.debug("Path to jetty.base '{}'", pathToJettyBase.toAbsolutePath());
        Assert.assertNotNull(pathToJettyBase, "Path to jetty.base not found");
        Assert.assertTrue(pathToJettyBase.toAbsolutePath().toFile().exists(), "Path to jetty.base not found");

        // Path to conf/idp.properties
        pathToIdPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "idp.properties");
        Assert.assertTrue(pathToIdPProperties.toFile().exists(), "Path to conf/idp.properties not found");

        // Path to conf/ldap.properties
        pathToLDAPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "ldap.properties");
        Assert.assertTrue(pathToLDAPProperties.toFile().exists(), "Path to conf/ldap.properties not found");
    }
    
    /**
     * Set up the address that the web server listens on, defaults to localhost. If a {@link #SERVER_ADDRESS_PROPERTY}
     * system property exists, use that as the address.
     * 
     */
    @BeforeClass public void setUpAddresses() {
        final String serverAddress = System.getProperty(SERVER_ADDRESS_PROPERTY);
        if (serverAddress != null) {
            address = serverAddress;
            secureAddress = serverAddress;
        }
    }

    /**
     * Set up available random ports between 20000 and 30000 for the web server to listen on as well as a port for the
     * test LDAP server;
     */
    @BeforeClass(enabled = true) public void setUpRandomPorts() {
        final SortedSet<Integer> ports = SocketUtils.findAvailableTcpPorts(4, 20000, 30000);
        final Iterator<Integer> iterator = ports.iterator();

        port = iterator.next();
        log.info("Non-secure port '{}'", port);

        securePort = iterator.next();
        log.info("Secure port '{}'", securePort);

        backchannelPort = iterator.next();
        log.info("Backchannel port '{}'", backchannelPort);

        ldapPort = iterator.next();
        log.info("LDAP port '{}'", ldapPort);
        serverCommands.add("-D" + TEST_LDAP_PORT_PROPERTY + "=" + Integer.toString(ldapPort));
    }

    /**
     * Set up endpoint URLs.
     */
    @BeforeClass(dependsOnMethods = {"setUpAddresses", "setUpRandomPorts"}) public void setUpBaseURLs() {
        final URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setScheme("http");
        urlBuilder.setHost(address);
        urlBuilder.setPort(port);
        baseURL = urlBuilder.buildURL();
        log.info("Base URL '{}'", baseURL);

        final URLBuilder secureUrlBuilder = new URLBuilder();
        secureUrlBuilder.setScheme("https");
        secureUrlBuilder.setHost(secureAddress);
        secureUrlBuilder.setPort(securePort);
        secureBaseURL = secureUrlBuilder.buildURL();
        log.info("Secure base URL '{}'", secureBaseURL);
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
    @BeforeClass(dependsOnMethods = {"setUpBaseURLs", "setUpPaths"}) public void setUpEndpoints() throws Exception {

        // Access control from non-localhost.
        if (!address.equalsIgnoreCase("localhost")) {
            replaceIdPHomeFile(Paths.get("conf", "access-control.xml"), "127\\.0\\.0\\.1/32", address + "/32");
        }

        // LDAP port.
        replaceLDAPProperty("idp.authn.LDAP.ldapURL", "ldap://localhost:" + ldapPort);

        // Jetty endpoints.
        final Path pathToJettyIdPIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
        replaceProperty(pathToJettyIdPIni, "jetty.host", secureAddress);
        replaceProperty(pathToJettyIdPIni, "jetty.https.port", Integer.toString(securePort));
        replaceProperty(pathToJettyIdPIni, "jetty.backchannel.port", Integer.toString(backchannelPort));
        replaceProperty(pathToJettyIdPIni, "jetty.nonhttps.host", address);
        replaceProperty(pathToJettyIdPIni, "jetty.nonhttps.port", Integer.toString(port));

        // Metadata.
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "http://localhost:8080", baseURL);
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "https://localhost:8443", secureBaseURL);
    }
    
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpPaths"}) public void setUpDebugLogging() throws Exception {
        final Path pathToLogbackXML = Paths.get("conf", "logback.xml");

        replaceIdPHomeFile(pathToLogbackXML, "<logger name=\"net.shibboleth.idp\" level=\"INFO\"/>",
                "<logger name=\"net.shibboleth.idp\" level=\"DEBUG\"/>");
        // TODO more
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to {@link #IDP_XML_SECURITY_MANAGER_PROP_VALUE}.
     * Save the previous value.
     */
    @BeforeClass public void setIdPXMLSecurityManager() {
        defaultIdpXMLSecurityManager = System.getProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME);
        System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, IDP_XML_SECURITY_MANAGER_PROP_VALUE);
    }

    /**
     * Set the {@link #IDP_XML_SECURITY_MANAGER_PROP_NAME} property to the previous value.
     */
    @AfterClass public void restoreIdPXMLSecurityManager() {
        if (defaultIdpXMLSecurityManager != null) {
            System.setProperty(IDP_XML_SECURITY_MANAGER_PROP_NAME, defaultIdpXMLSecurityManager);
        }
    }

    /**
     * Do not use STARTTLS for LDAP connection to test in-memory directory server.
     *
     * @throws Exception
     */
    @BeforeClass(dependsOnMethods = {"setUpPaths"}) public void disableLDAPSTARTTLS() throws Exception {
        replaceLDAPProperty("idp.authn.LDAP.useStartTLS", "false");
    }

    /**
     * Start the Jetty server.
     * 
     * Note : this method must be called in each test to allow for customization of the IdP configuration before the
     * server is started.
     * 
     * @throws ComponentInitializationException if the server cannot be initialized
     */
    public void startJettyServer() throws ComponentInitializationException {
        server = new JettyServerProcess(pathToJettyBase, pathToJettyHome, serverCommands);
        server.initialize();
        server.start();
    }

    /**
     * Stop the Jetty server.
     */
    @AfterMethod public void stopJettyServer() {
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
    public void replaceProperty(@Nonnull final Path pathToPropertyFile, @Nonnull @NotEmpty final String key,
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
    public void replaceIdPHomeFile(@Nonnull final Path relativePath, @Nonnull @NotEmpty final String regex,
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
    public void replaceFile(@Nonnull final Path pathToFile, @Nonnull @NotEmpty final String regex,
            @Nonnull @NotEmpty final String replacement) throws IOException {
        log.debug("Replacing regex '{}' with '{}' in file '{}'", regex, replacement, pathToFile);

        Assert.assertNotNull(pathToFile, "Path not found");
        Assert.assertTrue(pathToFile.toAbsolutePath().toFile().exists(), "Path does not exist");

        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(pathToFile), charset);
        content = content.replaceAll(regex, replacement);
        Files.write(pathToFile, content.getBytes(charset));
    }

    /**
     * Restore conf/idp.properties from dist/conf/idp.properties.dist.
     * 
     * @throws IOException if an I/O error occurs
     */
    @AfterMethod(alwaysRun = true) public void restoreIdPProperties() throws IOException {
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
    @AfterMethod(alwaysRun = true) public void restoreRelyingPartyXML() throws IOException {
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
     * Activate terms-of-use flow.
     *
     * @throws Exception
     */
    public void enableCustomRelyingPartyConfiguration() throws Exception {
        final Path pathToRelyingParty =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "relying-party.xml");
        Assert.assertTrue(pathToRelyingParty.toFile().exists());

        final Path pathToRelyingPartyWithConsent =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "relying-party-with-consent.xml");
        Assert.assertTrue(pathToRelyingPartyWithConsent.toFile().exists());

        Files.copy(pathToRelyingPartyWithConsent, pathToRelyingParty, StandardCopyOption.REPLACE_EXISTING);
    }

    public void enableLogout() throws Exception {
        // server-side storage of user sessions
        replaceIdPProperty("idp.session.StorageService", "shibboleth.StorageService");

        // track information about SPs logged into
        replaceIdPProperty(" idp.session.trackSPSessions", "true");

        // support lookup by SP for SAML logout
        replaceIdPProperty("idp.session.secondaryServiceIndex", "true");
    }

    /**
     * Populate the test name as the name of test class concatenated with the test method.
     * 
     * @param method the test method
     */
    @BeforeMethod public void setUpTestName(@Nonnull final Method method) {
        testName = method.getDeclaringClass().getName() + "." + method.getName();
    }

    /**
     * Set up HtmlUnitDriver web driver.
     */
    @BeforeMethod(enabled = false) public void setUpHtmlUnitDriver() throws IOException {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
    }

    /**
     * Set up Firefox web driver.
     */
    @BeforeMethod(enabled = false) public void setUpFirefoxDriver() throws IOException {
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
    @BeforeMethod(enabled = true, dependsOnMethods = {"setUpTestName"}) public void setUpSauceDriver() throws IOException {
        final SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();
        final String username = authentication.getUsername();
        final String accesskey = authentication.getAccessKey();
        final URL url = new URL("http://" + username + ":" + accesskey + "@ondemand.saucelabs.com:80/wd/hub");
        setUpDesiredCapabilities();
        driver = new RemoteWebDriver(url, desiredCapabilities);
    }

    /**
     * Set up the desired capabilities.
     * 
     * Prefer capabilities as provided by Jenkins, defaults to Firefox.
     * 
     * Sets the test name to be displayed by Sauce Labs at <a
     * href="https://saucelabs.com/tests">https://saucelabs.com/tests</a>.
     */
    public void setUpDesiredCapabilities() {

        if (System.getenv("SELENIUM_BROWSER") == null) {
            desiredCapabilities = DesiredCapabilities.firefox();
        } else {
            desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setBrowserName(System.getenv("SELENIUM_BROWSER"));
            desiredCapabilities.setVersion(System.getenv("SELENIUM_VERSION"));
            desiredCapabilities.setCapability(CapabilityType.PLATFORM, System.getenv("SELENIUM_PLATFORM"));
        }

        desiredCapabilities.setCapability("name", testName);
        
        log.debug("Desired capabilities '{}'", desiredCapabilities);
        
        System.out.println("Desired capabilities " + desiredCapabilities);
        System.out.println("SAUCE_ONDEMAND_HOST " + System.getenv("SAUCE_ONDEMAND_HOST"));
        
        Reporter.log("Desired capabilities " + desiredCapabilities, true);
        Reporter.log("SAUCE_ONDEMAND_HOST " + System.getenv("SAUCE_ONDEMAND_HOST"), true);
    }

    /**
     * Set {@link #testClassFailed} to false before running tests in a class.
     */
    @BeforeClass public void setUpTestClassFailed() {
        testClassFailed = false;
    }

    /**
     * Set {@link #testClassFailed} to true if any test method failed.
     * 
     * @param result the TestNG test result
     */
    @AfterMethod public void failTestClass(@Nonnull final ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            testClassFailed = true;
        }
    }

    /**
     * Delete the per-test idp.home directory if there were no failures in the test class.
     */
    @AfterClass(enabled = true) public void deletePerTestIdPHomeDirectory() {
        if (testClassFailed) {
            log.debug("There was a test class failure, not deleting per-test idp.home directory '{}'",
                    pathToIdPHome.toAbsolutePath());
        } else {
            log.debug("Deleting per-test idp.home directory '{}'", pathToIdPHome.toAbsolutePath());
            FileSystemUtils.deleteRecursively(pathToIdPHome.toAbsolutePath().toFile());
        }
    }

    /**
     * Quit the web driver.
     */
    @AfterMethod(enabled = true) public void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Get the web server base URL. For example, "http://localhost:8080" or "https://localhost:8443" if secure.
     * 
     * Whether the URL returned is secure or not is determined by {@link #useSecureBaseURL}.
     * 
     * @return the web server base URL
     */
    @NonnullAfterInit public String getBaseURL() {
        return getBaseURL(useSecureBaseURL);
    }

    /**
     * Get the web server base URL. For example, "http://localhost:8080" or "https://localhost:8443" if secure.
     * 
     * @param secure whether the URL should be secure
     * @return the web server base URL
     */
    @NonnullAfterInit public String getBaseURL(boolean secure) {
        return (secure == false) ? baseURL : secureBaseURL;
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
     * Get the source of the last page loaded.
     * 
     * @return the source of the last page loaded or <code>null</code>
     */
    @Nullable public String getPageSource() {
        String pageSource = null;

        if (driver instanceof FirefoxDriver) {
            pageSource = driver.findElement(By.tagName("body")).getText();
        } else {
            pageSource = driver.getPageSource();
        }

        return pageSource;
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
