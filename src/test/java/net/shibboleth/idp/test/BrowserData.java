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

package net.shibboleth.idp.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Bean which represents browser/OS/version triplet as provided by
 * {@link BaseIntegrationTest#sauceOnDemandBrowserDataProvider}.
 */
public class BrowserData {

    /** Browser name. */
    private String browserName;

    /** Browser version. */
    private String browserVersion;

    /** Browser operating system. */
    private String browserOS;

    /** Browser device. */
    private String browserDevice;

    /**
     * Get the browser name.
     * 
     * @return the browser name
     */
    @Nullable public String getBrowser() {
        return browserName;
    }

    /**
     * Get the browser version.
     * 
     * @return the browser version
     */
    @Nullable public String getVersion() {
        return browserVersion;
    }

    /**
     * Get the browser OS.
     * 
     * @return the browser OS
     */
    @Nullable public String getOS() {
        return browserOS;
    }

    /**
     * Get the browser device.
     * 
     * @return the browser device
     */
    @Nullable public String getDevice() {
        return browserDevice;
    }

    /**
     * Set the browser name.
     * 
     * @param browser the browser name
     * 
     * @return the {@link BrowserData}
     */
    public BrowserData setBrowser(@Nonnull @NotEmpty final String browser) {
        browserName = Constraint.isNotNull(StringSupport.trimOrNull(browser), "Browser cannot be null nor empty");
        return this;
    }

    /**
     * Set the browser version.
     * 
     * @param version the browser version
     * 
     * @return the {@link BrowserData}
     */
    public BrowserData setVersion(@Nonnull @NotEmpty final String version) {
        browserVersion = Constraint.isNotNull(StringSupport.trimOrNull(version), "Version cannot be null nor empty");
        return this;
    }

    /**
     * Set the browser OS.
     * 
     * @param os the browser OS
     * 
     * @return the {@link BrowserData}
     */
    public BrowserData setOS(@Nonnull @NotEmpty final String os) {
        browserOS = Constraint.isNotNull(StringSupport.trimOrNull(os), "OS cannot be null nor empty");
        return this;
    }

    /**
     * Set the browser device.
     * 
     * @param device the browser device
     * 
     * @return the {@link BrowserData}
     */
    public BrowserData setDevice(@Nonnull @NotEmpty final String device) {
        browserDevice = Constraint.isNotNull(StringSupport.trimOrNull(device), "Device cannot be null nor empty");
        return this;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        final StringBuilder stringForm = new StringBuilder("BrowserData {");
        stringForm.append("platform:").append(browserOS).append(", ");
        stringForm.append("browser:").append(browserName).append(", ");
        stringForm.append("version:").append(browserVersion).append(", ");
        stringForm.append("device:").append(browserDevice);
        stringForm.append("}");
        return stringForm.toString();
    }

}
