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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Log test class and method names.
 */
public class TestNameLogger implements ITestListener {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(TestNameLogger.class);

    /** {@inheritDoc} */
    public void onTestStart(ITestResult result) {
        ITestListener.super.onTestStart(result);
        log.info("Test start {}", result.getMethod().getQualifiedName());
    }

    /** {@inheritDoc} */
    public void onTestSuccess(ITestResult result) {
        ITestListener.super.onTestSuccess(result);
        log.info("Test success {}", result.getMethod().getQualifiedName());
    }

    /** {@inheritDoc} */
    public void onTestFailure(ITestResult result) {
        ITestListener.super.onTestFailure(result);
        log.info("Test failure {}", result.getMethod().getQualifiedName());
    }

    /** {@inheritDoc} */
    public void onTestSkipped(ITestResult result) {
        ITestListener.super.onTestSkipped(result);
        log.info("Test skipped {}", result.getMethod().getQualifiedName());
    }

}
