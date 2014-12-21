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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.Nonnull;

import net.shibboleth.idp.installer.PropertiesWithComments;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Abstract integration test which starts a Jetty server and an in-memory directory server.
 */
public abstract class BaseIntegrationTest {

    /** Directory in which distributions will be unpackaged. */
    @Nonnull public final static String TEST_DISTRIBUTIONS_DIRECTORY = "test-distributions";

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
    @NonnullAfterInit protected String idpXMLSecurityManager;

    /** Jetty server process. */
    @NonnullAfterInit protected JettyServerProcess server;

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

    /** Parser pool */
    @NonnullAfterInit protected ParserPool parserPool;

    /** XMLObject unmarshaller factory */
    @NonnullAfterInit protected UnmarshallerFactory unmarshallerFactory;

    /** Web driver. */
    @Nonnull protected WebDriver driver;

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
     */
    @BeforeClass public void setupPaths() {

        // Path to the project build directory.
        final Path buildPath = Paths.get(TEST_DISTRIBUTIONS_DIRECTORY);
        log.debug("Path to build directory '{}'", buildPath.toAbsolutePath());
        Assert.assertTrue(buildPath.toAbsolutePath().toFile().exists(), "Path to build directory '{}' not found");

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
        Assert.assertTrue(pathToJettyHome.toAbsolutePath().toFile().exists(), "Path to jetty.home '{}' not found");

        // Path to idp.home
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildPath, "*shibboleth-identity-provider-*")) {
            for (Path entry : stream) {
                pathToIdPHome = entry;
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Path to idp.home '{}'", pathToIdPHome.toAbsolutePath());
        Assert.assertNotNull(pathToIdPHome, "Path to idp.home not found");
        Assert.assertTrue(pathToIdPHome.toAbsolutePath().toFile().exists(), "Path to idp.home '{}' not found");

        // Set idp.home system property
        System.setProperty("idp.home", pathToIdPHome.toAbsolutePath().toString());

        // Path to jetty.base
        pathToJettyBase = pathToIdPHome.resolve(Paths.get("jetty-base"));
        log.debug("Path to jetty.base '{}'", pathToJettyBase.toAbsolutePath());
        Assert.assertNotNull(pathToJettyBase, "Path to jetty.base not found");
        Assert.assertTrue(pathToJettyBase.toAbsolutePath().toFile().exists(), "Path to jetty.base '{}' not found");

        // Path to conf/idp.properties
        pathToIdPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "idp.properties");
        Assert.assertTrue(pathToIdPProperties.toFile().exists(), "Path to conf/idp.properties not found");

        // Path to conf/ldap.properties
        pathToLDAPProperties = Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "ldap.properties");
        Assert.assertTrue(pathToLDAPProperties.toFile().exists(), "Path to conf/ldap.properties not found");
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
     * Do not use STARTTLS for LDAP connection to test in-memory directory server.
     *
     * @throws Exception
     */
    @BeforeClass(dependsOnMethods = {"setupPaths"}) public void disableLDAPSTARTTLS() throws Exception {
        replaceLDAPProperty("idp.authn.LDAP.useStartTLS", "false");
    }

    /**
     * Restore conf/ldap.properties from dist/conf/ldap.properties.dist.
     * 
     * @throws IOException if an I/O error occurs
     */
    @AfterClass(alwaysRun = true) public void restoreLDAPProperties() throws IOException {
        final Path pathToLDAPPropertiesDist =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "dist", "conf", "ldap.properties.dist");
        Assert.assertTrue(pathToLDAPPropertiesDist.toFile().exists());

        Files.copy(pathToLDAPPropertiesDist, pathToLDAPProperties, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Start the Jetty server.
     * 
     * @throws ComponentInitializationException if the server cannot be initialized
     */
    @BeforeMethod public void startJettyServer() throws ComponentInitializationException {
        server = new JettyServerProcess(pathToJettyBase, pathToJettyHome);
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
     * Setup HTML web driver.
     */
    @BeforeMethod(enabled = true, dependsOnMethods = {"startJettyServer"}) protected void setUpDriver()
            throws IOException {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
    }

    /**
     * Setup Firefox web driver.
     */
    @BeforeMethod(enabled = false, dependsOnMethods = {"startJettyServer"}) protected void setUpFirefoxDriver()
            throws IOException {
        final ProfilesIni allProfiles = new ProfilesIni();
        final FirefoxProfile profile = allProfiles.getProfile("FirefoxShibtest");
        driver = new FirefoxDriver(profile);
    }

    /**
     * <ul>
     * <li>Input username</li>
     * <li>Input password</li>
     * <li>Submit form.</li>
     * </ul>
     */
    protected void login() {
        final WebElement username = driver.findElement(By.name("j_username"));
        final WebElement password = driver.findElement(By.name("j_password"));
        username.sendKeys("jdoe");
        password.sendKeys("changeit");
        submitForm();
    }

    /**
     * Wait for page with title {@link #TERMS_OF_USE_PAGE_TITLE}.
     */
    protected void waitForTermsOfUsePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(TERMS_OF_USE_PAGE_TITLE);
            }
        });
    }

    /**
     * Accept terms of use by clicking the {@link #CONSENT_IDS_INPUT_NAME} checkbox.
     */
    protected void acceptTermsOfUse() {
        final WebElement element = driver.findElement(By.name(CONSENT_IDS_INPUT_NAME));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Wait for page with title {@link #ATTRIBUTE_RELEASE_PAGE_TITLE}.
     */
    protected void waitForAttributeReleasePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(ATTRIBUTE_RELEASE_PAGE_TITLE);
            }
        });
    }

    /**
     * Select web element with id {@link #REMEMBER_CONSENT_ID}.
     */
    protected void releaseAllAttributes() {
        final WebElement element = driver.findElement(By.id(REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select release of email attribute only by selecting web element with id {@link #EMAIL_ID} and not selecting web
     * element with id {@link #EDU_PERSON_AFFILIATION_ID}.
     */
    protected void releaseEmailAttributeOnly() {
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
    protected void rememberConsent() {
        final WebElement element = driver.findElement(By.id(REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select web element with id {@link #DO_NOT_REMEMBER_CONSENT_ID}.
     */
    protected void doNotRememberConsent() {
        final WebElement element = driver.findElement(By.id(DO_NOT_REMEMBER_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Select web element with id {@link #GLOBAL_CONSENT_ID}.
     */
    protected void globalConsent() {
        final WebElement element = driver.findElement(By.id(GLOBAL_CONSENT_ID));
        if (!element.isSelected()) {
            element.click();
        }
    }

    /**
     * Submit form by clicking element with name {@link #SUBMIT_FORM_INPUT_NAME}.
     */
    protected void submitForm() {
        driver.findElement(By.name(SUBMIT_FORM_INPUT_NAME)).click();
    }

}
