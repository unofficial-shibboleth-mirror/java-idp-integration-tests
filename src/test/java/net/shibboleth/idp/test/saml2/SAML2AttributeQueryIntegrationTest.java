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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.idp.test.flows.saml2.SAML2TestResponseValidator;
import net.shibboleth.idp.test.flows.saml2.SAML2TestStatusResponseTypeValidator;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.IPRange;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/** SAML 2 AttributeQuery test. */
public class SAML2AttributeQueryIntegrationTest extends AbstractSAML2IntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2AttributeQueryIntegrationTest.class);

    /** Validator to use for error responses. */
    @Nullable private SAML2TestStatusResponseTypeValidator errorValidator;

    /** SSO Response validator. */
    @Nonnull protected SAML2TestResponseValidator ssoValidator;

    /** Path to trusted SP certificate resource. */
    @NonnullAfterInit private String trustedSpCert;

    /** Path to trusted SP private key resource. */
    @NonnullAfterInit private String trustedSpKey;

    /** Path to untrusted SP certificate resource. */
    @NonnullAfterInit private String untrustedSpCert;

    /** Path to untrusted SP private key resource. */
    @NonnullAfterInit private String untrustedSpKey;

    /**
     * Setup response validators.
     * 
     * @throws IOException if an I/O error occurs
     */
    @BeforeMethod
    @Override
    public void setUpValidator() throws IOException {
        final SAMLObjectBuilder<NameID> builder = (SAMLObjectBuilder<NameID>) XMLObjectProviderRegistrySupport
                .getBuilderFactory().<NameID> getBuilderOrThrow(NameID.DEFAULT_ELEMENT_NAME);
        final NameID nameID = builder.buildObject();
        nameID.setValue("jdoe");
        nameID.setNameQualifier(null);
        nameID.setSPNameQualifier(null);
        nameID.setFormat(null);

        validator = new SAML2TestResponseValidator();
        validator.nameID = nameID;
        validator.subjectConfirmationMethod = SubjectConfirmation.METHOD_SENDER_VOUCHES;
        validator.validateAuthnStatements = false;
        validator.validateSubjectConfirmationData = false;

        errorValidator = new SAML2TestResponseValidator();
        errorValidator.statusCode = StatusCode.REQUESTER;

        ssoValidator = new SAML2TestResponseValidator();
        ssoValidator.spCredential = getSPCredential();
        ssoValidator.authnContextClassRef = AuthnContext.PPT_AUTHN_CTX;
        if (BaseIntegrationTest.isRemote()) {
            ssoValidator.subjectConfirmationDataAddressRange = IPRange.parseCIDRBlock(SAUCE_LABS_IP_RANGE);
        }
    }

    /**
     * Unmarshall the XML response into a SAML 2 {@link #Response} object.
     * 
     * This method differs from its parent class in that we are unmarshalling a SOAP response.
     * 
     * @param response the XML response
     * @return the SAML 2 Response object
     * @throws UnsupportedEncodingException if an error occurs
     * @throws XMLParserException if an error occurs
     * @throws UnmarshallingException if an error occurs
     */
    @Nonnull
    @Override
    public Response unmarshallResponse(@Nullable final String response)
            throws UnsupportedEncodingException, XMLParserException, UnmarshallingException {
        final XMLObject xmlObject = unmarshallXMLObject(response);
        Assert.assertTrue(xmlObject instanceof Envelope);
        final Envelope envelope = (Envelope) xmlObject;
        final Body body = envelope.getBody();
        Assert.assertEquals(body.getUnknownXMLObjects().size(), 1, "Expected a single SAML response");
        final XMLObject bodyXmlObject = body.getUnknownXMLObjects().get(0);
        Assert.assertTrue(bodyXmlObject instanceof Response);
        return (Response) bodyXmlObject;
    }

    /**
     * Set up paths to trusted and untrusted SP certificates and private keys from idp-conf.
     */
    @BeforeClass
    protected void setUpPaths() {
        final Path pathToTrustedSpCert = pathToIdPHome.resolve(Paths.get("credentials", "sp.crt"));
        Assert.assertTrue(pathToTrustedSpCert.toFile().exists(), "Path to sp.crt not found");
        trustedSpCert = "file:///" + pathToTrustedSpCert.toAbsolutePath().toString();
        log.debug("Path to trusted SP cert '{}'", trustedSpCert);

        final Path pathToTrustedSpKey = pathToIdPHome.resolve(Paths.get("credentials", "sp.key"));
        Assert.assertTrue(pathToTrustedSpKey.toFile().exists(), "Path to sp.key not found");
        trustedSpKey = "file:///" + pathToTrustedSpKey.toAbsolutePath().toString();
        log.debug("Path to trusted SP key '{}'", trustedSpKey);

        final Path pathToUntrustedSpCert = pathToIdPHome.resolve(Paths.get("credentials", "sp-untrusted.crt"));
        Assert.assertTrue(pathToUntrustedSpCert.toFile().exists(), "Path to sp-untrusted.crt not found");
        untrustedSpCert = "file:///" + pathToUntrustedSpCert.toAbsolutePath().toString();
        log.debug("Path to untrusted SP cert '{}'", untrustedSpCert);

        final Path pathToUntrustedSpKey = pathToIdPHome.resolve(Paths.get("credentials", "sp-untrusted.key"));
        Assert.assertTrue(pathToUntrustedSpKey.toFile().exists(), "Path to sp-untrusted.key not found");
        untrustedSpKey = "file:///" + pathToUntrustedSpKey.toAbsolutePath().toString();
        log.debug("Path to untrusted SP key '{}'", untrustedSpKey);
    }

    @BeforeClass
    protected void setUpURLs() throws Exception {

        startFlowURLPath = "/sp/SAML2/InitSSO/Redirect";

        loginPageURLPath = "/idp/profile/SAML2/Redirect/SSO";

        responsePageURLPath = "/sp/SAML2/POST/ACS";
    }

    /**
     * Change endpoint port from the default to whatever is in use.
     * 
     * @throws MalformedURLException
     */
    protected void adjustEndpointPort() throws MalformedURLException {
        final WebElement endpointInput = driver.findElement(By.id("saml2-attribute-query-endpoint"));
        final String oldEndpoint = endpointInput.getAttribute("value");
        final URLBuilder urlBuilder = new URLBuilder(oldEndpoint);
        urlBuilder.setPort(backchannelPort);
        final String newEndpoint = urlBuilder.buildURL();
        endpointInput.clear();
        endpointInput.sendKeys(newEndpoint);
    }

    /**
     * Enable attribute consent during an attribute query.
     * 
     * @throws IOException if the configuration file cannot be changed
     */
    protected void enableConsent() throws IOException {
        final Path pathToConsentInterceptConfigXML = Paths.get("conf", "intercept", "consent-intercept-config.xml");

        final String oldText =
                "<bean id=\"shibboleth.consent.AttributeQuery.Condition\" parent=\"shibboleth.Conditions.FALSE\" />";
        final String newText =
                "<bean id=\"shibboleth.consent.AttributeQuery.Condition\" parent=\"shibboleth.Conditions.TRUE\" />";

        replaceIdPHomeFile(pathToConsentInterceptConfigXML, oldText, newText);
    }

    /**
     * Use in-memory storage service for consent.
     * 
     * @throws IOException
     */
    protected void enableConsentStorageService() throws IOException {
        replaceIdPProperty("idp.consent.StorageService", "shibboleth.StorageService");
    }

    /**
     * Enable direct NameID mapping.
     * 
     * @throws IOException if the configuration file cannot be changed
     */
    protected void enableDirectNameIDMapping() throws IOException {
        final Path pathToSubjectC14nXML = pathToIdPHome.resolve(Paths.get("conf", "c14n", "subject-c14n.xml"));
        final String toUncomment = "<value>https://sp.example.org</value>";
        uncommentFile(pathToSubjectC14nXML, toUncomment);
    }

    /**
     * Set path to client TLS certificate resource. A <code>null</code> path clears the request parameter.
     * 
     * @param resource path to client TLS certificate resource
     */
    protected void setClientTLSCertificate(@Nullable final String resource) {
        setParameter("saml2-attribute-query-clientTLSCertificate", resource);
    }

    /**
     * Set path to client TLS private key resource. A <code>null</code> path clears the request parameter.
     * 
     * @param resource path to client TLS private key resource
     */
    protected void setClientTLSPrivateKey(@Nullable final String resource) {
        setParameter("saml2-attribute-query-clientTLSPrivateKey", resource);
    }

    /**
     * Set path to client signing certificate resource. A <code>null</code> path clears the request parameter.
     * 
     * @param resource path to client TLS signing resource
     */
    protected void setClientSigningCertificate(@Nullable final String resource) {
        setParameter("saml2-attribute-query-clientSigningCertificate", resource);
    }

    /**
     * Set path to client signing private key resource. A <code>null</code> path clears the request parameter.
     * 
     * @param resource path to client TLS private key resource
     */
    protected void setClientSigningPrivateKey(@Nullable final String resource) {
        setParameter("saml2-attribute-query-clientSigningPrivateKey", resource);
    }

    /**
     * Clear form input parameter and set value if not <code>null</code>.
     * 
     * @param name form parameter name
     * @param value form parameter value
     */
    private void setParameter(@Nonnull final String name, @Nullable final String value) {
        Constraint.isNotNull(name, "Parameter name cannot be null");
        final WebElement element = driver.findElement(By.id(name));
        element.clear();
        if (value != null) {
            element.sendKeys(value);
        }
    }

    /**
     * Submit the SAML 2 AttributeQuery form.
     */
    protected void submitAttributeQueryForm() {
        driver.findElement(By.id("saml2-attribute-query")).submit();
    }

    /**
     * Validate SAML error response.
     * 
     * @throws Exception
     */
    protected void validateErrorResponse() throws Exception {
        
        log.debug("Error response:\n{}", getPageSource());

        final Response response = unmarshallResponse(getPageSource());

        errorValidator.validateResponse(response);
    }

    /**
     * Validate SAML 2 SSO {@link Response}.
     * 
     * @throws Exception
     */
    protected void validateSSOResponse() throws Exception {
        ssoValidator.validateResponse(super.unmarshallResponse(getPageSource()));
    }

    /**
     * Activities common to tests. Start a browser, configure and start the IdP, etc.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    protected void commonSetup(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableDirectNameIDMapping();

        startServer();

        getAndWaitForTestbedPage();

        adjustEndpointPort();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testNoCertNoSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(null);

        setClientSigningCertificate(null);

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testNoCertTrustedSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(null);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testNoCertUntrustedSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(null);

        setClientSigningCertificate("classpath:/credentials/sp-untrusted.crt");

        setClientSigningPrivateKey("classpath:/credentials/sp-untrusted.key");

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testTrustedCertNoSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(null);

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testTrustedCertTrustedSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testTrustedCertUntrustedSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(untrustedSpCert);

        setClientSigningPrivateKey(untrustedSpKey);

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testUntrustedCertNotSigned(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(untrustedSpCert);

        setClientTLSPrivateKey(untrustedSpKey);

        setClientSigningCertificate(null);

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testUntrustedCertTrustedSignature(@Nullable final BrowserData browserData) throws Exception {
        
        commonSetup(browserData);

        setClientTLSCertificate(untrustedSpCert);

        setClientTLSPrivateKey(untrustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testUntrustedCertUntrustedSignature(@Nullable final BrowserData browserData) throws Exception {

        commonSetup(browserData);

        setClientTLSCertificate(untrustedSpCert);

        setClientTLSPrivateKey(untrustedSpKey);

        setClientSigningCertificate(untrustedSpCert);

        setClientSigningPrivateKey(untrustedSpKey);

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentDisabledReleaseAllAttributes(@Nullable final BrowserData browserData) throws Exception {

        enableConsentStorageService();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should have attributes

        submitAttributeQueryForm();

        validateResponse();

        // start SSO

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        waitForResponsePage();

        validateSSOResponse();

        // attribute query, should have attributes

        getAndWaitForTestbedPage();

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentEnabledNoConsent(@Nullable final BrowserData browserData) throws Exception {

        enableConsent();

        enableConsentStorageService();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should return an error since there are no consent storage records

        submitAttributeQueryForm();

        errorValidator.statusCode = StatusCode.RESPONDER;

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentEnabledDoNotRememberConsent(@Nullable final BrowserData browserData) throws Exception {

        enableConsent();

        enableConsentStorageService();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should return an error since there are no consent storage records

        submitAttributeQueryForm();

        errorValidator.statusCode = StatusCode.RESPONDER;

        validateErrorResponse();

        // start SSO

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        doNotRememberConsent();

        submitForm();

        waitForResponsePage();

        validateSSOResponse();

        // attribute query, should return an error since there are no consent storage records

        getAndWaitForTestbedPage();

        submitAttributeQueryForm();

        validateErrorResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentEnabledGlobalConsent(@Nullable final BrowserData browserData) throws Exception {

        enableConsent();

        enableConsentStorageService();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should return an error since there are no consent storage records

        submitAttributeQueryForm();

        errorValidator.statusCode = StatusCode.RESPONDER;

        validateErrorResponse();

        // start SSO

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        globalConsent();

        submitForm();

        waitForResponsePage();

        validateSSOResponse();

        // attribute query, should have attributes

        getAndWaitForTestbedPage();

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentEnabledReleaseAllAttributes(@Nullable final BrowserData browserData) throws Exception {

        enableConsent();

        enableConsentStorageService();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should return an error since there are no consent storage records

        submitAttributeQueryForm();

        errorValidator.statusCode = StatusCode.RESPONDER;

        validateErrorResponse();

        // start SSO

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        waitForResponsePage();

        validateSSOResponse();

        // attribute query, should have attributes

        getAndWaitForTestbedPage();

        submitAttributeQueryForm();

        validateResponse();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testConsentEnabledReleaseOneAttribute(@Nullable final BrowserData browserData) throws Exception {

        enableConsent();

        enableConsentStorageService();

        enablePerAttributeConsent();

        commonSetup(browserData);

        setClientTLSCertificate(trustedSpCert);

        setClientTLSPrivateKey(trustedSpKey);

        setClientSigningCertificate(trustedSpCert);

        setClientSigningPrivateKey(trustedSpKey);

        // attribute query, should return an error since there are no consent storage records

        submitAttributeQueryForm();

        errorValidator.statusCode = StatusCode.RESPONDER;

        validateErrorResponse();

        // start SSO

        startFlow();

        waitForLoginPage();

        login();

        // attribute release

        waitForAttributeReleasePage();

        releaseEmailAttributeOnly();

        rememberConsent();

        submitForm();

        waitForResponsePage();

        ssoValidator.expectedAttributes.clear();
        ssoValidator.expectedAttributes.add(validator.mailAttribute);

        validateSSOResponse();

        // attribute query, should have one attribute

        getAndWaitForTestbedPage();

        submitAttributeQueryForm();

        validator.expectedAttributes.clear();
        validator.expectedAttributes.add(validator.mailAttribute);

        validateResponse();
    }
}
