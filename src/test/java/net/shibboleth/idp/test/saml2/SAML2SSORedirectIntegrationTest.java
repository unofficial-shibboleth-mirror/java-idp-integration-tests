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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** SAML 2 HTTP Redirect binding test. */
public class SAML2SSORedirectIntegrationTest extends AbstractSAML2IntegrationTest {

    @BeforeClass public void setUpURLs() throws Exception {

        startFlowURL = BASE_URL + "/sp/SAML2/InitSSO/Redirect";

        loginPageURL = BASE_URL + "/idp/profile/SAML2/Redirect/SSO";

        responsePageURL = BASE_URL + "/sp/SAML2/POST/ACS";

        isPassiveRequestURL = BASE_URL + "/sp/SAML2/InitSSO/Passive";

        forceAuthnRequestURL = BASE_URL + "/sp/SAML2/InitSSO/ForceAuthn";
        
        idpLogoutURL = BASE_URL + "/idp/profile/SAML2/Redirect/SLO";
        
        spLogoutURL = BASE_URL + "/sp/SAML2/Redirect/SLO";
        
        logoutTransientIDInputID = "InitSLO_Redirect";
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

    @Test public void testSSOForceAuthn() throws Exception {
        super.testSSOForceAuthn();
    }

    @Test public void testSSOPassiveWithoutSession() throws Exception {
        super.testSSOPassiveWithoutSession();
    }

    @Test public void testSSOPassiveWithSession() throws Exception {
        super.testSSOPassiveWithSession();
    }
    
    @Test(enabled = false) public void testSLO() throws Exception {
        super.testSLO();
    }

}
