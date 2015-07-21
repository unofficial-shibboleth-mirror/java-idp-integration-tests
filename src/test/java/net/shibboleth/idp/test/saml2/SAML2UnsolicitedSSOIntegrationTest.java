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
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.idp.test.IsLocalTestListener;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.saucelabs.testng.SauceOnDemandTestListener;

/** SAML 2 unsolicited SSO test. */
@Listeners({IsLocalTestListener.class, SauceOnDemandTestListener.class})
public class SAML2UnsolicitedSSOIntegrationTest extends AbstractSAML2IntegrationTest {

    /** IdP endpoint. */
    @Nonnull public final String idpEndpointPath = "/idp/profile/SAML2/Unsolicited/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shirePath = "/sp/SAML2/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    @BeforeClass(dependsOnMethods = {"setUpEndpoints"})
    public void setUpURLs() throws Exception {

        final String shire = getBaseURL() + shirePath;

        startFlowURLPath = idpEndpointPath + "?providerId=" + providerID + "&shire=" + shire + "&target=" + target;

        loginPageURLPath = idpEndpointPath;

        responsePageURLPath = shirePath;
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
}
