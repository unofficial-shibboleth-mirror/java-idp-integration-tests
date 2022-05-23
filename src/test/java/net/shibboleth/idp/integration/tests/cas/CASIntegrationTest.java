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

package net.shibboleth.idp.integration.tests.cas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.integration.tests.BaseIntegrationTest;
import net.shibboleth.idp.integration.tests.BrowserData;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Simple CAS integration test.
 */
public class CASIntegrationTest extends BaseIntegrationTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CASIntegrationTest.class);

    /** URL of testbed SP service. */
    @Nonnull private String spServicePageURLPath = "/sp/CAS/Service";

    /** URL of IdP to validate ticket. */
    @Nonnull private String idpServiceValidatePageURLPath = "/idp/profile/cas/serviceValidate";

    /** Expected CAS service response. */
    @Nonnull final String expectedCASServiceResponse = 
            "<cas:serviceresponse xmlns:cas=\"http://www.yale.edu/tp/cas\">"
            + "<cas:authenticationsuccess>"
              + "<cas:user>jdoe</cas:user>"
            + "</cas:authenticationsuccess>"
          + "</cas:serviceresponse>";

    /** Expected, but incorrect, CAS service response with attributes for IdP V3. */
    @Nonnull final String expectedCASServiceResponseWithAttributesV3 =
            "<cas:serviceresponse xmlns:cas=\"http://www.yale.edu/tp/cas\">"
          + "<cas:authenticationsuccess>"
          + "<cas:user>jdoe</cas:user>"
          + "<cas:attributes>"
          + "<cas:edupersonscopedaffiliation>member</cas:edupersonscopedaffiliation>"
          + "</cas:attributes>"
          + "</cas:authenticationsuccess>"
          + "</cas:serviceresponse>";

    /** Expected CAS service response with attributes. */
    @Nonnull final String expectedCASServiceResponseWithAttributes =
            "<cas:serviceresponse xmlns:cas=\"http://www.yale.edu/tp/cas\">"
            + "<cas:authenticationsuccess>"
              + "<cas:user>jdoe</cas:user>"
              + "<cas:attributes>ANY_ATTRIBUTES</cas:attributes>"
            + "</cas:authenticationsuccess>"
          + "</cas:serviceresponse>";

    @BeforeClass
    public void setUpURLs() throws Exception {

        startFlowURLPath = "/sp/CAS/InitSSO";

        loginPageURLPath = "/idp/profile/cas/login";
    }

    /**
     * Get the CAS service response from the page source.
     * 
     * This method is likely fragile across browsers, it is known to work with Firefox and HtmlUnit.
     * 
     * @param pageSource ...
     * 
     * @return the CAS service response.
     */
    @Nullable
    public String getCASServiceResponse(@Nullable final String pageSource) {
        if (StringSupport.trimOrNull(pageSource) == null) {
            return null;
        }

        // Trim anything before "<cas:serviceresponse"
        String response = pageSource.substring(pageSource.indexOf("<cas:serviceresponse"));

        // Trim anything after and including "</body>"
        response = response.substring(0, response.indexOf("</body>"));

        // Trim newlines and whitespace between elements
        response = response.replaceAll("\n", "").replaceAll("\\s+<", "<").replaceAll(">\\s+", ">");

        return response;
    }

    /**
     * Get expected CAS response either with or without attributes.
     * 
     * @param casServiceResponse
     * @return the expected CAS response
     */
    @Nonnull
    public String getExpectedCASServiceResponse(@Nonnull final String casServiceResponse) {
        if (casServiceResponse.contains("<cas:attributes>")) {
            return expectedCASServiceResponseWithAttributes;
        } else {
            return expectedCASServiceResponse;
        }
    }

    /**
     * Remove attributes from response and replace with placeholder.
     * 
     * Workaround for validating order of attributes.
     * 
     * @param casServiceResponse
     * @return the CAS service response with placeholder attributes
     */
    @Nonnull
    public String removeAttributesInCASServiceResponse(@Nonnull final String casServiceResponse) {
        return casServiceResponse.replaceFirst(
                "<cas:attributes>.*?</cas:attributes>",
                "<cas:attributes>ANY_ATTRIBUTES</cas:attributes>");
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testCASSSO(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableLogout();

        enableCAS();

        enableCASServiceDefinition();

        startServer();

        startFlow();

        waitForLoginPage();

        login();

        waitForPageWithURL(getBaseURL() + spServicePageURLPath);

        waitForPageBodyStartsWith("CAS Service Validate");
        
        driver.findElement(By.id("cas-service-validate")).click();

        waitForPageWithURL(getBaseURL() + idpServiceValidatePageURLPath);

        waitForPageBodyContains("jdoe");

        final String actualCASServiceResponse = getCASServiceResponse(driver.getPageSource());

        final String modifiedCASServiceResponse = removeAttributesInCASServiceResponse(actualCASServiceResponse);

        final String expectedCASServiceResponse = getExpectedCASServiceResponse(actualCASServiceResponse);

        Assert.assertEquals(modifiedCASServiceResponse, expectedCASServiceResponse);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testCASSSOWithAttributes(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        enableLogout();

        enableCAS();

        enableCASServiceDefinition();

        enableCASAttributes();

        startServer();

        startFlow();

        waitForLoginPage();

        login();

        waitForPageWithURL(getBaseURL() + spServicePageURLPath);

        waitForPageBodyStartsWith("CAS Service Validate");

        driver.findElement(By.id("cas-service-validate")).click();

        waitForPageWithURL(getBaseURL() + idpServiceValidatePageURLPath);

        waitForPageBodyContains("jdoe");

        final String actualCASServiceResponse = getCASServiceResponse(driver.getPageSource());

        if (idpVersion.startsWith("3")) {
            Assert.assertEquals(actualCASServiceResponse, expectedCASServiceResponseWithAttributesV3);
        } else {
            final String modifiedCASServiceResponse = removeAttributesInCASServiceResponse(actualCASServiceResponse);

            final String expectedCASServiceResponse = getExpectedCASServiceResponse(actualCASServiceResponse);

            Assert.assertEquals(modifiedCASServiceResponse, expectedCASServiceResponse);
        }
    }

}
