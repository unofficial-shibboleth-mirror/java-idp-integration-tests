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

package net.shibboleth.idp.integration.tests.ui.csrf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.integration.tests.BaseIntegrationTest;
import net.shibboleth.idp.integration.tests.BrowserData;

/**
 * Test the anti-csrf token is required when submitting the username and password form.
 */
public class CSRFMitigationTest extends BaseIntegrationTest {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CSRFMitigationTest.class);
    
    /** Title of error page where we would expect a login without CSRF token to end up. */
    public final static String ERROR_PAGE_TITLE_PROPERTY = "idp.title";
    
    /** The title used when displaying the invalid CSRF token page.*/
    public final static String CSRF_ERROR_PAGE_TITLE_PROPERTY = "invalid-csrf-token.title";
    
    /** The message show when the invalid CSRF token page is displayed.*/
    public final static String CSRF_ERROR_PAGE_MESSAGE_PROPERTY = "invalid-csrf-token.message";
    
    /** The name of the input element holding the anti-csrf token value.*/
    public final static String CSRF_INPUT_ELEMENT_NAME = "csrf_token";
    
    /** The path to the messages.properties file.*/
    public Path pathToMessagesProperties;

    @BeforeClass
    public void setUp() throws Exception {

        startFlowURLPath = "/sp/SAML2/InitSSO/Redirect";

        loginPageURLPath = "/idp/profile/SAML2/Redirect/SSO";      

    }

    /**
     * 
     * Check that a username/password login form submitted without an anti-csrf token renders the invalid CSRF token page.
     * <p>Only runs for IdP v4 or greater</p>
     * 
     * @param browserData the browser data
     * @throws Exception on exception
     */
    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testCSRFTokenRemovedFromLoginPage(@Nullable final BrowserData browserData) throws Exception {
        
        if (getMajorIdPVersionAsInt() >= 4) {
         
            startSeleniumClient(browserData);
            
            //make sure CSRF protection is enabled
            replaceIdPProperty("idp.csrf.enabled", "true");
            //make sure we are using the password flow
            replaceIdPProperty("idp.authn.flows", "Password");
            
            startServer();
            startFlow();
            waitForLoginPage(); 
            removeCSRFTokenAndlogin("jdoe","changeit");
            checkCSRFErrorPage();
        
        }
    }
    
    /**
     * Get the IdP Major version from <code>idpVersion</code> as an int.
     * 
     * @return the IdP major version as an int.
     * 
     * @throws Exception if it could not be converted.
     */
    private int getMajorIdPVersionAsInt() throws Exception{
        
        if (idpVersion!=null) {
            String[] splitVersion = idpVersion.split("\\.");
            if (splitVersion.length>0) {
                String majorVersionString = splitVersion[0];
                return Integer.parseInt(majorVersionString);               
            }
        }
        throw new Exception("Could not get IdP major version as int");
        
    }

    /**
     * <p>Check the CSRF error page is displayed.</p>
     * 
     * <p> More specifically, checks the <code>div</code> element with class <code>content</code> 
     * contains text content which matches with the {@value #CSRF_ERROR_PAGE_MESSAGE_PROPERTY} 
     * system messages property.</p>
     * 
     * @throws IOException if the messages properties file can not be loaded,
     */
    private void checkCSRFErrorPage() throws IOException {
        
        final String csrfErrorMessage = getMessage(CSRF_ERROR_PAGE_MESSAGE_PROPERTY);
        Assert.assertNotNull(csrfErrorMessage);
        final String errorPageTitle = getMessage(ERROR_PAGE_TITLE_PROPERTY);
        Assert.assertNotNull(errorPageTitle);
        final String errorPageSubtitle = getMessage(CSRF_ERROR_PAGE_TITLE_PROPERTY);
        Assert.assertNotNull(errorPageSubtitle);
        Assert.assertTrue(driver.getPageSource()!=null);
        final String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains(csrfErrorMessage));
        Assert.assertTrue(pageSource.contains(errorPageTitle + " - " + errorPageSubtitle));
    }

    /**
     * Wait for the username and password login page to display (max wait is 3 seconds). Find the anti-csrf token
     * hidden input element and unhide it using Javascript. Clear the token from the input element, 
     * fill in the form with proper credentials and submit the form.
     * 
     * @param user the user to authenticate
     * @param password the password of the user to authenticate
     */
    private void removeCSRFTokenAndlogin(final @Nonnull String user, final @Nonnull String password) {


        //wait for page to load for max 3 seconds.
        WebDriverWait wait = new WebDriverWait(driver, 3);
        wait.until(x -> x.findElement(By.name("j_username")));

        //check the token input exists - fail if not.
        final List<WebElement> csrfTokenInput = driver.findElements(By.name(CSRF_INPUT_ELEMENT_NAME));
        Assert.assertNotNull(csrfTokenInput);
        Assert.assertEquals(csrfTokenInput.size(),1,"Could not find csrf token input element, is csrf protection enabled?");
        
        //unhide the csrftoken hidden input using JavaScript.
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript(
                "document.getElementsByName('" + CSRF_INPUT_ELEMENT_NAME + "')[0].setAttribute('type', 'text');");
        
        //now clear the CSRF token.
        final WebElement csrfToken = driver.findElement(By.name(CSRF_INPUT_ELEMENT_NAME));
        csrfToken.clear();

        //finally add username and password.
        final WebElement usernameElement = driver.findElement(By.name("j_username"));
        final WebElement passwordElement = driver.findElement(By.name("j_password"));
        usernameElement.sendKeys(user);
        passwordElement.sendKeys(password);

        //submit the form.
        submitForm();

    }
    
    

}
