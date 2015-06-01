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
import java.net.MalformedURLException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.net.URLBuilder;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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

    /** Local storage exception identifier. */
    @Nonnull public final static String LOCAL_STORAGE_EXCEPTION_ID = "localStorageException";

    /** Local storage item key identifier. */
    @Nonnull public final static String LOCAL_STORAGE_KEY_ID = "localStorageKey";

    /** Local storage success identifier. */
    @Nonnull public final static String LOCAL_STORAGE_SUCCESS_ID = "localStorageSuccess";

    /** Local storage support identifier. */
    @Nonnull public final static String LOCAL_STORAGE_SUPPORTED_ID = "localStorageSupported";

    /** Local storage item value identifier. */
    @Nonnull public final static String LOCAL_STORAGE_VALUE_ID = "localStorageValue";

    /** Local storage version identifier. */
    @Nonnull public final static String LOCAL_STORAGE_VERSION_ID = "localStorageVersion";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LocalStorageTest.class);

    /** Test local storage item key. */
    @Nonnull protected String testKey = "shib_idp_ls_test_key";

    /** Test local storage item value. */
    @Nonnull protected String testValue = "shib_idp_ls_test_key_value";

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

    /**
     * Get the local storage exception from the test page.
     * 
     * @return the local storage exception
     */
    protected String getLocalStorageException() {
        return driver.findElement(By.id(LOCAL_STORAGE_EXCEPTION_ID)).getText();
    }

    /**
     * Get the local storage key from the test page.
     * 
     * @return the local storage key
     */
    protected String getLocalStorageKey() {
        return driver.findElement(By.id(LOCAL_STORAGE_KEY_ID)).getText();
    }

    /**
     * Get whether the attempt to read or write local storage was successful.
     * 
     * @return whether the attempt to read or write local storage was successful
     */
    protected String getLocalStorageSuccess() {
        return driver.findElement(By.id(LOCAL_STORAGE_SUCCESS_ID)).getText();
    }

    /**
     * Get whether local storage is supported from the test page.
     * 
     * @return whether local storage is supported
     */
    protected String getLocalStorageSupported() {
        return driver.findElement(By.id(LOCAL_STORAGE_SUPPORTED_ID)).getText();
    }

    /**
     * Get the local storage value from the test page via the test flow.
     * 
     * @return the local storage value via the test flow
     */
    protected String getLocalStorageValue() {
        return driver.findElement(By.id(LOCAL_STORAGE_VALUE_ID)).getText();
    }

    /**
     * Get the local storage value by executing JavaScript on the test page.
     * 
     * @return the local storage value via JavaScript
     */
    protected String getLocalStorageValueViaGetItem() {
        return driver.findElement(By.id("localStorageGetItem")).getText();
    }

    /**
     * Get the local storage value from the client directly by executing JavaScript.
     * 
     * @param key local storage item key
     * @return the local storage value directly via JavaScript
     */
    protected String getLocalStorageValueViaWrapper(@Nullable final String key) {
        return wrapper.getItem(key);
    }

    /**
     * Get the local storage version from the test page.
     * 
     * @return the local storage version
     */
    protected String getLocalStorageVersion() {
        return driver.findElement(By.id(LOCAL_STORAGE_VERSION_ID)).getText();
    }

    /**
     * Build the URL to read from local storage using the test flow.
     * 
     * The read test flow expects a {@link #LOCAL_STORAGE_KEY_ID} query parameter.
     * 
     * @param localStorageKey the key of the item to read from local storage.
     * @return the URL to read from local storage using the test flow
     * @throws MalformedURLException
     */
    protected String buildReadURL(@Nullable final String localStorageKey) throws MalformedURLException {
        final URLBuilder urlBuilder = new URLBuilder(readURL);
        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        if (localStorageKey != null) {
            queryParams.add(new Pair<String, String>(LOCAL_STORAGE_KEY_ID, localStorageKey));
        }
        final String URL = urlBuilder.buildURL();
        log.debug("URL to read from local storage is '{}'", URL);
        return URL;
    }

    /**
     * Build the URL to write to local storage using the test flow.
     * 
     * @param localStorageKey the key of the item to be written to local storage.
     * @param localStorageValue the value of the item to be written to local storage.
     * @return the URL to write to local storage using the test flow
     * @throws MalformedURLException
     */
    protected String buildWriteURL(@Nullable final String localStorageKey, @Nullable final String localStorageValue)
            throws MalformedURLException {
        final URLBuilder urlBuilder = new URLBuilder(writeURL);
        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        if (localStorageKey != null) {
            queryParams.add(new Pair<String, String>(LOCAL_STORAGE_KEY_ID, localStorageKey));
        }
        if (localStorageValue != null) {
            queryParams.add(new Pair<String, String>(LOCAL_STORAGE_VALUE_ID, localStorageValue));
        }
        final String URL = urlBuilder.buildURL();
        log.debug("URL to write to local storage is '{}'", URL);
        return URL;
    }

    @Test public void testNothingToRead() throws Exception {

        startJettyServer();

        driver.get(buildReadURL(testKey));

        waitForLocalStorageTestViewPage();

        Assert.assertEquals(getLocalStorageException(), "");
        Assert.assertEquals(getLocalStorageKey(), testKey);
        Assert.assertEquals(getLocalStorageSuccess(), "true");
        Assert.assertEquals(getLocalStorageSupported(), "true");
        if (driver instanceof HtmlUnitDriver) {
            Assert.assertEquals(getLocalStorageValue(), "null");
        } else {
            Assert.assertEquals(getLocalStorageValue(), "");
        }
        Assert.assertEquals(getLocalStorageValueViaGetItem(), "null");
        Assert.assertNull(getLocalStorageValueViaWrapper(testKey));
        Assert.assertEquals(getLocalStorageVersion(), ""); // TODO
    }

    @Test public void testWrite() throws Exception {

        startJettyServer();

        driver.get(buildWriteURL(testKey, testValue));

        waitForLocalStorageTestViewPage();

        Assert.assertEquals(getLocalStorageException(), "");
        Assert.assertEquals(getLocalStorageKey(), testKey);
        Assert.assertEquals(getLocalStorageSuccess(), "true");
        Assert.assertEquals(getLocalStorageSupported(), ""); // Not returned from write flow
        Assert.assertEquals(getLocalStorageValue(), testValue);
        Assert.assertEquals(getLocalStorageValueViaGetItem(), testValue);
        Assert.assertEquals(getLocalStorageValueViaWrapper(testKey), testValue);
        Assert.assertEquals(getLocalStorageVersion(), ""); // TODO
    }

    // TODO more tests

    // TODO test read without key or version

    // TODO test write without key or version or value

}
