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

import javax.annotation.Nonnull;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** SAML 2 unsolicited SSO test. */
public class SAML2UnsolicitedSSOIntegrationTest extends AbstractSAML2IntegrationTest {

    /** IdP endpoint. */
    @Nonnull public final String idpEndpoint = BASE_URL + "/idp/profile/SAML2/Unsolicited/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shire = BASE_URL + "/sp/SAML2/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    /** URL. */
    @Nonnull public final String url = idpEndpoint + "?providerId=" + providerID + "&shire=" + shire + "&target="
            + target;

    @BeforeClass public void setUpURLs() throws Exception {

        startFlowURL = url;

        loginPageURL = idpEndpoint;

        responsePageURL = shire;
    }
    

    @Test public void testSSOReleaseAllAttributes() throws Exception {
        super.testSSOReleaseAllAttributes();
    }

    @Test public void testSSOReleaseOneAttribute() throws Exception {
        super.testSSOReleaseOneAttribute();
    }

    @Test public void testSSODoNotRememberConsent() throws Exception {
        super.testSSODoNotRememberConsent();
    }

    @Test public void testSSOGlobalConsent() throws Exception {
        super.testSSOGlobalConsent();
    }

    @Test public void testSSOTermsOfUse() throws Exception {
        super.testSSOTermsOfUse();
    }
}