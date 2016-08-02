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
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.idp.test.flows.saml2.SAML2TestResponseValidator;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/** SAML 2 AttributeQuery test. */
public class SAML2AttributeQueryIntegrationTest extends AbstractSAML2IntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2AttributeQueryIntegrationTest.class);

    /**
     * Set up the validator for SAML 2 attribute queries.
     */
    public void setupValidator() {
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
     * Enable direct NameID mapping.
     * 
     * @throws IOException if the configuration file cannot be changed
     */
    public void enableDirectNameIDMapping() throws IOException {
        final Path pathToSubjectC14nXML = pathToIdPHome.resolve(Paths.get("conf", "c14n", "subject-c14n.xml"));
        final String toUncomment = "<value>https://sp.example.org</value>";
        uncommentFile(pathToSubjectC14nXML, toUncomment);
    }

    /**
     * Submit the SAML 2 AttributeQuery form.
     */
    public void submitAttributeQueryForm() {
        driver.findElement(By.id("saml2-attribute-query")).submit();
    }

    /**
     * Change endpoint port from the default to whatever is in use.
     */
    public void adjustEndpointPort() {
        final WebElement endpointInput = driver.findElement(By.id("saml2-attribute-query-endpoint"));
        final String oldEndpoint = endpointInput.getAttribute("value");
        final String newEndpoint = oldEndpoint.replaceAll("9443", backchannelPort.toString());
        endpointInput.clear();
        endpointInput.sendKeys(newEndpoint);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testAttributeQuery(@Nullable final BrowserData browserData) throws Exception {

        setupValidator();

        startSeleniumClient(browserData);

        enableDirectNameIDMapping();

        startServer();

        getAndWaitForTestbedPage();

        adjustEndpointPort();

        submitAttributeQueryForm();

        validateResponse();
    }

}
