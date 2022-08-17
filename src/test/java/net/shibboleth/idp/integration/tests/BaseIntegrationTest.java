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

package net.shibboleth.idp.integration.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.installer.PropertiesWithComments;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;
import org.testng.Assert;
import org.testng.ITestResult;
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
 * 
 * <p>
 * The Maven POM unpacks the IdP and Jetty distributions, adds test views and flows from idp-conf, and adds deployment
 * of the testbed. The testbed provides an in-memory directory server.
 * </p>
 * 
 * <p>
 * The IdP and testbed webapps are run via Jetty's start.jar in a separate {@link Process}, see
 * {@link JettyServerProcess}.
 * </p>
 * 
 * <p>
 * Each concrete subclass is associated with an idp.home directory, which is created by copying the unpacked IdP
 * distribution, see {@link #setUpIdPPaths()}. Consequently, each test method in a class uses the same idp.home directory.
 * This per-class idp.home directory is deleted only if all tests pass. The per-class idp.home directory name is a
 * timestamp whose pattern is defined by {@link #idpHomePattern}.
 * </p>
 * 
 * <p>
 * Ports for Jetty and the in-memory directory server will be automatically selected between the range of 20000 - 30000,
 * see {@link #setUpAvailablePorts()}.
 * </p>
 * 
 * <p>
 * Test methods should start clients via {@link #startSeleniumClient(BrowserData)} and start the server via
 * {@link #startJettyServer()}.
 * </p>
 * 
 * <p>
 * By default, tests run using a local browser. By default the {@link HtmlUnitDriver} will be used. To override, set the
 * {@link #driver} to the desired {@link WebDriver}. See {@link #startSeleniumClient(BrowserData)} for one way to
 * override.
 * </p>
 * 
 * <p>
 * To run tests using remote browsers provided by Sauce Labs, set the {@link #SELENIUM_IS_REMOTE} system property and
 * set the {@link #SERVER_ADDRESS_PROPERTY} to the publicly accessible IP address of the server to which clients should
 * connect to. You will also probably need to set the {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} to the IP address that
 * the server should be run on, which might be the same as the {@link #SERVER_ADDRESS_PROPERTY}.
 * </p>
 * 
 * <p>
 * With Sauce Labs, the browsers tested are defined by {@link SauceBrowserDataProvider#SAUCE_ONDEMAND_BROWSERS} in the
 * environment, which is a JSON string. See
 * <a href="https://docs.saucelabs.com/ci-integrations/jenkins/">https://docs.saucelabs.com/ci-integrations/jenkins/</a>
 * for details. This is populated by the Jenkins Sauce OnDemand Plugin. If this is not available via the environment,
 * the 'firefox' browser is used by default, see {@link #sauceOnDemandBrowserDataProvider(Method)}. To override the
 * 'firefox' browser, manipulate the {@link #desiredCapabilities} before calling
 * {@link #startSeleniumClient(BrowserData)}, for example:
 * </p>
 * 
 * <pre>
 * desiredCapabilities.setCapability("platform", "win8");
 * </pre>
 * 
 * <p>or</p>
 * 
 * <pre>
 * desiredCapabilities.setCapability(org.openqa.selenium.remote.CapabilityType.Platform,
 * org.openqa.selenium.Platform.WIN8);
 * </pre>
 * 
 * <p>
 * See {@link org.openqa.selenium.Platform}. Or, configure a new TestNG data provider.
 * </p>
 */
@Listeners({TestNameLogger.class, CustomSauceOnDemandTestListener.class})
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
    @Nonnull public final static List<String> SAUCE_LABS_IP_RANGES = Arrays.asList("162.222.72.0/21", "66.85.48.0/21", "185.94.24.0/22");

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

    /** Whether to use the secure base URL by default. Defaults to true. */
    @Nonnull protected boolean useSecureBaseURL = true;

    /** Client IP range to allow access from. Defaults to "127.0.0.1/32". */
    @Nonnull protected Set<String> clientIPRanges = new LinkedHashSet(Arrays.asList("127.0.0.1/32"));

    /** Path to idp.home. */
    @NonnullAfterInit protected Path pathToIdPHome;

    /** Path to idp distribution. */
    @NonnullAfterInit protected Path pathToIdPDist;

    /** Path to conf/idp.properties. */
    @NonnullAfterInit protected Path pathToIdPProperties;

    /** Path to conf/ldap.properties. */
    @NonnullAfterInit protected Path pathToLDAPProperties;
    
    /** Path to directory containing idp.home and jetty-base for each test. */
    @NonnullAfterInit protected Path pathToPerTestDirectory;

    /** Resource to messages.properties.*/
    @NonnullAfterInit protected Resource messagesPropertiesResource;

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

    /** IdP version determined from distribution name. **/
    @Nullable protected String idpVersion;
    
    /** Jetty version determined from distribution name. **/
    @Nullable protected String jettyVersion;

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

        // Path to idp distribution.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*shibboleth-identity-provider-*")) {
            for (Path entry : stream) {
                pathToIdPDist = entry;
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Path to idp distribution '{}'", pathToIdPDist.toAbsolutePath());
        Assert.assertNotNull(pathToIdPDist, "IdP distribution not found");
        Assert.assertTrue(pathToIdPDist.toAbsolutePath().toFile().exists(), "IdP distribution does not exist");

        // Determine IdP version from distribution name
        final Pattern pattern = Pattern.compile("shibboleth-identity-provider-(.*)");
        final Matcher matcher = pattern.matcher(pathToIdPDist.getFileName().toString());
        if (matcher.find()) {
            idpVersion = matcher.group(1);
        }
        log.debug("Testing IdP version '{}'", idpVersion);
        if (idpVersion == null || idpVersion.isBlank()) {
            log.error("Unable to determine version of IdP");
        }

        // Path to per-test idp.home
        final String timestamp = DateTimeFormatter.ofPattern(idpHomePattern).format(LocalDateTime.now());
        final String perTestDirectoryName = String.join("-", timestamp, getClass().getSimpleName());
        log.debug("Per-test directory name '{}'", perTestDirectoryName);
        pathToPerTestDirectory = buildPath.resolve(perTestDirectoryName); 
        log.debug("Per-test directory '{}'", pathToPerTestDirectory);
        pathToIdPHome = pathToPerTestDirectory.resolve("shibboleth-idp");
        log.debug("Path to idp.home '{}'", pathToIdPHome.toAbsolutePath());
        final File idpHome = pathToIdPHome.toAbsolutePath().toFile();
        Assert.assertFalse(idpHome.exists(), "Path to idp.home already exists");
        // Create empty idp.home directory
        Files.createDirectories(pathToIdPHome.toAbsolutePath());
        Assert.assertTrue(idpHome.exists(), "Path to idp.home not found");

        // Run installer from idp distribution directory
        final ProcessBuilder installerBuilder = new ProcessBuilder();
        installerBuilder.directory(pathToIdPDist.toFile());
        installerBuilder.redirectErrorStream();
        final List<String> installerCommands = new ArrayList();
        if (isWindows()) {
            final Path pathToInstallBat = pathToIdPDist.resolve(Paths.get("bin", "install.bat"));
            Assert.assertTrue(pathToInstallBat.toFile().exists());
            installerCommands.add(pathToInstallBat.toAbsolutePath().toString());
        } else {
            installerCommands.add("bin/install.sh");
            
        }
        // installerCommands.add("-h");
        installerCommands.add("-Didp.src.dir=" + pathToIdPDist.toAbsolutePath());
        installerCommands.add("-Didp.target.dir=" + pathToIdPHome.toAbsolutePath());
        installerCommands.add("-Didp.host.name=idp");
        installerCommands.add("-Didp.scope=example.org");
        installerCommands.add("-Didp.entityID=https://idp.example.org");
        installerCommands.add("-Didp.keystore.password=password");
        installerCommands.add("-Didp.sealer.password=password");
        installerBuilder.command(installerCommands);
        // Run installer
        final Process installerProcess = installerBuilder.start();
        logProcess(installerProcess, "install :");

        // List and enable modules
        if (isWindows()) {
            final Path pathToModuleBat = pathToIdPHome.resolve(Paths.get("bin", "module.bat"));
            Assert.assertTrue(pathToModuleBat.toFile().exists());
            final String moduleBat = pathToModuleBat.toAbsolutePath().toString();
            logProcess(Runtime.getRuntime().exec(moduleBat + " -l", null, idpHome), "module :");
            logProcess(Runtime.getRuntime().exec(moduleBat + " -e idp.intercept.Consent", null, idpHome),"module :");
            logProcess(Runtime.getRuntime().exec(moduleBat + " -e idp.profile.CAS", null, idpHome), "module :");
            logProcess(Runtime.getRuntime().exec(moduleBat + " -l", null, idpHome), "module :");
        } else {
            logProcess(Runtime.getRuntime().exec("bin/module.sh -l", null, idpHome), "module :");
            logProcess(Runtime.getRuntime().exec("bin/module.sh -e idp.intercept.Consent", null, idpHome),"module :");
            logProcess(Runtime.getRuntime().exec("bin/module.sh -e idp.profile.CAS", null, idpHome), "module :");
            logProcess(Runtime.getRuntime().exec("bin/module.sh -l", null, idpHome), "module :");
        }

        // Expand idp.war to webapp/ directory (so web.xml can be modified later if need be)
        final Path pathToWebappDir = pathToIdPHome.resolve("webapp").toAbsolutePath();
        log.debug("Path to webapp dir '{}'", pathToWebappDir);
        Files.createDirectory(pathToWebappDir);
        Assert.assertTrue(pathToWebappDir.toFile().exists());
        if (isWindows()) {
            logProcess(Runtime.getRuntime().exec("jar -xvf ..\\war\\idp.war", null, pathToWebappDir.toFile()), "jar :");
        } else {
            logProcess(Runtime.getRuntime().exec("jar -xvf ../war/idp.war", null, pathToWebappDir.toFile()), "jar :");
        }

        // Copy directories from idp distribution to idp home
        copyFromIdPDistToIdPHome("metadata");
        copyFromIdPDistToIdPHome("credentials");
        copyFromIdPDistToIdPHome("testbed-war");

        // Set idp.home system property, replace '\' with '/' for Windows
        System.setProperty("idp.home", pathToIdPHome.toAbsolutePath().toString().replace('\\', '/'));

        // Path to conf/idp.properties
        pathToIdPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "idp.properties");
        Assert.assertTrue(pathToIdPProperties.toFile().exists(), "Path to conf/idp.properties not found");

        // Path to conf/ldap.properties
        pathToLDAPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "ldap.properties");
        Assert.assertTrue(pathToLDAPProperties.toFile().exists(), "Path to conf/ldap.properties not found");
        
        // Classpath messages.properties
        messagesPropertiesResource = new ClassPathResource("/net/shibboleth/idp/messages/messages.properties");
        Assert.assertTrue(messagesPropertiesResource.exists(), "Classpath resource messages.properties not found");
        log.debug("Path to message properties '{}'", messagesPropertiesResource);
    }
    
    /**
     * Enable one or more modules.
     * 
     * @param modules modules to enable
     * @param idpHome location of IdP
     * 
     * @throws ModuleException if an error occurs
     */
    protected void enableModules(@Nonnull @NonnullElements final Collection<String> modules,
            @Nonnull final Path idpHome) throws ModuleException {

        final ModuleContext context = new ModuleContext(idpHome);
        log.trace("Module context {}", context);

        for (final IdPModule module : ServiceLoader.load(IdPModule.class)) {
            log.trace("Found module {}", module);
            if (modules.contains(module.getId())) {
                log.trace("Enabling module {}", module);
                module.enable(context);
            }
        }
    }

    /**
     * Set up paths to Tomcat if they exist.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass(dependsOnMethods = {"setUpIdPPaths"})
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

            // Copy tomcat-base
            copyFromIdPDistToIdPHome("tomcat-base");

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
     * Set up paths to Jetty if they exist and if 'tomcat' system property is not true.
     * 
     * @throws Exception if an error occurs
     */
    @BeforeClass(dependsOnMethods = {"setUpIdPPaths"})
    public void setUpJettyPaths() throws Exception {

        if (Boolean.getBoolean("tomcat")) {
            log.debug("Not setting up Jetty because system property 'tomcat' is true");
            return;
        }

        // Path to the project build directory.
        final Path buildPath = Paths.get(TEST_DISTRIBUTIONS_DIRECTORY);
        log.debug("Path to build directory '{}'", buildPath.toAbsolutePath());
        Assert.assertTrue(buildPath.toAbsolutePath().toFile().exists(), "Path to build directory not found");

        // Path to Jetty distribution
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*jetty-home-*")) {
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

            // Determine Jetty version from distribution name
            final Pattern pattern = Pattern.compile("jetty-home-(.*)");
            final Matcher matcher = pattern.matcher(pathToJettyHome.getFileName().toString());
            if (matcher.find()) {
                jettyVersion = matcher.group(1);
            }
            log.debug("Testing Jetty version '{}'", jettyVersion);
            if (jettyVersion == null || jettyVersion.isBlank()) {
                log.error("Unable to determine version of Jetty");
            }

            // Path to jetty.base
            pathToJettyBase = pathToIdPHome.getParent().resolve(Paths.get("jetty-base"));
            log.debug("Path to jetty.base '{}'", pathToJettyBase.toAbsolutePath());

            // Copy jetty-base
            final Path pathToJettyBaseDist = buildPath.resolve("jetty-base");
            FileSystemUtils.copyRecursively(pathToJettyBaseDist, pathToJettyBase);
            Assert.assertNotNull(pathToJettyBase, "Path to jetty.base not found");
            Assert.assertTrue(pathToJettyBase.toAbsolutePath().toFile().exists(), "Path to jetty.base not found");

            // Make tmp directories exist
            Assert.assertTrue(pathToJettyBase.resolve("tmp").toFile().exists(), "Path to jetty.base/tmp/ not found");
            
            // set idp.home system property
            serverCommands.add(0, "-Didp.home=" + System.getProperty("idp.home"));
            // set idp.war.path system property to webapp directory
            serverCommands.add("-Didp.war.path=" + pathToIdPHome.resolve("webapp").toAbsolutePath());
            // set tmp directory system property
            serverCommands.add("-Djava.io.tmpdir=" + pathToJettyBase.resolve("tmp").toAbsolutePath());
        } else {
            Assert.fail("Unable to find jetty.home");
        }
    }

    /**
     * Disable SNI host check by adding jetty.ssl.sniHostCheck=false to idp.ini for Jetty after version 9.
     * 
     * @throws IOException
     */
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpJettyPaths"})
    public void setUpJettySNI() throws IOException {
        if (Boolean.getBoolean("tomcat")) {
            log.debug("Not setting up Jetty WebInfIncludeJarPattern because system property 'tomcat' is true");
            return;
        }
        if (jettyVersion.startsWith("9")) {
            return;
        } ;
        final Path idpIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
        log.debug("Path to idp.ini '{}'", idpIni.toAbsolutePath());
        if (idpIni.toAbsolutePath().toFile().exists()) {
            log.debug("Disabling Jetty SNI host check for version '{}'", jettyVersion);
            replaceFile(idpIni, "\\z", System.lineSeparator() + "jetty.ssl.sniHostCheck=false");
        } else {
            Assert.fail("Unable to find idp.ini");
        }
    }

    /**
     * Add testbed webapp to Jetty.
     * 
     * <ul>
     * <li>Add "testbed.xml" to either start.ini for Jetty 9.3 or start.d/idp.ini for Jetty 9.4</li>
     * <li>Modify path to IdP webapp in webapps/idp.xml for Jetty 9.4</li>
     * </ul>
     * 
     * Must run after {@link #setUpEndpoints()} so that {@link PropertiesWithComments} does not add a breaking "=" to
     * "testbed.xml".
     * 
     * @throws IOException ...
     */
    @BeforeClass(enabled = true, dependsOnMethods = {"setUpEndpoints"}) // must run after setUpEndpoints
    public void setUpJettyTestbed() throws IOException {

        if (pathToJettyBase == null) {
            log.debug("Not setting up jetty-base because directory does not exist.");
            return;
        }

        // Jetty 9.3
        final Path startIni = pathToJettyBase.resolve("start.ini");
        log.debug("Path to start.ini '{}'", startIni.toAbsolutePath());

        // Jetty 9.4
        final Path idpIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
        log.debug("Path to idp.ini '{}'", idpIni.toAbsolutePath());

        // Add testbed to either start.ini or start.d/idp.ini
        if (startIni.toAbsolutePath().toFile().exists()) {
            replaceFile(startIni, "\\z", System.lineSeparator() + "testbed.xml");
        } else if (idpIni.toAbsolutePath().toFile().exists()) {
            replaceFile(idpIni, "\\z", System.lineSeparator() + "testbed.xml");
        } else {
            Assert.fail("Unable to find start.ini or idp.ini");
        }
    }
 
    /**
     * Set up addresses the web server listens on and clients connect to.
     * 
     * <p>
     * If the {@link #SERVER_ADDRESS_PROPERTY} system property exists, use it as the non-secure and secure server
     * address.
     * </p>
     * 
     * <p>
     * If the {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} system property exists, use it as the non-secure and secure
     * private server address.
     * </p>
     * 
     * <p>
     * The private server address may be different than the server address, for example, when the server is behind NAT.
     * </p>
     * 
     * <p>
     * If the {@link #SERVER_ADDRESS_PROPERTY} system property exists but {@link #PRIVATE_SERVER_ADDRESS_PROPERTY} does
     * not, use it as both the non-secure and secure (1) server and (2) private server address.
     * </p>
     * @throws Exception if unable to retrieve IP addresses from EC2
     */
    @BeforeClass
    public void setUpAddresses() throws Exception {
        final String envPrivateServerAddress = System.getProperty(PRIVATE_SERVER_ADDRESS_PROPERTY);
        log.debug("System property '{}' is '{}'", PRIVATE_SERVER_ADDRESS_PROPERTY, envPrivateServerAddress);

        final String envPublicServerAddress = System.getProperty(SERVER_ADDRESS_PROPERTY);
        log.debug("System property '{}' is '{}'", SERVER_ADDRESS_PROPERTY, envPublicServerAddress);

        if (Boolean.getBoolean("EC2")) {
            final String publicIPV4address = getEC2PublicIPV4();
            address = publicIPV4address;
            secureAddress = publicIPV4address;

            final String privateIPV4address = getEC2PrivateIPV4();
            privateAddress = privateIPV4address;
            privateSecureAddress = privateIPV4address;
        }

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
        log.debug("Selecting port '{}' for non-secure endpoints", port);

        securePort = iterator.next();
        log.debug("Selecting port '{}' for secure endpoints", securePort);

        backchannelPort = iterator.next();
        log.debug("Selecting port '{}' for backchannel endpoint", backchannelPort);

        ldapPort = iterator.next();
        log.debug("Selecting port '{}' for LDAP", ldapPort);
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
        log.debug("URL '{}' is the base URL which clients should connect to.", baseURL);

        final URLBuilder secureUrlBuilder = new URLBuilder();
        secureUrlBuilder.setScheme("https");
        secureUrlBuilder.setHost(secureAddress);
        secureUrlBuilder.setPort(securePort);
        secureBaseURL = secureUrlBuilder.buildURL();
        log.debug("URL '{}' is the secure base URL which clients should connect to.", secureBaseURL);
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

        // Add public server addresses if not localhost
        if (address != "localhost") {
            clientIPRanges.add(address + "/32");
        }
        if (secureAddress != "localhost") {
            clientIPRanges.add(secureAddress + "/32");
        }
        // Add private server addresses if not localhost
        if (privateAddress != "localhost") {
            clientIPRanges.add(privateAddress + "/32");
        }
        if (privateSecureAddress != "localhost") {
            clientIPRanges.add(privateSecureAddress + "/32");
        }

        final List<String> escapedClientIPRanges = new ArrayList<>();
        for(final String clientIPRange : clientIPRanges) {
            escapedClientIPRanges.add("'" + clientIPRange + "'");
        }
        String newClientIPRange = String.join(" , ", escapedClientIPRanges);

        log.debug("setUpEndpoints newClientIPRange {}", newClientIPRange);
        replaceIdPHomeFile(Paths.get("conf", "access-control.xml"), "'127\\.0\\.0\\.1/32'", newClientIPRange);

        // LDAP port.
        replaceLDAPProperty("idp.authn.LDAP.ldapURL", "ldap://localhost:" + ldapPort);

        // Jetty endpoints.
        if (pathToJettyBase != null) {
            final Path pathToJettyIdPIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));

            // Jetty with jetty-base 9.4+ listens on all addresses by default

            // Set SSL port to public secure port
            replaceProperty(pathToJettyIdPIni, "jetty.ssl.port", Integer.toString(securePort));

            // Jetty 9.4 backchannel host and port
            final Path backchannelIni = pathToJettyBase.resolve(Paths.get("start.d", "idp-backchannel.ini"));
            if (backchannelIni.toAbsolutePath().toFile().exists()) {
                replaceProperty(backchannelIni, "idp.backchannel.port", Integer.toString(backchannelPort));
            }
        }
        
        // Tomcat endpoints.
        if (pathToTomcatBase != null) {
            final Path pathToCatalinaProperties = pathToTomcatBase.resolve(Paths.get("conf", "catalina.properties"));
            replaceFile(pathToCatalinaProperties, "tomcat.host=.*", "tomcat.host=" + privateSecureAddress);
            replaceFile(pathToCatalinaProperties, "tomcat.https.port=.*", "tomcat.https.port=" + Integer.toString(securePort));
            replaceFile(pathToCatalinaProperties, "tomcat.backchannel.port=.*", "tomcat.backchannel.port=" + Integer.toString(backchannelPort));
        }

        // Metadata.
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "http://localhost:8080", baseURL);
        replaceIdPHomeFile(Paths.get("metadata", "example-metadata.xml"), "https://localhost:8443", secureBaseURL);
    }

    /**
     * Use non-secure port (default 8080) rather than secure port (default 8443).
     * 
     * Enables Jetty http module.
     * Sets jetty.http.port to non-secure port.
     * Disable session cookie security.
     * Sets idp.cookie.secure to false.
     * 
     * @throws IOException if unable to set up non-secure port
     */
    public void setUpNonSecurePort() throws IOException {
        // Add http module to Jetty
        final Path pathToIdPMod = pathToJettyBase.resolve(Paths.get("modules", "idp.mod"));
        replaceFile(pathToIdPMod, "https", "http\nhttps");

        // Set HTTP port used by Jetty
        final Path pathToIdpIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
        replaceFile(pathToIdpIni, "\\z", System.lineSeparator() + "jetty.http.port=" + port);

        // Set secure container session cookie to false
        final Path pathToIdPWebXML = Paths.get("webapp", "WEB-INF", "web.xml");
        replaceIdPHomeFile(pathToIdPWebXML, "<secure>true</secure>", "<secure>false</secure>");

        // Set secure cookies to false
        replaceIdPProperty("idp.cookie.secure", "false");

        useSecureBaseURL = false;
    }

    /**
     * If system property 'no-secure' is 'true', use non-secure port.
     * 
     * @see #setUpNonSecurePort()
     * 
     * @throws IOException if unable to set up non-secure port
     */
    @BeforeClass(dependsOnMethods = {"setUpEndpoints"})
    public void setUpNonSecurePortFromSystemProperties() throws IOException {
        if (Boolean.getBoolean("no-secure")) {
            setUpNonSecurePort();
        }
    }

    /**
     * Set up debug logging for the IdP.
     * 
     * @throws Exception if something bad happens
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
        // serverCommands.add("-Dlogback.configurationFile=" + pathToIdPHome.resolve(pathToLogbackXML).toAbsolutePath().toString());
    }

    /**
     * Set up example metadata provider for the IdP.
     * 
     * @throws Exception if something bad happens
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
     * @throws Exception if something bad happens
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

    @BeforeClass(enabled = true, dependsOnMethods = {"setUpIdPPaths", "setUpJettyPaths"})
    public void setUpIdPWebApp() throws Exception {
        if (Boolean.getBoolean("tomcat")) {
            log.debug("Not setting up Jetty WebInfIncludeJarPattern because system property 'tomcat' is true");
            return;
        }

        final Path pathToIdPXML = pathToJettyBase.resolve(Paths.get("webapps", "idp.xml"));
        Assert.assertTrue(pathToIdPXML.toAbsolutePath().toFile().exists(), "Path to idp.xml not found");

        final String oldText = "</Configure>";

        final StringBuilder builder = new StringBuilder();
        builder.append("  <Call name=\"setAttribute\">\n");
        builder.append("    <Arg>org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern</Arg>\n");
        builder.append("    <Arg>none</Arg>\n");
        builder.append("  </Call>\n");
        builder.append("</Configure>\n");

        replaceFile(pathToIdPXML, oldText, builder.toString());
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
     * @throws InitializationException ...
     */
    @BeforeClass(dependsOnMethods = {"setIdPXMLSecurityManager"})
    public void initializeXMLbjectSupport() throws InitializationException {
        InitializationService.initialize();
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    }

    /**
     * Start the web driver (browser).
     * 
     * If test is remote, as defined by {@link #isRemote()}, then start a {@link RemoteWebDriver} on Sauce Labs.
     * 
     * If test is local and 'SELENIUM_BROWSER' system property is 'safari', start a Safari web driver.
     * 
     * If test is local and 'SELENIUM_BROWSER' system property is 'chrome', throw an IllegalArgumentException.
     * 
     * Otherwise, start a Firefox web driver.
     * 
     * <p>
     * Note : this method must be called in each test.
     * </p>
     * 
     * @param browserData the platform+browser+version triplet
     * 
     * @throws IllegalArgumentException if browser is chrome
     * @throws Exception if an error occurs
     */
    public void startSeleniumClient(@Nullable final BrowserData browserData) throws Exception {
        setUpDesiredCapabilities(browserData);
        if (BaseIntegrationTest.isRemote()) {
            log.debug("Setting up remote web driver with desired capabilities '{}'", desiredCapabilities);
            setUpSauceDriver();
        } else if (browserData.getBrowser().equalsIgnoreCase("chrome")) {
            throw new IllegalArgumentException("Chrome web driver not supported");
        } else if (browserData.getBrowser().equalsIgnoreCase("safari")) {
            log.debug("Setting up local Safari web driver with desired capabilities '{}'", desiredCapabilities);
            setUpSafariDriver();
        } else {
            log.debug("Setting up local Firefox web driver with desired capabilities '{}'", desiredCapabilities);
            setUpFirefoxDriver();
        }
        log.debug("Started web driver '{}' with desired capabilities '{}'", driver, desiredCapabilities);
    }

    /**
     * Quit the web driver.
     */
    @AfterMethod(enabled = true)
    public void stopSeleniumClient() {
        if (driver != null) {
            log.debug("Stopping Selenium client");
            driver.quit();
        }
    }

    /**
     * Start the IdP server. Uses Jetty by default, and Tomcat if the system property 'tomcat' is true.
     * 
     * <p>
     * Note : this method must be called in each test to allow for customization of the IdP configuration before the
     * server is started.
     * </p>
     * 
     * @throws ComponentInitializationException ...
     */
    public void startServer() throws ComponentInitializationException {
        if (Boolean.getBoolean("tomcat")) {
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
    @AfterMethod(dependsOnMethods = {"failTestClass", "stopSeleniumClient"})
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
    
    /**
     * Get a message value from <code>messages.properties</code> relating to the <code>key</code> argument.
     * <p> Can NOT be used to get messages for different languages, only the default system message bundle.<p>
     * 
     * @param key the key used to lookup the value.
     * @return the value to which the specified key is mapped, or {@literal null} if the key does not exist or the
     *              value is not a {@link String}. 
     * @throws IOException if there is an error loading the messages.properties file.
     */
    @Nullable
    protected String getMessage(@Nonnull @NotEmpty final String key) throws IOException {
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Replacement property key cannot be null nor empty");

        log.debug("Finding message property '{}' in resource '{}'", key, messagesPropertiesResource);

        final Properties props = new Properties();
        props.load(messagesPropertiesResource.getInputStream());
        Object propValueObject =  props.get(key);
        if (propValueObject instanceof String) {
            return (String)propValueObject;
        }
        return null;
        
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
        replaceProperty(pathToLDAPProperties, key, value, false);
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
        replaceProperty(pathToPropertyFile, key, value, false);
    }

    /**
     * Replace a property in a properties file.
     * 
     * @param pathToPropertyFile path to the property file
     * @param key property key
     * @param value property value
     * @param addNewProperties add new properties as well as replace existing
     * @throws IOException if an I/O error occurs
     */
    public void replaceProperty(@Nonnull final Path pathToPropertyFile, @Nonnull @NotEmpty final String key,
            @Nonnull @NotEmpty final String value, final boolean addNewProperties) throws IOException {
        Constraint.isNotNull(pathToPropertyFile, "Path to property file cannot be null nor empty");
        Constraint.isNotNull(StringSupport.trimOrNull(key), "Replacement property key cannot be null nor empty");
        Constraint.isNotNull(StringSupport.trimOrNull(value), "Replacement property value cannot be null nor empty");

        log.debug("Replace property '{}' with '{}' in file '{}'", key, value, pathToPropertyFile);

        final FileSystemResource propertyResource =
                new FileSystemResource(pathToPropertyFile.toAbsolutePath().toString());

        final PropertiesWithComments pwc = new PropertiesWithComments();
        pwc.load(propertyResource.getInputStream());
        boolean wasPropertyReplaced = pwc.replaceProperty(key, value);
        if (wasPropertyReplaced || addNewProperties) {
            log.debug("Replacing property '{}' with '{}' in file '{}'", key, value, pathToPropertyFile);
            pwc.store(propertyResource.getOutputStream());
        }
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
     * <p>
     * The regular expression is replaced with the replacement string and the file is over-written.
     * </p>
     * 
     * <p>
     * See {@link String#replaceAll(String, String)} and {@link Files#write(Path, byte[], java.nio.file.OpenOption...)}.
     * </p>
     * 
     * @param pathToFile path to the file
     * @param regex regular expression to be replaced
     * @param replacement string to be substituted for each match
     * 
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
     * Restore conf/idp.properties from dist/conf/idp.properties.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void restoreIdPProperties() throws IOException {
        final Path pathToIdPPropertiesDist =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "dist", "conf", "idp.properties");
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
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "dist", "conf", "relying-party.xml");
        Assert.assertTrue(pathToRelyingPartyDist.toFile().exists());

        Files.copy(pathToRelyingPartyDist, pathToRelyingParty, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Enable per attribute consent.
     *
     * @throws IOException ...
     */
    public void enablePerAttributeConsent() throws IOException {
        replaceIdPProperty("idp.consent.allowPerAttribute", "true");
    }

    /**
     * Activate terms-of-use flow and include attribute statement.
     *
     * @throws Exception if something bad happens
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
     * Disable Local Storage in conf/idp.properties.
     * 
     * @throws Exception if something bad happens
     */
    public void disableLocalStorage() throws Exception {
        replaceIdPProperty("idp.storage.htmlLocalStorage", "false");
    }
    
    /**
     * Enable logout in conf/idp.properties.
     * 
     * @throws Exception if something bad happens
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
     * Enable CAS for the default relying party.
     *
     * @throws IOException if an error occurs replacing text in relying-party.xml
     * @throws ModuleException if an error occurs
     */
    public void enableCAS() throws IOException, ModuleException {
        final Path pathToRelyingPartyXML = Paths.get("conf", "relying-party.xml");

        final String regex = "<ref bean=\"Liberty.SSOS\" />";
        final String replacement = regex + "\n" + "<ref bean=\"CAS.LoginConfiguration\" />\n"
                + "<ref bean=\"CAS.ProxyConfiguration\" />\n" + "<ref bean=\"CAS.ValidateConfiguration\" />";
        replaceIdPHomeFile(pathToRelyingPartyXML, regex, replacement);
        
        if (!idpVersion.startsWith("3")) {
            enableModules(Collections.singletonList("idp.profile.CAS"), pathToIdPHome);
        }
    }

    /**
     * Add localhost to CAS service definition.
     * 
     * @throws IOException ...
     */
    public void enableCASServiceDefinition() throws IOException {
        final Path pathToCASProtocolXML = Paths.get("conf", "cas-protocol.xml");

        final String regex = "</list>";

        final String serviceDefintiion = 
                  "<bean class=\"net.shibboleth.idp.cas.service.ServiceDefinition\"\n"
                + "c:regex=\"https?://localhost(:\\\\d+)?/.*\"\n"
                + "p:group=\"test-services\"\n"
                + "p:authorizedToProxy=\"false\" />\n";

        final StringBuilder replacement = new StringBuilder();

        // localhost
        replacement.append(serviceDefintiion);

        // Add public server addresses if not localhost
        final Set<String> addresses = new LinkedHashSet();
        addresses.add(address);
        addresses.add(secureAddress);
        addresses.add(privateAddress);
        addresses.add(privateSecureAddress);
        for (final String addr : addresses) {
            if (addr == "localhost") {
                continue;
            }
            replacement.append(serviceDefintiion.replaceFirst("localhost", addr));
        }

        replacement.append(regex);

        replaceIdPHomeFile(pathToCASProtocolXML, "[ \t]*" + regex, replacement.toString());
    }

    /**
     * Release eduPersonAffiliation by adding a wildcard regex to attribute-filter.xml.
     * 
     * @throws IOException ...
     */
    public void enableCASAttributes() throws IOException {
        final Path pathToAttributeFilterXML = Paths.get("conf", "attribute-filter.xml");

        final String oldText = "<Rule xsi:type=\"Requester\" value=\"https://another.example.org/shibboleth\" />";
        
        final String requesterRegex = "<Rule xsi:type=\"RequesterRegex\" regex=\"https?://localhost(:\\\\d+)?/.*\" />\n";

        final StringBuilder replacement = new StringBuilder();

        // localhost
        replacement.append(requesterRegex);

        // Add public server addresses if not localhost
        final Set<String> addresses = new LinkedHashSet();
        addresses.add(address);
        addresses.add(secureAddress);
        addresses.add(privateAddress);
        addresses.add(privateSecureAddress);
        for (final String addr : addresses) {
            if (addr == "localhost") {
                continue;
            }
            replacement.append(requesterRegex.replaceFirst("localhost", addr));
        }

        replaceIdPHomeFile(pathToAttributeFilterXML, oldText, replacement.toString());
    }

    public void enableAttributeReleaseConsent() throws IOException {
        final Path pathToRelyingPartyXML = Paths.get("conf", "relying-party.xml");
        String oldText;
        if (idpVersion.startsWith("4.1") || idpVersion.startsWith("4.2")) {
            oldText = "<bean parent=\"SAML2.SSO\" />";
        } else {
            oldText = "<ref bean=\"SAML2.SSO\" />";
        }
        final String newText = "<bean parent=\"SAML2.SSO\" p:postAuthenticationFlows=\"attribute-release\" />";
        replaceIdPHomeFile(pathToRelyingPartyXML, oldText, newText);
    }

    /**
     * Use attribute-resolver-ldap.xml instead of attribute-resolver.xml.
     * 
     * @throws IOException ...
     */
    public void enableAttributeResolverLDAP() throws IOException {
        final Path pathToServicesXML = Paths.get("conf", "services.xml");
        final String oldText = "<value>%\\{idp.home\\}/conf/attribute-resolver.xml</value>";
        final String newText = "<value>%\\{idp.home\\}/conf/examples/attribute-resolver-ldap.xml</value>";
        replaceIdPHomeFile(pathToServicesXML, oldText, newText);
    }

    /**
     * Export uid via LDAP Connector.
     * 
     * @throws IOException ...
     */
    public void enableAttributeResolverLDAPExportUid() throws IOException {
        final Path pathToAttributeResolverLdapXML = Paths.get("conf", "examples", "attribute-resolver-ldap.xml");
        
        final String oldText = "mail displayName sn givenName departmentNumber employeeNumber eduPersonEntitlement eduPersonAssurance";
        final String newText = "mail displayName sn givenName departmentNumber employeeNumber eduPersonEntitlement eduPersonAssurance uid";
        replaceIdPHomeFile(pathToAttributeResolverLdapXML, oldText, newText);
    }

    /**
     * Log unencrypted SAML.
     * 
     * @throws IOException ...
     */
    public void logUnencryptedSAML() throws IOException {
        final Path pathToLogbackXML = Paths.get("conf", "logback.xml");
        final String oldMessagesText = "<variable name=\"idp.loglevel.messages\" value=\"INFO\" />";
        final String newMessagesText = "<variable name=\"idp.loglevel.messages\" value=\"DEBUG\" />";
        replaceIdPHomeFile(pathToLogbackXML, oldMessagesText, newMessagesText);
        
        final String oldEncryptionText = "<variable name=\"idp.loglevel.encryption\" value=\"INFO\" />";
        final String newEncryptionText = "<variable name=\"idp.loglevel.encryption\" value=\"DEBUG\" />";
        replaceIdPHomeFile(pathToLogbackXML, oldEncryptionText, newEncryptionText);
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
     * Set up Firefox web driver.
     * 
     * @throws IOException ...
     */
    @BeforeMethod(enabled = false)
    public void setUpFirefoxDriver() throws IOException {
        final FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(true);
        options.setHeadless(true);
        if (Boolean.getBoolean("no-headless")) {
            options.setHeadless(false);
        }
        options.addPreference("devtools.jsonview.enabled", false);
        driver = new FirefoxDriver(options);
        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(1024, 768));
    }

    /**
     * Set up Safari web driver.
     * 
     * Note that Safari does not support running headless.
     */
    public void setUpSafariDriver() {
        driver = new SafariDriver();
    }

    /**
     * Set up remote web driver to Sauce Labs.
     * 
     * <p>
     * Prefers credentials from system properties/environment variables as provided by Jenkins over ~/.sauce-ondemand,
     * see {@link SauceOnDemandAuthentication}.
     * </p>
     * 
     * @throws IOException ...
     */
    @BeforeMethod(enabled = false, dependsOnMethods = {"setUpTestName"})
    public void setUpSauceDriver() throws IOException {
        final SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();
        final String username = authentication.getUsername();
        final String accesskey = authentication.getAccessKey();
        final URL url = new URL("https://" + username + ":" + accesskey + "@ondemand.us-west-1.saucelabs.com:443/wd/hub");
        final RemoteWebDriver remoteWebDriver = new RemoteWebDriver(url, desiredCapabilities);
        threadLocalWebDriver.set(remoteWebDriver);
        driver = threadLocalWebDriver.get();
        threadLocalSessionId.set(remoteWebDriver.getSessionId().toString());
    }

    /**
     * Set up the desired capabilities.
     * 
     * Prefer capabilities as provided by the data provider {@link sauceOnDemandBrowserDataProvider}.
     * 
     * Sets the test name to be displayed by Sauce Labs at
     * <a href="https://saucelabs.com/tests">https://saucelabs.com/tests</a>.
     * 
     * The desired capabilities will be overridden by the {@link #overrideCapabilities} if non-null.
     * 
     * Disables JSON view when using Firefox.
     * 
     * Accepts insecure certs when using browsers other than Safari.
     * 
     * Use non-secure port when using Safari.
     * 
     * @param browserData the browser data
     */
    public void setUpDesiredCapabilities(@Nullable final BrowserData browserData) {

        // name of test displayed on Sauce Labs
        desiredCapabilities.setCapability("name", testName);

        if (browserData != null) {
            // browser name
            if (browserData.getBrowser() != null) {
                desiredCapabilities.setCapability("browserName", browserData.getBrowser());
            }
            // browser version
            if (browserData.getVersion() != null) {
                desiredCapabilities.setCapability("version", browserData.getVersion());
            }
            // browser OS
            if (browserData.getOS() != null) {
                desiredCapabilities.setCapability("platform", browserData.getOS());
            }
            // browser device
            if (browserData.getDevice() != null) {
                desiredCapabilities.setCapability("deviceName", browserData.getDevice());
            }
        }

        if (desiredCapabilities.getBrowserName().equalsIgnoreCase("firefox")) {
            log.debug("Disabling Firefox JSON view");
            final FirefoxOptions options = new FirefoxOptions();
            options.addPreference("devtools.jsonview.enabled", false);
            desiredCapabilities.merge(options);
        }

        // Override desired capabilities.
        if (overrideCapabilities != null) {
            log.debug("Override desired capabilities with '{}'", overrideCapabilities);
            desiredCapabilities.merge(overrideCapabilities);
        }

        if (browserData != null && (browserData.getBrowser().equalsIgnoreCase("safari")
                || browserData.getBrowser().equalsIgnoreCase("ipad")
                || browserData.getBrowser().equalsIgnoreCase("iphone"))) {
            log.warn("Safari does not support accepting insecure certs");
            try {
                setUpNonSecurePort();
            } catch (IOException e) {
                log.error("Unable to set up non-secure port {}", e);
                throw new RuntimeException(e);
            }
        } else {
            desiredCapabilities.setAcceptInsecureCerts(true);
        }

        log.debug("Desired capabilities '{}'", desiredCapabilities);
    }

    /**
     * A TestNG {@link DataProvider} which provides platform/browser/version triplets in the form of {@link BrowserData}
     * to test methods.
     * 
     * Prefers system properties over environment variables.
     * 
     * Prefers {@link SauceBrowserDataProvider#SAUCE_ONDEMAND_BROWSERS} over SELENIUM_PLATFORM, SELENIUM_BROWSER, and
     * SELENIUM_VERSION. Defaults to Firefox.
     * 
     * Rewrites 'Mac 11' platform as 'macOS 11' as a workaround when using Sauce Labs.
     * 
     * Populates browser data with SELENIUM_DEVICE if it exists, used to select iOS devices.
     * 
     * Wraps {@link SauceBrowserDataProvider#sauceBrowserDataProvider(Method)} to avoid the IllegalArgumentException
     * when the environment does not contain the desired property/variable.
     * 
     * @param testMethod the test method
     * @return data provider which supplies {@link BrowserData} to test methods
     */
    @DataProvider(name = "sauceOnDemandBrowserDataProvider", parallel = false)
    public static Iterator<Object[]> sauceOnDemandBrowserDataProvider(@Nonnull final Method testMethod) {

        final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

        final List<Object[]> data = new ArrayList<Object[]>();

        try {
            // multiple platform/browser/version triplets
            log.debug("Attempting to find '{}' in system properties or environment",
                    SauceBrowserDataProvider.SAUCE_ONDEMAND_BROWSERS);
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
            // single platform/browser/version triplet
            log.debug("Did not find '{}' in system properties or environment",
                    SauceBrowserDataProvider.SAUCE_ONDEMAND_BROWSERS);

            // prefer system properties to environment variable
            log.trace("System property SELENIUM_PLATFORM      '{}'", System.getProperty("SELENIUM_PLATFORM"));
            log.trace("System property SELENIUM_BROWSER       '{}'", System.getProperty("SELENIUM_BROWSER"));
            log.trace("System property SELENIUM_VERSION       '{}'", System.getProperty("SELENIUM_VERSION"));
            log.trace("System property SELENIUM_DEVICE        '{}'", System.getProperty("SELENIUM_DEVICE"));
            log.trace("Environment variable SELENIUM_PLATFORM '{}'", System.getenv("SELENIUM_PLATFORM"));
            log.trace("Environment variable SELENIUM_BROWSER  '{}'", System.getenv("SELENIUM_BROWSER"));
            log.trace("Environment variable SELENIUM_VERSION  '{}'", System.getenv("SELENIUM_VERSION"));
            log.trace("Environment variable SELENIUM_DEVICE   '{}'", System.getenv("SELENIUM_DEVICE"));

            final String platform = System.getProperty("SELENIUM_PLATFORM", System.getenv("SELENIUM_PLATFORM"));
            log.debug("Found SELENIUM_PLATFORM '{}'", platform);

            final String browser = System.getProperty("SELENIUM_BROWSER", System.getenv("SELENIUM_BROWSER"));
            log.debug("Found SELENIUM_BROWSER  '{}'", browser);

            final String version = System.getProperty("SELENIUM_VERSION", System.getenv("SELENIUM_VERSION"));
            log.debug("Found SELENIUM_VERSION  '{}'", version);

            final String device = System.getProperty("SELENIUM_DEVICE", System.getenv("SELENIUM_DEVICE"));
            log.debug("Found SELENIUM_DEVICE   '{}'", device);

            final BrowserData browserData = new BrowserData();

            if (platform != null) {
                // hack macOS
                if (platform.equalsIgnoreCase("Mac 11")) {
                    log.debug("Rewriting platform 'Mac 11' as 'macOS 11'");
                    browserData.setOS("macOS 11");
                } else {
                    browserData.setOS(platform);
                }
            }

            if (browser != null) {
                browserData.setBrowser(browser);
            } else {
                log.debug("No SELENIUM_BROWSER found, defaulting to Firefox");
                browserData.setBrowser("firefox");
            }

            if (version != null) {
                browserData.setVersion(version);
            }

            if (device != null) {
                browserData.setDevice(device);
            }

            data.add(new Object[] {browserData});
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
     * Whether the browser is Safari on macOS or iOS.
     * 
     * @return whether the browser is Safari.
     */
    public boolean isSafari(@Nullable final BrowserData browserData) {
        if (browserData != null) {
            if (browserData.getBrowser().equalsIgnoreCase("safari")) {
                return true;
            }
            if (browserData.getBrowser().equalsIgnoreCase("iphone")) {
                return true;
            }
            if (browserData.getBrowser().equalsIgnoreCase("ipad")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set up the client IP range used in conf/access-control.xml to Sauce Labs {@link #SAUCE_LABS_IP_RANGE} if Selenium
     * is not local.
     */
    @BeforeClass(enabled = true)
    public void setUpSauceLabsClientIPRange() {
        if (BaseIntegrationTest.isRemote()) {
            clientIPRanges.addAll(SAUCE_LABS_IP_RANGES);
            log.debug("Setting client IP range to '{}'", clientIPRanges);
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
                    pathToPerTestDirectory.toAbsolutePath());
        } else if (!Boolean.getBoolean("keepTests")) {
            log.debug("Deleting per-test idp.home directory '{}'", pathToPerTestDirectory.toAbsolutePath());
            FileSystemUtils.deleteRecursively(pathToPerTestDirectory.toAbsolutePath().toFile());
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

        pageSource = driver.findElement(By.tagName("body")).getText();

        if (isInternetExplorer()) {
            pageSource = cleanupPageSourceIE(pageSource);
        }
        log.trace("get page source\n{}", pageSource);
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
        login("jdoe");
    }
    

    /**
     * <ul>
     * <li>Input username</li>
     * <li>Input password</li>
     * <li>Submit form.</li>
     * </ul>
     * 
     * @param user username
     */
    public void login(final @Nonnull String user) {
        new WebDriverWait(driver, 10).until(driver -> driver.findElement(By.name("j_username")));
        final WebElement username = driver.findElement(By.name("j_username"));
        final WebElement password = driver.findElement(By.name("j_password"));
        username.sendKeys(user);
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
            clickWorkaround(element);
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
     * Wait for the page whose URL contains the given fraction.
     * 
     * @param fraction the fraction of the url
     */
    public void waitForPageURLContains(@Nonnull final String fraction) {
        Assert.assertNotNull(fraction);
        new WebDriverWait(driver, 10).until(ExpectedConditions.urlContains(fraction));
    }

    /**
     * Wait for page whose body has text that starts with the given text.
     * 
     * Attempts to workaround {@link StaleElementReferenceException} by retrieving the body text a second time.
     * 
     * @param prefix the prefix of the page body text
     */
    public void waitForPageBodyStartsWith(@Nonnull final String prefix) {
        Assert.assertNotNull(prefix);
        try {
            (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver d) {
                    return d.findElement(By.tagName("body")).getText().startsWith(prefix);
                }
            });
        } catch (final StaleElementReferenceException e) {
            log.debug("Caught StaleElementReferenceException, will try again...", e);
            (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver d) {
                    return d.findElement(By.tagName("body")).getText().startsWith(prefix);
                }
            });
        }
    }

    /**
     * Wait for page whose body contains the given text.
     * 
     * @param fraction the fraction of the page body text
     */
    public void waitForPageBodyContains(@Nonnull final String fraction) {
        Assert.assertNotNull(fraction);
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.findElement(By.tagName("body")).getText().contains(fraction);
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
            clickWorkaround(email);
        }

        final WebElement eduPersonAffiliation = driver.findElement(By.id(EDU_PERSON_AFFILIATION_ID));
        if (eduPersonAffiliation.isSelected()) {
            clickWorkaround(eduPersonAffiliation);
        }

        final WebElement eduPersonPrincipalName = driver.findElement(By.id(EDU_PERSON_PRINCIPAL_NAME_ID));
        if (eduPersonPrincipalName.isSelected()) {
            clickWorkaround(eduPersonPrincipalName);
        }

        final WebElement uid = driver.findElement(By.id(UID_ID));
        if (uid.isSelected()) {
            clickWorkaround(uid);
        }
    }

    /**
     * Select web element with id {@link #REMEMBER_CONSENT_ID}.
     */
    public void rememberConsent() {
        final WebElement element = driver.findElement(By.id(REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            clickWorkaround(element);
        }
    }

    /**
     * Select web element with id {@link #DO_NOT_REMEMBER_CONSENT_ID}.
     */
    public void doNotRememberConsent() {
        final WebElement element = driver.findElement(By.id(DO_NOT_REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            clickWorkaround(element);
        }
    }

    /**
     * Select web element with id {@link #GLOBAL_CONSENT_ID}.
     */
    public void globalConsent() {
        final WebElement element = driver.findElement(By.id(GLOBAL_CONSENT_ID));
        if (!element.isSelected()) {
            clickWorkaround(element);
        }
    }

    /**
     * Submit form by clicking element with name {@link #SUBMIT_FORM_INPUT_NAME}.
     */
    public void submitForm() {
        driver.findElement(By.name(SUBMIT_FORM_INPUT_NAME)).click();
    }

    /**
     * Get EC2 metadata.
     * 
     * @return EC2 metadata or null
     * @throws Exception if unable to retrieve metadata from EC2
     */
    @Nullable
    public String getEC2Metadata(@Nonnull final String url) throws Exception {
        log.debug("Get EC2 metadata '{}'", url);
        final HttpGet httpget = new HttpGet(url);
        final HttpClientBuilder builder = new HttpClientBuilder();
        try {
            final HttpClient httpClient = builder.buildClient();
            final HttpResponse response = httpClient.execute(httpget);
            log.trace("EC2 metadata response '{}'", response);
            try {
                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    long len = entity.getContentLength();
                    if (len != -1) {
                        final String metadata = EntityUtils.toString(entity);
                        log.info("EC2 '{}'='{}'", url, metadata);
                        return metadata;
                    }
                }
            } finally {
                if (response instanceof CloseableHttpResponse) {
                    ((CloseableHttpResponse) response).close();
                }
                if (httpClient instanceof CloseableHttpClient) {
                    ((CloseableHttpClient) httpClient).close();
                }
            }
        } catch (Exception e) {
            log.error("Status page response error '{}'", e);
            throw e;
        }
        return null;
    }

    /**
     * Get EC2 private IPV4 address.
     * 
     * @return EC2 private IPV4 address or null
     * @throws Exception if unable to determine private IPV4 address
     */
    @Nullable
    public String getEC2PrivateIPV4() throws Exception {
        log.info("Attempting to get private IPV4 address from EC2");
        final String url = "http://169.254.169.254/latest/meta-data/local-ipv4";
        return getEC2Metadata(url);
    }

    /**
     * Get EC2 public IPV4 address.
     * 
     * @return EC2 public IPV4 address or null
     * @throws Exception if unable to determine public IPV4 address
     */
    @Nullable
    public String getEC2PublicIPV4() throws Exception {
        log.info("Attempting to get public IPV4 address from EC2");
        final String url = "http://169.254.169.254/latest/meta-data/public-ipv4";
        return getEC2Metadata(url);
    }

    /**
     * Workaround being unable to click an element because of accessibility changes.
     * 
     * Catch {@link ElementClickInterceptedException} and click via {@link Actions}.
     * 
     * @param element element to be clicked
     */
    public void clickWorkaround(final WebElement element) {
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.trace("Unable to click element {}, will try again", element);
            final Actions action = new Actions(driver);
            action.moveToElement(element).click().build().perform();
        }
    }

    /**
     * Log output of process.
     * 
     * @param process the process to be logged
     * @param prefix the prefix of the message to be logged
     * @throws IOException
     */
    public void logProcess(@Nonnull final Process process, @Nullable final String prefix) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            log.debug("{} {}", prefix, line);
        }
    }

    /**
     * Copy IdP distribution directory to IdP home.
     *
     * @param directory files to be copied from IdP distribution to IdP home
     * @throws IOException if an error occurs
     */
    public void copyFromIdPDistToIdPHome(@Nonnull final String directory) throws IOException {
        final File idpDistDirectory = pathToIdPDist.resolve(directory).toAbsolutePath().toFile();
        final File idpHomeDirectory = pathToIdPHome.resolve(directory).toAbsolutePath().toFile();
        if (!idpHomeDirectory.exists()) {
            Files.createDirectory(idpHomeDirectory.toPath());
        }
        FileSystemUtils.copyRecursively(idpDistDirectory, idpHomeDirectory);
    }

    /**
     * Return true if os.name system property starts with "windows", ignoring case.
     * 
     * @return true if os.name system property starts with "windows", ignoring case
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * Disable OIDC dynamic response header filter because the testbed is not properly configured as an OP.
     */
    @BeforeClass
    public void disableRegisterFilterServletContextInitializer() {
        serverCommands
                .add("-Dnet.shibboleth.idp.plugin.oidc.op.servlet.RegisterFilterServletContextInitializer=disabled");
    }

}
