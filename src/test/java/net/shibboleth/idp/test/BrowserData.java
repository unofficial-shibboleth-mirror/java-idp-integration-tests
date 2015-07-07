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

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Bean which represents browser/OS/version triplet. */
public class BrowserData {

    /** Browser name. */
    private String browserName;

    /** Browser version. */
    private String browserVersion;

    /** Browser operating system. */
    private String browserOS;

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
     * Set the browser name.
     * 
     * @param browser the browser name
     */
    public BrowserData setBrowser(@Nonnull final String browser) {
        browserName = Constraint.isNotNull(StringSupport.trimOrNull(browser), "Browser cannot be null nor empty");
        return this;
    }

    /**
     * Set the browser version.
     * 
     * @param version the browser version
     */
    public BrowserData setVersion(@Nonnull final String version) {
        browserVersion = Constraint.isNotNull(StringSupport.trimOrNull(version), "Version cannot be null nor empty");
        return this;
    }

    /**
     * Set the browser OS.
     * 
     * @param os the browser OS
     */
    public BrowserData setOS(@Nonnull final String os) {
        browserOS = Constraint.isNotNull(StringSupport.trimOrNull(os), "OS cannot be null nor empty");
        return this;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "BrowserData [browser=" + browserName + ", version=" + browserVersion + ", OS=" + browserOS + "]";
    }

}
