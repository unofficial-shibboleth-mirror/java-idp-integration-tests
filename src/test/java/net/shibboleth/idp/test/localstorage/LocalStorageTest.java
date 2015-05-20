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

package net.shibboleth.idp.test.localstorage;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.shibboleth.idp.test.BaseIntegrationTest;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Local storage tests.
 */
public class LocalStorageTest extends BaseIntegrationTest {

    /** Title of local storage test view page. */
    public final static String LOCAL_STORAGE_TEST_VIEW_PAGE_TITLE = "Local Storage Test View";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LocalStorageTest.class);

    /** Key used to retrieve serialized storage service from local storage. */
    @Nonnull protected String key = "shib_idp_ls";

    /** URL of test flow to read from local storage. */
    @Nonnull protected String readURL = BASE_URL + "/idp/profile/test/local-storage/read";

    /** JavaScript capable web driver wrapper. */
    @Nonnull protected LocalStorageWebDriverWrapper wrapper;

    /** URL of test flow to write to local storage. */
    @Nonnull protected String writeURL = BASE_URL + "/idp/profile/test/local-storage/write";

    /**
     * Set up local storage web driver wrapper.
     */
    @BeforeMethod protected void setUpLocalStorageWrapper() throws IOException {
        wrapper = new LocalStorageWebDriverWrapper(driver);
    }

    /**
     * Set up test views.
     */
    @BeforeMethod protected void setUpTestViews() throws IOException {
        replaceIdPProperty("idp.views", "%{idp.home}/views,%{idp.home}/views-test");
    }

    /**
     * Wait for page with title {@link #LOCAL_STORAGE_TEST_VIEW_PAGE_TITLE}.
     */
    protected void waitForLocalStorageTestViewPage() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(LOCAL_STORAGE_TEST_VIEW_PAGE_TITLE);
            }
        });
    }

    @Test public void testNothingToRead() throws Exception {

        startJettyServer();

        driver.get(readURL);

        waitForLocalStorageTestViewPage();

        final String localStorageViaJavaScript = wrapper.getItem(key);
        final String localStorageViaView = driver.findElement(By.id("localStorage")).getText();
        final String localStorageSupportViaView = driver.findElement(By.id("localStorageSupport")).getText();

        Assert.assertNull(localStorageViaJavaScript);
        if (driver instanceof FirefoxDriver) {
            Assert.assertEquals(localStorageViaView, "");
        } else if (driver instanceof HtmlUnitDriver) {
            Assert.assertEquals(localStorageViaView, "null");
        }

        Assert.assertEquals(localStorageSupportViaView, "true");
    }

    @Test public void testWrite() throws Exception {

        startJettyServer();

        driver.get(writeURL + "?localStorageValue=foo");

        waitForLocalStorageTestViewPage();

        final String localStorageViaJavaScript = wrapper.getItem(key);
        final String localStorageViaView = driver.findElement(By.id("localStorage")).getText();
        final String localStorageSupportViaView = driver.findElement(By.id("localStorageSupport")).getText();

        Assert.assertEquals(localStorageViaJavaScript, "foo");
        // only set during read test flow
        Assert.assertEquals(localStorageViaView, "");
        Assert.assertEquals(localStorageSupportViaView, "");
    }

    // TODO more tests

}
