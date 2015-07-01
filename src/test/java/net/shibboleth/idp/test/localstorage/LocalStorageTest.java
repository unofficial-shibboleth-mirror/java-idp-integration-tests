/*
, "1" * Licensed to the University Corporation for Advanced Internet Development, 
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
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.URLBuilder;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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

    /** Title of local storage test view. */
    public final static String LOCAL_STORAGE_TEST_VIEW_TITLE = "Local Storage Test View";

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

    /** Delimiter used to separate the version and value of a versioned item. */
    @Nonnull public final static String LOCAL_STORAGE_VALUE_DELIMITER = ":";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LocalStorageTest.class);

    /** Test local storage item key. */
    @Nonnull protected String testKey = "shib_idp_ls_test_key";

    /** Test local storage item value. */
    @Nonnull protected String testValue = "shib_idp_ls_test_key_value";
    
    /** Path to test flow to read from local storage. */
    @Nonnull protected String readPath = "/idp/profile/test/local-storage/read";

    /** Path to test flow to write to local storage. */
    @Nonnull protected String writePath = "/idp/profile/test/local-storage/write";

    /** JavaScript capable web driver wrapper. */
    @Nonnull protected LocalStorageWebDriverWrapper wrapper;

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
     * Wait for page with title {@link #LOCAL_STORAGE_TEST_VIEW_TITLE}.
     */
    protected void waitForLocalStorageTestView() {
        (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().equals(LOCAL_STORAGE_TEST_VIEW_TITLE);
            }
        });
        log.debug("{} source:\n'{}'", LOCAL_STORAGE_TEST_VIEW_TITLE, driver.getPageSource());
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
     * The read flow requires the following query parameters :
     * <ul>
     * <li>{@link #LOCAL_STORAGE_KEY_ID}</li>
     * <li>{@link #LOCAL_STORAGE_VERSION_ID}</li>
     * </ul>
     * 
     * @param localStorageKey the key of the item to read from local storage.
     * @param localStorageVersion the version of the item to read from local storage.
     * @return the URL to read from local storage using the test flow
     * @throws MalformedURLException
     */
    protected String buildReadURL(@Nullable final String localStorageKey, @Nullable final String localStorageVersion)
            throws MalformedURLException {
        final URLBuilder urlBuilder = new URLBuilder(getBaseURL());
        urlBuilder.setPath(readPath);
        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        if (localStorageKey != null) {
            queryParams.add(new Pair<>(LOCAL_STORAGE_KEY_ID, localStorageKey));
        }
        if (localStorageVersion != null) {
            queryParams.add(new Pair<String, String>(LOCAL_STORAGE_VERSION_ID, localStorageVersion));
        }
        final String URL = urlBuilder.buildURL();
        log.debug("URL to read from local storage is '{}'", URL);
        return URL;
    }

    /**
     * Build the URL to write to local storage using the test flow.
     * 
     * The write flow requires the following query parameters :
     * <ul>
     * <li>{@link #LOCAL_STORAGE_KEY_ID}</li>
     * <li>{@link #LOCAL_STORAGE_VALUE_ID}</li>
     * <li>{@link #LOCAL_STORAGE_VERSION_ID}</li>
     * </ul>
     * 
     * @param localStorageKey the key of the item to be written to local storage.
     * @param localStorageValue the value of the item to be written to local storage.
     * @return the URL to write to local storage using the test flow
     * @throws MalformedURLException
     */
    protected String buildWriteURL(@Nullable final String localStorageKey, @Nullable final String localStorageValue,
            @Nullable final String localStorageVersion) throws MalformedURLException {
        final URLBuilder urlBuilder = new URLBuilder(getBaseURL());
        urlBuilder.setPath(writePath);
        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        if (localStorageKey != null) {
            queryParams.add(new Pair<>(LOCAL_STORAGE_KEY_ID, localStorageKey));
        }
        if (localStorageValue != null) {
            queryParams.add(new Pair<>(LOCAL_STORAGE_VALUE_ID, localStorageValue));
        }
        if (localStorageVersion != null) {
            queryParams.add(new Pair<String, String>(LOCAL_STORAGE_VERSION_ID, localStorageVersion));
        }
        final String URL = urlBuilder.buildURL();
        log.debug("URL to write to local storage is '{}'", URL);
        return URL;
    }

    /**
     * Assert that reading from local storage was successful.
     * 
     * Must be called after {@link #waitForLocalStorageTestView()}.
     * 
     * @param itemKey the local storage item key
     * @param itemValue the expected local storage item value
     * @param version the expected local storage item version
     */
    protected void assertSuccessfulRead(@Nullable final String itemKey, @Nullable final String itemValue,
            @Nullable final String version) {
        Assert.assertEquals(getLocalStorageException(), "");
        Assert.assertEquals(getLocalStorageKey(), itemKey);
        Assert.assertEquals(getLocalStorageSuccess(), "true");
        Assert.assertEquals(getLocalStorageSupported(), "true");
        Assert.assertEquals(getLocalStorageVersion(), version);
        Assert.assertEquals(getLocalStorageValue(), itemValue);
    }

    /**
     * Assert that writing to local storage was successful.
     * 
     * Must be called after {@link #waitForLocalStorageTestView()}.
     * 
     * @param itemKey the local storage item key
     * @param itemValue the expected local storage item value
     * @param version the expected local storage item version
     */
    protected void assertSuccessfulWrite(@Nullable final String itemKey, @Nullable final String itemValue,
            @Nullable final String version) {
        Assert.assertEquals(getLocalStorageException(), "");
        Assert.assertEquals(getLocalStorageKey(), itemKey);
        Assert.assertEquals(getLocalStorageSuccess(), "true");
        Assert.assertEquals(getLocalStorageSupported(), ""); // Not returned from write flow
        Assert.assertEquals(getLocalStorageValue(), itemValue);
        Assert.assertEquals(getLocalStorageVersion(), version);

        final String versionedValue = version + LOCAL_STORAGE_VALUE_DELIMITER + itemValue;
        Assert.assertEquals(getLocalStorageValueViaGetItem(), versionedValue);
        Assert.assertEquals(getLocalStorageValueViaWrapper(itemKey), versionedValue);
    }

    /**
     * Write to local storage and assert that the write was successful via the test view.
     * 
     * @param itemKey the local storage item key
     * @param itemValue the local storage item value
     * @param version the item version
     * @throws MalformedURLException
     */
    protected void writeAndAssert(@Nullable final String itemKey, @Nullable final String itemValue,
            @Nullable final String version) throws MalformedURLException {

        driver.get(buildWriteURL(itemKey, itemValue, version));

        waitForLocalStorageTestView();

        assertSuccessfulWrite(itemKey, itemValue, version);
    }

    /**
     * Construct the versioned value written to local storage as a concatenation of the version, delimiter, and value.
     * 
     * @param value the local storage item value
     * @param version the item version
     * @return the versioned value as a concatenation of the version, delimiter, and value
     */
    @Nonnull protected String versionedValue(@Nullable final String value, @Nullable final String version) {
        return version + LOCAL_STORAGE_VALUE_DELIMITER + value;
    }

    @Test public void testNothingToRead() throws Exception {

        startJettyServer();

        driver.get(buildReadURL(testKey, "0"));

        waitForLocalStorageTestView();

        assertSuccessfulRead(testKey, "", "");

        Assert.assertEquals(getLocalStorageValueViaGetItem(), "null");
        Assert.assertNull(getLocalStorageValueViaWrapper(testKey));
        Assert.assertNull(getLocalStorageValueViaWrapper("shib_idp_ls_version"));
    }

    @Test public void testReadEarlierVersion() throws Exception {

        startJettyServer();

        writeAndAssert(testKey, testValue, "1");

        driver.get(buildReadURL(testKey, "0"));

        waitForLocalStorageTestView();

        assertSuccessfulRead(testKey, testValue, "1");
    }

    @Test public void testReadSameVersion() throws Exception {

        startJettyServer();

        writeAndAssert(testKey, testValue, "1");

        driver.get(buildReadURL(testKey, "1"));

        waitForLocalStorageTestView();

        assertSuccessfulRead(testKey, "", "1");
    }

    @Test public void testReadLaterVersion() throws Exception {

        startJettyServer();

        writeAndAssert(testKey, testValue, "1");

        driver.get(buildReadURL(testKey, "2"));

        waitForLocalStorageTestView();

        assertSuccessfulRead(testKey, "", "1");
    }

    @Test public void testReadWrapper() throws Exception {

        startJettyServer();

        // Wrapper needs a page to execute JavaScript.
        driver.get(buildReadURL(testKey, "0"));

        // Set item directly, not using the write URL
        wrapper.setItem(testKey, versionedValue(testValue, "1"));

        driver.get(buildReadURL(testKey, "0"));

        waitForLocalStorageTestView();

        assertSuccessfulRead(testKey, testValue, "1");
    }

    @Test public void testReadInvalidVersion() throws Exception {

        startJettyServer();

        driver.get(buildReadURL(testKey, "NaN"));

        waitForLocalStorageTestView();

        Assert.assertEquals(getLocalStorageException(), "Version [NaN] must be a number");
        Assert.assertEquals(getLocalStorageKey(), testKey);
        Assert.assertEquals(getLocalStorageSuccess(), "false");
        Assert.assertEquals(getLocalStorageSupported(), "true");
        Assert.assertEquals(getLocalStorageVersion(), "");
        Assert.assertEquals(getLocalStorageValue(), "");
    }

    @Test public void testWrite() throws Exception {

        startJettyServer();

        writeAndAssert(testKey, testValue, "1");
    }

    @Test public void testWriteInvalidVersion() throws Exception {

        startJettyServer();

        driver.get(buildWriteURL(testKey, testValue, "NaN"));

        waitForLocalStorageTestView();

        Assert.assertEquals(getLocalStorageException(), "Version [NaN] must be a number");
        Assert.assertEquals(getLocalStorageKey(), testKey);
        Assert.assertEquals(getLocalStorageSuccess(), "false");
        Assert.assertEquals(getLocalStorageSupported(), ""); // Not returned from write flow
        Assert.assertEquals(getLocalStorageVersion(), "NaN");
        Assert.assertEquals(getLocalStorageValue(), testValue);
    }

    /**
     * Return a string of a given size.
     * 
     * @param c a character to compose the string with
     * @param size the size of the string
     * @return a string of the given size consisting of the given character
     */
    @Nonnull protected static String generateString(char c, int size) {
        final char[] chars = new char[size];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    @Test(enabled = false) public void testWriteQuotaExceeded() throws ComponentInitializationException,
            MalformedURLException {

        // Set Firefox's quota to 1 kilobyte.
        // profile.setPreference("dom.storage.default_quota", "1");

        startJettyServer();

        final String value = LocalStorageTest.generateString('x', 2048);

        driver.get(buildWriteURL(testKey, value, "1"));

        waitForLocalStorageTestView();

        Assert.assertEquals(getLocalStorageKey(), testKey);
        Assert.assertEquals(getLocalStorageSuccess(), "false");
        Assert.assertFalse(getLocalStorageException().isEmpty());
    }

    // TODO more tests

    // TODO test read without key or version

    // TODO test write without key or version or value

}
