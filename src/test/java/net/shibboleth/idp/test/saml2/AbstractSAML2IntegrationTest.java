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
import net.shibboleth.utilities.java.support.xml.XMLParserException;

import org.cryptacular.util.CertUtil;
import org.cryptacular.util.KeyPairUtil;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract SAML 2 integration test.
 */
public class AbstractSAML2IntegrationTest extends BaseIntegrationTest {

    /** SP private key resource location. */
    @Nonnull public final String SP_KEY = "/credentials/sp.key";

    /** SP certificate resource location. */
    @Nonnull public final String SP_CRT = "/credentials/sp.crt";

    /**
     * Unmarshall the XML response into a SAML 2 Response object.
     * 
     * @param response the XML response
     * @return the SAML 2 Response object
     * @throws UnsupportedEncodingException if an error occurs
     * @throws XMLParserException if an error occurs
     * @throws UnmarshallingException if an error occurs
     */
    @Nonnull public Response unmarshallResponse(@Nullable final String response) throws UnsupportedEncodingException,
            XMLParserException, UnmarshallingException {
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
     * Get the SP credential.
     * 
     * @return the SP credential.
     * @throws IOException if an error occurs reading the credential
     */
    @Nonnull public Credential getSPCredential() throws IOException {

        final ClassPathResource spKeyResource = new ClassPathResource(SP_KEY);
        Assert.assertTrue(spKeyResource.exists());
        final PrivateKey spPrivateKey = KeyPairUtil.readPrivateKey(spKeyResource.getInputStream());

        final ClassPathResource spCrtResource = new ClassPathResource(SP_CRT);
        Assert.assertTrue(spCrtResource.exists());
        final X509Certificate spEntityCert = (X509Certificate) CertUtil.readCertificate(spCrtResource.getInputStream());

        return new BasicX509Credential(spEntityCert, spPrivateKey);
    }
}
