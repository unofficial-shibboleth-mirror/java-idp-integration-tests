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

import net.shibboleth.idp.test.BrowserData;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** SAML 2 HTTP Redirect binding test. */
public class SAML2SSORedirectIntegrationTest extends AbstractSAML2IntegrationTest {

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
    public void testSSOReleaseAllAttributes(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOReleaseAllAttributes(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOReleaseOneAttribute(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOReleaseOneAttribute(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSODoNotRememberConsent(@Nullable final BrowserData browserData) throws Exception {
        super.testSSODoNotRememberConsent(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOGlobalConsent(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOGlobalConsent(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOTermsOfUse(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOTermsOfUse(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOTermsOfUsePassive(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOTermsOfUsePassive(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOTermsOfUsePassiveNoConsent(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOTermsOfUsePassiveNoConsent(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOForceAuthn(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOForceAuthn(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOPassiveWithoutSession(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOPassiveWithoutSession(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOPassiveWithSession(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOPassiveWithSession(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSSOPassiveWithSessionNoConsent(@Nullable final BrowserData browserData) throws Exception {
        super.testSSOPassiveWithSessionNoConsent(browserData);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider", enabled = false)
    public void testSLO(@Nullable final BrowserData browserData) throws Exception {
        super.testSLO(browserData);
    }

}
