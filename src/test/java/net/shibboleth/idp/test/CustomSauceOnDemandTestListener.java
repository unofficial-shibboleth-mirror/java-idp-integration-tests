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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.testng.SauceOnDemandTestListener;

/**
 * Customization of the {@link SauceOnDemandTestListener}.
 */
public class CustomSauceOnDemandTestListener extends SauceOnDemandTestListener {

    /** Class logger. */
    @Nonnull private final static Logger log = LoggerFactory.getLogger(CustomSauceOnDemandTestListener.class);

    /** Whether to print the public job link on test success. */
    @Nonnull public boolean printPublicJobLinkOnSuccess;

    /**
     * Invert the logic of the {@link SauceOnDemandTestListener} by defaulting to local rather than remote tests.
     * 
     * Set the {@link BaseIntegrationTest#SELENIUM_IS_LOCAL} system property to 'true' if
     * {@link BaseIntegrationTest#isRemote()} is false, which is the default. This will make the
     * {@link com.saucelabs.testng.SauceOnDemandTestListener} think that tests are local.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onStart(ITestContext testContext) {
        if (!BaseIntegrationTest.isRemote()) {
            log.debug("Setting system property '{}' to 'true'", BaseIntegrationTest.SELENIUM_IS_LOCAL);
            System.setProperty(BaseIntegrationTest.SELENIUM_IS_LOCAL, "true");
        }
        super.onStart(testContext);
    }

    /**
     * Print public job link if {@link #printPublicJobLinkOnSuccess} is true.
     * 
     * {@inheritDoc}
     */
    @Override
    public void onTestSuccess(ITestResult tr) {
        if (!BaseIntegrationTest.isRemote()) {
            return;
        }

        super.onTestSuccess(tr);

        if (printPublicJobLinkOnSuccess && BaseIntegrationTest.isRemote()) {
            final String publicJobLink = getPublicJobLink(tr);
            System.out.println(publicJobLink);
            Reporter.log(publicJobLink);
        }
    }

    /**
     * Get the public job link.
     * 
     * @param tr test result
     * @return the public job link or <code>null</code>.
     */
    @Nullable
    public String getPublicJobLink(ITestResult tr) {
        if (tr.getInstance() instanceof SauceOnDemandSessionIdProvider) {
            final SauceOnDemandSessionIdProvider sessionIdProvider = (SauceOnDemandSessionIdProvider) tr.getInstance();
            final SauceOnDemandAuthentication sauceOnDemandAuthentication = new SauceOnDemandAuthentication();
            final SauceREST sauceREST = new SauceREST(sauceOnDemandAuthentication.getUsername(),
                    sauceOnDemandAuthentication.getAccessKey());
            final String sessionId = sessionIdProvider.getSessionId();
            return sauceREST.getPublicJobLink(sessionId);
        } else {
            return null;
        }
    }

}
