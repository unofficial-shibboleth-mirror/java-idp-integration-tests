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

import javax.annotation.Nullable;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Status test.
 */
public class StatusTest extends BaseIntegrationTest {

    /** Path to status page. */
    public final static String statusPath = "/idp/status";

    /** Initial text of status page . */
    public final static String STARTS_WITH = "### Operating Environment Information";

    @Test(dataProvider = "sauceOnDemandBrowserDataProvider")
    public void testStatus(@Nullable final BrowserData browserData) throws Exception {

        startSeleniumClient(browserData);

        startJettyServer();

        driver.get(baseURL + statusPath);

        Assert.assertTrue(getPageSource().startsWith(STARTS_WITH));
    }

}