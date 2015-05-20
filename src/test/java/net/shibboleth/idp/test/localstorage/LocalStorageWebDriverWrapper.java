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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Wrap a JavaScript capable {@link WebDriver} to support retrieving key/value pairs via local storage.
 */
public class LocalStorageWebDriverWrapper {

    /** JavaScript to get an item from local storage. */
    @Nonnull public final static String GET = "return window.localStorage.getItem('%s');";

    /** JavaScript to set an item to local storage. */
    @Nonnull public final static String SET = "window.localStorage.setItem('%s','%s');";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LocalStorageWebDriverWrapper.class);

    /** JavaScript capable web driver. */
    private JavascriptExecutor javascriptExecutor;

    /**
     * Constructor.
     *
     * @param webDriver the JavaScript capable web driver.
     * @throws AssertionError if the web driver can not execute JavaScript
     */
    public LocalStorageWebDriverWrapper(@Nonnull final WebDriver webDriver) {
        Assert.assertNotNull(webDriver);
        Assert.assertTrue(webDriver instanceof JavascriptExecutor);
        javascriptExecutor = (JavascriptExecutor) webDriver;
    }

    /**
     * Get a value from local storage.
     * 
     * @param key the key
     * @return the value, possibly <code>null</code>
     */
    @Nullable public String getItem(@Nonnull final String key) {
        Assert.assertNotNull(key);
        final Object returned = executeScript(GET, key);
        return (returned == null) ? null : returned.toString();
    }

    /**
     * Store an item to local storage.
     * 
     * @param key the key
     * @param value the value
     */
    public void setItem(@Nonnull final String key, @Nonnull final String value) {
        Assert.assertNotNull(key);
        Assert.assertNotNull(value);
        executeScript(SET, key, value);
    }

    /**
     * Execute JavaScript.
     * 
     * @see {@link JavascriptExecutor}.
     * 
     * @param script the script
     * @param args script arguments
     * @return the result of executing the script
     */
    @Nullable protected Object executeScript(@Nonnull final String script, @Nonnull final Object... args) {
        Assert.assertNotNull(script);
        Assert.assertNotNull(args);
        log.trace("Executing script '{}' with args '{}'", script, args);
        final String formatted = String.format(script, args);
        log.trace("Executing script '{}'", formatted);
        final Object returned = javascriptExecutor.executeScript(formatted, args);
        log.debug("Script '{}' returned '{}'", formatted, returned);
        return returned;
    }

}
