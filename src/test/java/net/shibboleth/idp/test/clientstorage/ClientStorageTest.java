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

package net.shibboleth.idp.test.clientstorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opensaml.storage.StorageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.test.BaseIntegrationTest;
import net.shibboleth.idp.test.BrowserData;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import storage.SimpleStorageRecordSerializer;

/**
 * Client storage tests.
 * 
 * TODO WIP
 */
public class ClientStorageTest extends BaseIntegrationTest {

    /** Default storage service ID. */
    @Nonnull public final static String DEFAULT_STORAGE_SERIVCE_ID = "shibboleth.ClientPersistentStorageService";

    /** Default cookie name / local storage key. */
    @Nonnull public final static String DEFAULT_STORAGE_NAME = "shib_idp_persistent_ss";

    /** Path of test flow to read from client storage. */
    @Nonnull public final static String TEST_READ_FLOW_PATH = "/idp/profile/test/client-storage/read";

    /** Path of test flow to write to client storage. */
    @Nonnull public final static String TEST_WRITE_FLOW_PATH = "/idp/profile/test/client-storage/write";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ClientStorageTest.class);

    /** Testbed storage serializer. */
    @NonnullAfterInit private SimpleStorageRecordSerializer serializer;

    @BeforeClass
    protected void setUp() {
        serializer = new SimpleStorageRecordSerializer();
    }

    /**
     * Initialize client storage by executing the test client-storage/read flow.
     */
    protected void executeTestClientStorageReadFlow() {
        log.trace("Execute test client storage read flow {}'", getBaseURL() + TEST_READ_FLOW_PATH);
        driver.get(getBaseURL() + TEST_READ_FLOW_PATH);
    }

    /**
     * Persist client storage by executing the test client-storage/write flow.
     */
    protected void executeTestClientStorageWriteFlow() {
        log.trace("Execute test client storage write flow {}'", getBaseURL() + TEST_WRITE_FLOW_PATH);
        driver.get(getBaseURL() + TEST_WRITE_FLOW_PATH);
    }

    /**
     * Read a storage record from a storage service by submitting the testbed form.
     * 
     * @param storageServiceId storage service ID
     * @param context storage record context
     * @param key storage record key
     */
    protected void submitReadForm(@Nonnull final String storageServiceId,
            @Nonnull final String context,
            @Nonnull final String key) {

        getAndWaitForTestbedPage();

        final WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("client-storage-read")));

        final WebElement read = driver.findElement(By.id("client-storage-read"));
        read.findElement(By.name("context")).sendKeys("context");
        read.findElement(By.name("key")).sendKeys("key");
        read.submit();
    }

    /**
     * Create a storage record in a storage service by submitting the testbed form.
     * 
     * @param storageServiceId storage service ID
     * @param context storage record context
     * @param key storage record key
     * @param value storage record value
     */
    protected void submitCreateForm(@Nonnull final String storageServiceId,
            @Nonnull final String context,
            @Nonnull final String key,
            @Nonnull final String value) {

        getAndWaitForTestbedPage();

        final WebElement read = driver.findElement(By.id("client-storage-create"));
        read.findElement(By.name("context")).sendKeys("context");
        read.findElement(By.name("key")).sendKeys("key");
        read.findElement(By.name("value")).sendKeys("value");
        read.submit();
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testRecordNotFound(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startServer();

        getAndWaitForTestbedPage();

        executeTestClientStorageReadFlow();

        submitReadForm(DEFAULT_STORAGE_SERIVCE_ID, "context", "key");

        Assert.assertEquals(getPageSource(), HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testCreate(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startServer();

        getAndWaitForTestbedPage();

        executeTestClientStorageReadFlow();

        submitCreateForm(DEFAULT_STORAGE_SERIVCE_ID, "context", "key", "value");

        Assert.assertEquals(getPageSource(), HttpStatus.CREATED.getReasonPhrase());

        Assert.assertNull(driver.manage().getCookieNamed(DEFAULT_STORAGE_NAME));

        executeTestClientStorageWriteFlow();

        Assert.assertNotNull(driver.manage().getCookieNamed(DEFAULT_STORAGE_NAME));

        submitReadForm(DEFAULT_STORAGE_SERIVCE_ID, "context", "key");

        final StorageRecord deserialized = serializer.deserialize("context", "key", getPageSource());

        Assert.assertEquals(deserialized.getValue(), "value");
        Assert.assertEquals(deserialized.getVersion(), 1);
        Assert.assertNull(deserialized.getExpiration());
    }

    // TODO more tests
}
