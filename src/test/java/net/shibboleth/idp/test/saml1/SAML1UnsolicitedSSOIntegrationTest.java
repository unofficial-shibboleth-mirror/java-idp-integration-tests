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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BrowserData;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SAML 1 unsolicited SSO test.
 */
public class SAML1UnsolicitedSSOIntegrationTest extends AbstractSAML1IntegrationTest {

    /** IdP endpoint. */
    @Nonnull public final String idpEndpointPath = "/idp/profile/Shibboleth/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shirePath = "/sp/SAML1/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    @BeforeClass(dependsOnMethods = {"setUpEndpoints"})
    public void setUpURLs() throws Exception {

        final String shire = getBaseURL() + shirePath;

        startFlowURLPath = idpEndpointPath + "?providerId=" + providerID + "&shire=" + shire + "&target=" + target;

        loginPageURLPath = idpEndpointPath;

        responsePageURLPath = shirePath;
    }

    /**
     * Test SAML 1 unsolicited SSO.
     * 
     * @param browserData browser/os/version triplet provided by data provider
     * @throws Exception if an error occurs
     */
    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testSAML1UnsolicitedSSO(@Nullable final BrowserData browserData) throws Exception {
        super.testSSO(browserData);
    }
}
