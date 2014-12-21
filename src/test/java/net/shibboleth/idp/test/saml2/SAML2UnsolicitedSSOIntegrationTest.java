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

import org.testng.annotations.Test;

/**
 * SAML 2 unsolicited SSO test.
 */
public class SAML2UnsolicitedSSOIntegrationTest extends AbstractSAML2UnsolicitedSSOIntegrationTest {

    /**
     * Test SAML 2 unsolicited SSO.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2UnsolicitedSSO() throws Exception {

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

        startFlow();

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

        startFlow();

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

        driver.get(url);

        // attribute release again

        waitForResponsePage();

        validateResponse();
    }
}
