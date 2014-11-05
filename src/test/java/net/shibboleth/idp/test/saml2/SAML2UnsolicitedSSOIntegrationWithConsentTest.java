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

package net.shibboleth.idp.test.saml2;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.shibboleth.idp.test.flows.saml2.SAML2TestResponseValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * SAML 2 unsolicited SSO test with consent flows.
 */
public class SAML2UnsolicitedSSOIntegrationWithConsentTest extends AbstractSAML2IntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2UnsolicitedSSOIntegrationWithConsentTest.class);

    /** Property value of consent flows to enable. */
    public final static String ENABLE_CONSENT_FLOW_PROPERTY_VALUE = "terms-of-use|attribute-release";

    /** Title of terms of use page. */
    public final static String TERMS_OF_USE_PAGE_TITLE = "Terms of Use";

    /** Title of attribute release page. */
    public final static String ATTRIBUTE_RELEASE_PAGE_TITLE = "Attribute Release";

    /** ID of email attribute checkbox. */
    public final static String EMAIL_ID = "email";

    /** ID of eduPersonAffiliation attribute checkbox. */
    public final static String EDU_PERSON_AFFILIATION_ID = "eduPersonAffiliation";

    /** ID of radio button to not remember consent. */
    public final static String DO_NOT_REMEMBER_CONSENT_ID = "_idp_doNotRememberConsent";

    /** ID of radio button to remember consent. */
    public final static String REMEMBER_CONSENT_ID = "_idp_rememberConsent";

    /** ID of radio button for global consent. */
    public final static String GLOBAL_CONSENT_ID = "_idp_globalConsent";

    /** Name of form input element containing consent IDs. */
    public final static String CONSENT_IDS_INPUT_NAME = "consentIds";

    /** Name of form input element to submit form. */
    public final static String SUBMIT_FORM_INPUT_NAME = "_eventId_proceed";

    /** IdP endpoint. */
    @Nonnull public final String idpEndpoint = "https://localhost:8443/idp/profile/SAML2/Unsolicited/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shire = "https://localhost:8443/sp/SAML2/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    /** URL. */
    @Nonnull public final String url = idpEndpoint + "?providerId=" + providerID + "&shire=" + shire + "&target="
            + target;

    /** Response validator. */
    @Nonnull protected SAML2TestResponseValidator validator;

    /** Web driver. */
    @Nonnull protected WebDriver driver;

    /**
     * Restore idp.properties from original source.
     * 
     * @throws IOException if an I/O error occurs
     */
    @AfterClass(alwaysRun = true) protected void restoreConfiguration() throws IOException {
        restoreIdPProperties();
    }

    /**
     * Setup response validator.
     * 
     * @throws IOException if an I/O error occurs
     */
    @BeforeMethod protected void setUpValidator() throws IOException {
        validator = new SAML2TestResponseValidator();
        validator.spCredential = getSPCredential();
        validator.authnContextClassRef = AuthnContext.PASSWORD_AUTHN_CTX;
    }

    /**
     * Setup HTML web driver.
     */
    @BeforeMethod(enabled = true) protected void setUpDriver() throws IOException {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
    }

    /**
     * Setup Firefox web driver.
     */
    @BeforeMethod(enabled = false) protected void setUpFirefoxDriver() throws IOException {
        final ProfilesIni allProfiles = new ProfilesIni();
        final FirefoxProfile profile = allProfiles.getProfile("FirefoxShibtest");
        driver = new FirefoxDriver(profile);
    }

    /**
     * <ul>
     * <li>Wait for login page.</li>
     * <li>Input username</li>
     * <li>Input password</li>
     * <li>Submit form.</li>
     * </ul>
     */
    protected void login() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(idpEndpoint);
            }
        });

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

    /**
     * Wait for page containing SAML response.
     */
    protected void waitForResponsePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().equals(shire);
            }
        });
    }

    /**
     * Validate SAML response
     * 
     * @throws Exception
     */
    protected void validateResponse() throws Exception {
        validator.validateResponse(unmarshallResponse(driver.getPageSource()));
    }

    /**
     * Test releasing all attributes.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseAllAttributes() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test releasing a single attribute.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseOneAttribute() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseEmailAttributeOnly();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validator.expectedAttributes.clear();
        validator.expectedAttributes.add(validator.mailAttribute);

        validateResponse();
    }

    /**
     * Test releasing a single attribute and remember consent.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseOneAttributeRememberConsent() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseEmailAttributeOnly();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validator.expectedAttributes.clear();
        validator.expectedAttributes.add(validator.mailAttribute);

        validateResponse();

        // twice

        driver.get(url);

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test releasing all attributes and remembering consent.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseAllAttributesRememberConsent() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        driver.get(url);

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test releasing all attributes and not remembering consent.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseAllAttributesDoNotRememberConsent() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        doNotRememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        driver.get(url);

        // attribute release again

        waitForAttributeReleasePage();

        releaseAllAttributes();

        doNotRememberConsent();

        submitForm();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test global consent.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOGlobalConsent() throws Exception {

        driver.get(url);

        login();

        // terms of use

        waitForTermsOfUsePage();

        acceptTermsOfUse();

        submitForm();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        globalConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        driver.get(url);

        // attribute release again

        waitForResponsePage();

        validateResponse();
    }
}
