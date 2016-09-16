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

import javax.annotation.Nullable;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.BrowserData;

/** SAML 2 HTTP Redirect binding test with attributes from LDAP. */
public class SAML2SSORedirectLDAPIntegrationTest extends AbstractSAML2IntegrationTest {

    @BeforeClass
    public void setUpURLs() throws Exception {

        startFlowURLPath = "/sp/SAML2/InitSSO/Redirect";

        loginPageURLPath = "/idp/profile/SAML2/Redirect/SSO";

        responsePageURLPath = "/sp/SAML2/POST/ACS";

        isPassiveRequestURLPath = "/sp/SAML2/InitSSO/Passive";

        forceAuthnRequestURLPath = "/sp/SAML2/InitSSO/ForceAuthn";

        idpLogoutURLPath = "/idp/profile/SAML2/Redirect/SLO";

        spLogoutURLPath = "/sp/SAML2/Redirect/SLO";

        logoutTransientIDInputID = "InitSLO_Redirect";
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOReleaseLDAPAttributes(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOReleaseLDAPAttributes(browserData);
    }
}
