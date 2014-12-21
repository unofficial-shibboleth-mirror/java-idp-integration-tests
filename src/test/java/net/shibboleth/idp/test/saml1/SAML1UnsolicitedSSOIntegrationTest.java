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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SAML 1 unsolicited SSO test.
 */
public class SAML1UnsolicitedSSOIntegrationTest extends AbstractSAML1UnsolicitedSSOIntegrationTest {

    /**
     * Activate terms-of-use flow.
     *
     * @throws Exception
     */
    @BeforeClass protected void enableConsentFlows() throws Exception {
        final Path pathToRelyingParty =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "relying-party.xml");
        Assert.assertTrue(pathToRelyingParty.toFile().exists());

        final Path pathToRelyingPartyWithConsent =
                Paths.get(pathToIdPHome.toAbsolutePath().toString(), "conf", "relying-party-with-consent.xml");
        Assert.assertTrue(pathToRelyingPartyWithConsent.toFile().exists());

        Files.copy(pathToRelyingPartyWithConsent, pathToRelyingParty, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restore relying-party.xml from original source.
     * 
     * @throws IOException if an I/O error occurs
     */
    @AfterClass(alwaysRun = true) protected void restoreConfiguration() throws IOException {
        restoreRelyingPartyXML();
    }

    /**
     * Test SAML 1 unsolicited SSO.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1UnsolicitedSSO() throws Exception {

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
}
