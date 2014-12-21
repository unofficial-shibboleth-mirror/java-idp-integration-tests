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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SAML 2 unsolicited SSO with per-attribute consent test.
 */
public class SAML2UnsolicitedSSOPerAttributeConsentIntegrationTest extends AbstractSAML2UnsolicitedSSOIntegrationTest {

    /**
     * Enable per attribute consent.
     *
     * @throws Exception
     */
    @BeforeClass protected void enablePerAttributeConsent() throws Exception {
        replaceIdPProperty("idp.consent.allowPerAttribute", "true");
    }

    /**
     * Restore idp.properties from original source.
     * 
     * @throws IOException if an I/O error occurs
     */
    @AfterClass(alwaysRun = true) protected void restoreConfiguration() throws IOException {
        restoreIdPProperties();
    }

    /**
     * Test releasing a single attribute.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseOneAttribute() throws Exception {

        startFlow();

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
    }

    /**
     * Test releasing a single attribute and remember consent.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSSOReleaseOneAttributeRememberConsent() throws Exception {

        startFlow();

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

        driver.get(url);

        waitForResponsePage();

        validateResponse();
    }
}
