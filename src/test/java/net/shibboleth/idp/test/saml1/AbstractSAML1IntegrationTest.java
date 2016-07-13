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

package net.shibboleth.idp.test.saml1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.idp.test.flows.saml1.SAML1TestResponseValidator;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract SAML 1 integration test.
 */
public class AbstractSAML1IntegrationTest extends BaseIntegrationTest {

    /** Response validator. */
    @Nonnull protected SAML1TestResponseValidator validator;

    /**
     * Setup response validator.
     * 
     * @throws IOException if an I/O error occurs
     */
    @BeforeMethod
    public void setUpValidator() throws IOException {
        validator = new SAML1TestResponseValidator();
        validator.authenticationMethod = AuthenticationStatement.PASSWORD_AUTHN_METHOD;
    }

    /**
     * Validate SAML 1 response
     * 
     * @throws Exception
     */
    public void validateResponse() throws Exception {
        validator.validateResponse(unmarshallResponse(getPageSource()));
    }

    /**
     * Unmarshall the XML response into a SAML 1 Response object.
     * 
     * @param response the XML response
     * @return the SAML 1 Response object
     * @throws UnsupportedEncodingException if an error occurs
     * @throws XMLParserException if an error occurs
     * @throws UnmarshallingException if an error occurs
     */
    @Nonnull
    public Response unmarshallResponse(@Nullable final String response)
            throws UnsupportedEncodingException, XMLParserException, UnmarshallingException {
        Assert.assertNotNull(response);
        final Document doc = parserPool.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
        final Element element = doc.getDocumentElement();
        Assert.assertNotNull(element);
        final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        Assert.assertNotNull(unmarshaller);
        final Response object = (Response) unmarshaller.unmarshall(element);
        Assert.assertNotNull(object);
        return object;
    }

    /**
     * Test SAML 1 SSO.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    public void testSSO(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableCustomRelyingPartyConfiguration();

        startServer();

        startFlow();

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
}
