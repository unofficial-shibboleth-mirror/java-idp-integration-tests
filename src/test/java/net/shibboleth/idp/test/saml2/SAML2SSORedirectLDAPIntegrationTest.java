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

import java.util.List;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.opensaml.saml.saml2.core.Attribute;
import org.testng.Assert;
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

    public void setUpIDP1026Validator() {

        final SAMLObjectBuilder<Attribute> builder = (SAMLObjectBuilder<Attribute>) XMLObjectProviderRegistrySupport
                .getBuilderFactory().<Attribute> getBuilderOrThrow(Attribute.DEFAULT_ELEMENT_NAME);

        final XMLObjectBuilder<XSAny> anyBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().<XSAny> getBuilderOrThrow(XSAny.TYPE_NAME);

        // the expected uid attribute
        final Attribute uidAttribute = builder.buildObject();
        uidAttribute.setName("urn:oid:0.9.2342.19200300.100.1.1");
        uidAttribute.setNameFormat(Attribute.URI_REFERENCE);
        uidAttribute.setFriendlyName("uid");
        final XSAny uidValue = anyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        uidValue.setTextContent("IDP-1026");
        uidAttribute.getAttributeValues().add(uidValue);

        validator.expectedAttributes.clear();
        validator.expectedAttributes.add(uidAttribute);
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testIDP1026(@Nullable final BrowserData browserData) throws Exception {

        setUpIDP1026Validator();

        enableAttributeResolverLDAP();

        enableAttributeResolverLDAPExportUid();

        startSeleniumClient(browserData);

        startServer();

        startFlow();

        waitForLoginPage();

        login("IDP-1026");

        // attribute release

        waitForAttributeReleasePage();

        // fail if mail attribute is displayed

        final WebElement table = driver.findElement(By.tagName("table"));
        final List<WebElement> tds = table.findElements(By.tagName("td"));
        for (final WebElement td : tds) {
            Assert.assertNotEquals("mail", td.getText(), "Emtpy mail attribute should not be displayed");
            Assert.assertNotEquals("ZERO_LENGTH_VALUE", td.getText(), "Emtpy attributes should not be displayed");
            Assert.assertNotEquals("NULL_VALUE", td.getText(), "Emtpy attributes should not be displayed");
        }

        releaseAllAttributes();

        rememberConsent();

        submitForm();

        // response

        waitForResponsePage();

        validateResponse();

        // twice

        startFlow();

        waitForResponsePage();

        validateResponse();
    }
}
