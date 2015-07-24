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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.idp.test.flows.saml2.SAML2TestResponseValidator;
import net.shibboleth.idp.test.flows.saml2.SAML2TestStatusResponseTypeValidator;
import net.shibboleth.utilities.java.support.net.IPRange;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

import org.cryptacular.util.CertUtil;
import org.cryptacular.util.KeyPairUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract SAML 2 integration test.
 */
public abstract class AbstractSAML2IntegrationTest extends BaseIntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAML2IntegrationTest.class);

    /** Response validator. */
    @Nonnull protected SAML2TestResponseValidator validator;

    /** SP private key resource location. */
    @Nonnull public final String SP_KEY = "/credentials/sp.key";

    /** SP certificate resource location. */
    @Nonnull public final String SP_CRT = "/credentials/sp.crt";

    /** ID of logout iframe element. */
    @Nonnull public String logoutIFrameID = "buffer";

    /** ID of transient ID input element to init logout. */
    @Nonnull public String logoutTransientIDInputID;

    /**
     * Setup response validator.
     * 
     * @throws IOException if an I/O error occurs
     */
    @BeforeMethod
    public void setUpValidator() throws IOException {
        validator = new SAML2TestResponseValidator();
        validator.spCredential = getSPCredential();
        validator.authnContextClassRef = AuthnContext.PPT_AUTHN_CTX;
        if (BaseIntegrationTest.isRemote()) {
            validator.subjectConfirmationDataAddressRange = IPRange.parseCIDRBlock(SAUCE_LABS_IP_RANGE);
        }
    }

    /**
     * Validate SAML 2 {@link Response}.
     * 
     * @throws Exception
     */
    public void validateResponse() throws Exception {
        validator.validateResponse(unmarshallResponse(getPageSource()));
    }

    /**
     * Validate SAML 2 {@link LogoutResponse}.
     * 
     * @throws Exception
     */
    public void validateLogoutResponse() throws Exception {

        driver.switchTo().frame(logoutIFrameID);

        final XMLObject xmlObject = unmarshallXMLObject(getPageSource());

        Assert.assertTrue(xmlObject instanceof LogoutResponse);

        final SAML2TestStatusResponseTypeValidator validator = new SAML2TestStatusResponseTypeValidator();
        validator.destination = getBaseURL() + spLogoutURLPath;
        validator.validateResponse((LogoutResponse) xmlObject);
    }

    /**
     * Unmarshall the XML response into an {@link #XMLObject} object.
     * 
     * @param response
     * @return
     * @throws UnsupportedEncodingException
     * @throws XMLParserException
     * @throws UnmarshallingException
     */
    @Nonnull
    public XMLObject unmarshallXMLObject(@Nullable final String response)
            throws UnsupportedEncodingException, XMLParserException, UnmarshallingException {
        Assert.assertNotNull(response);
        final Document doc = parserPool.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
        final Element element = doc.getDocumentElement();
        Assert.assertNotNull(element);
        final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        Assert.assertNotNull(unmarshaller);
        return unmarshaller.unmarshall(element);
    }

    /**
     * Unmarshall the XML response into a SAML 2 {@link #Response} object.
     * 
     * @param response the XML response
     * @return the SAML 2 Response object
     * @throws UnsupportedEncodingException if an error occurs
     * @throws XMLParserException if an error occurs
     * @throws UnmarshallingException if an error occurs
     */
    @Nonnull
    public Response unmarshallResponse(@Nullable final String response)
            throws UnsupportedEncodingException, XMLParserException, UnmarshallingException {
        final XMLObject xmlObject = unmarshallXMLObject(response);
        Assert.assertTrue(xmlObject instanceof Response);
        return (Response) xmlObject;
    }

    /**
     * Get the SP credential.
     * 
     * @return the SP credential.
     * @throws IOException if an error occurs reading the credential
     */
    @Nonnull
    public Credential getSPCredential() throws IOException {

        final ClassPathResource spKeyResource = new ClassPathResource(SP_KEY);
        Assert.assertTrue(spKeyResource.exists());
        final PrivateKey spPrivateKey = KeyPairUtil.readPrivateKey(spKeyResource.getInputStream());

        final ClassPathResource spCrtResource = new ClassPathResource(SP_CRT);
        Assert.assertTrue(spCrtResource.exists());
        final X509Certificate spEntityCert = (X509Certificate) CertUtil.readCertificate(spCrtResource.getInputStream());

        return new BasicX509Credential(spEntityCert, spPrivateKey);
    }

    /**
     * Get the value of the principal's name identifier.
     * 
     * This method unmarshalls the page source into a {@link Response} and assumes that {@link #validateResponse()} was
     * successful.
     * 
     * @return the value of the principal's name identifier
     * @throws Exception if an error occurs
     */
    @Nullable
    public String getNameIDValue() throws Exception {
        final Response response = unmarshallResponse(getPageSource());
        return response.getAssertions().get(0).getSubject().getNameID().getValue();
    }

    /**
     * Init logout at mock SP by submitting a nameID on the testbed page.
     * 
     * @param inputID ID of transient name identifier input element
     * @param nameID value of the transient name identifier
     */
    public void initLogout(@Nonnull final String inputID, @Nonnull final String nameID) {
        final WebElement input = driver.findElement(By.id(inputID));
        input.sendKeys(nameID);
        input.submit();
    }

    /**
     * Test SAML 2 SSO releasing all attributes.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOReleaseAllAttributes(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        startFlow();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SAML 2 SSO releasing a single attribute.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOReleaseOneAttribute(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enablePerAttributeConsent();

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

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

        startFlow();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SAML 2 SSO releasing all attributes and not remembering consent.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSODoNotRememberConsent(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        doNotRememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        startFlow();

        // attribute release again

        waitForAttributeReleasePage();

        releaseAllAttributes();

        doNotRememberConsent();

        submitForm();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SAML 2 SSO with global attribute consent.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOGlobalConsent(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        globalConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        startFlow();

        // attribute release again

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SAML 2 SSO terms of use flow.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOTermsOfUse(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableCustomRelyingPartyConfiguration();

        startJettyServer();

        startFlow();

        waitForLoginPage();

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

        startFlow();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SAML 2 SSO ForceAuthn.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOForceAuthn(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        driver.get(getBaseURL() + forceAuthnRequestURLPath);

        waitForLoginPage();

        login();

        waitForResponsePage();

        validateResponse();
    }

    /**
     * Test SSO isPasive without a session.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOPassiveWithoutSession(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        // start flow
        driver.get(getBaseURL() + isPassiveRequestURLPath);

        waitForResponsePage();

        final SAML2TestResponseValidator passiveValidator = new SAML2TestResponseValidator();
        passiveValidator.spCredential = getSPCredential();
        passiveValidator.statusCode = StatusCode.REQUESTER;
        passiveValidator.statusCodeNested = StatusCode.NO_PASSIVE;
        passiveValidator.statusMessage = "An error occurred.";

        passiveValidator.validateResponse(unmarshallResponse(getPageSource()));
    }

    /**
     * Test SSO isPassive with a pre-existing session.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSOPassiveWithSession(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // isPassive

        driver.get(getBaseURL() + isPassiveRequestURLPath);

        // response

        waitForResponsePage();

        validateResponse();
    }

    public void testSLO(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableLogout();

        startJettyServer();

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        startFlow();

        waitForResponsePage();

        validateResponse();

        // logout

        final String nameID = getNameIDValue();

        getAndWaitForTestbedPage();

        initLogout(logoutTransientIDInputID, nameID);

        waitForLogoutPage();

        validateLogoutResponse();
    }

}
