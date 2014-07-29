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

import net.shibboleth.idp.test.flows.saml2.SAML2TestResponseValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * SAML 2 unsolicited SSO test.
 */
public class SAML2UnsolicitedSSOIntegrationTest extends AbstractSAML2IntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2UnsolicitedSSOIntegrationTest.class);

    /** IdP endpoint. */
    @Nonnull public final String idpEndpoint = "https://localhost:8443/idp/profile/SAML2/Unsolicited/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shire = "https://localhost:8443/sp/SAML2/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    /** URL. */
    @Nonnull public final String url = idpEndpoint + "?providerId=" + providerID + "&shire=" + shire + "&target="
            + target;

    /**
     * Test SAML 2 unsolicited SSO.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML2UnsolicitedSSO() throws Exception {

        final HtmlUnitDriver driver = new HtmlUnitDriver();
        driver.setJavascriptEnabled(true);

        driver.get(url);

        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(idpEndpoint);
            }
        });

        final WebElement username = driver.findElement(By.name("j_username"));
        final WebElement password = driver.findElement(By.name("j_password"));
        final WebElement submit = driver.findElement(By.name("_eventId_proceed"));

        username.sendKeys("jdoe");
        password.sendKeys("changeit");
        submit.click();

        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().equals(shire);
            }
        });

        final Response response = unmarshallResponse(driver.getPageSource());

        final SAML2TestResponseValidator validator = new SAML2TestResponseValidator();
        validator.spCredential = getSPCredential();
        validator.authnContextClassRef = AuthnContext.PASSWORD_AUTHN_CTX;
        validator.validateResponse(response);
    }
}
