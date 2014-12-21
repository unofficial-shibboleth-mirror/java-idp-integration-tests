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

import javax.annotation.Nonnull;

import net.shibboleth.idp.test.flows.saml1.SAML1TestResponseValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;

/**
 * Abstract SAML 1 unsolicited SSO test.
 */
public abstract class AbstractSAML1UnsolicitedSSOIntegrationTest extends AbstractSAML1IntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAML1UnsolicitedSSOIntegrationTest.class);

    /** IdP endpoint. */
    @Nonnull public final String idpEndpoint = "https://localhost:8443/idp/profile/Shibboleth/SSO";

    /** Provider ID. */
    @Nonnull public final String providerID = "https://sp.example.org";

    /** SHIRE. */
    @Nonnull public final String shire = "https://localhost:8443/sp/SAML1/POST/ACS";

    /** Target. */
    @Nonnull public final String target = "MyRelayState";

    /** URL. */
    @Nonnull public final String url = idpEndpoint + "?providerId=" + providerID + "&shire=" + shire + "&target="
            + target;

    /** Response validator. */
    @Nonnull protected SAML1TestResponseValidator validator;

    /**
     * Setup response validator.
     * 
     * @throws IOException if an I/O error occurs
     */
    @BeforeMethod protected void setUpValidator() throws IOException {
        validator = new SAML1TestResponseValidator();
        validator.authenticationMethod = AuthenticationStatement.PASSWORD_AUTHN_METHOD;
    }

    /**
     * <ul>
     * <li>Wait for login page.</li>
     * </ul>
     */
    protected void startFlow() {

        driver.get(url);

        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().startsWith(idpEndpoint);
            }
        });
    }

    /**
     * Wait for page containing SAML response.
     */
    protected void waitForResponsePage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getCurrentUrl().equals(shire);
            }
        });
    }

    /**
     * Validate SAML response
     * 
     * @throws Exception
     */
    protected void validateResponse() throws Exception {
        if (driver instanceof FirefoxDriver) {
            validator.validateResponse(unmarshallResponse(driver.findElement(By.tagName("body")).getText()));
        } else {
            validator.validateResponse(unmarshallResponse(driver.getPageSource()));
        }
    }
}
