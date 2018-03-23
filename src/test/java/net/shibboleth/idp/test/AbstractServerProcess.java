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

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;

import com.google.common.base.Stopwatch;

import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Start IdP server in a new {@link Process}.
 * <p>
 * Waits for the IdP status page to be available.
 */
public class AbstractServerProcess extends AbstractInitializableComponent implements Lifecycle {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractServerProcess.class);

    /** Path to Servlet container base. */
    @Nonnull private Path pathToContainerBase;

    /** Path to Servlet container home. */
    @Nonnull private Path pathToContainerHome;

    /** URL of the IdP status page. */
    @NonnullAfterInit private String statusPageURL;

    /** Process builder used to create the server process. */
    @NonnullAfterInit private ProcessBuilder processBuilder;

    /** Server process. */
    @Nullable private Process process;

    /** Commands used to start the server process. */
    @NonnullAfterInit private List<String> commands;

    /** Additional commands used to start the server process. */
    @Nullable private List<String> additionalCommands;

    /** Whether the server process is running. */
    @Nonnull private boolean isRunning = false;

    /**
     * Build the commands used to create the server process. Appends additional commands.
     * 
     * @return commands used to create the server process
     */
    public List<String> buildCommands() {
        final List<String> commands = getCommands();
        final List<String> additionalCommands = getAdditionalCommands();
        if (additionalCommands != null && !additionalCommands.isEmpty()) {
            log.debug("Additional commands '{}'", additionalCommands);
            commands.addAll(additionalCommands);
        }
        return commands;
    }

    /**
     * Build the process builder used to create the server process.
     * 
     * @return the process builder used to create the server process
     */
    public ProcessBuilder buildProcessBuilder() {
        final ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        if (Boolean.getBoolean("inheritIO")) {
            builder.inheritIO();
        }
        builder.directory(pathToContainerBase.toAbsolutePath().toFile());
        return builder;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (pathToContainerBase == null) {
            throw new ComponentInitializationException("Path to Servlet container base cannot be null");
        }

        if (pathToContainerHome == null) {
            throw new ComponentInitializationException("Path to Servlet container home cannot be null");
        }

        if (statusPageURL == null) {
            throw new ComponentInitializationException("Status page URL cannot be null");
        }

        // Throw exception if idp.home is not defined.
        final String idpHome = System.getProperty("idp.home");
        if (idpHome == null) {
            log.error("System property 'idp.home' is not defined.");
            throw new ComponentInitializationException("System property 'idp.home' is not defined.");
        }

        // Throw exception if path to idp.home does not exist.
        if (!Paths.get(idpHome).toAbsolutePath().toFile().exists()) {
            log.error("Path to idp.home '{}' not found", idpHome);
            throw new ComponentInitializationException("Path to idp.home '" + idpHome + "' not found.");
        }

        commands = new ArrayList<>();

        processBuilder = buildProcessBuilder();
    }

    /**
     * Get additional commands used to create the server process. These commands are appended to those returned by
     * {@link #getCommands()},
     * 
     * @return additional commands used to create the server process
     */
    @Nullable
    @Live
    public List<String> getAdditionalCommands() {
        return additionalCommands;
    }

    /**
     * Get the commands used to create the server process.
     * 
     * @return commands used to create the server process
     */
    @NonnullAfterInit
    @Live
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Get the process builder used to create the server process.
     * 
     * @return the process builder
     */
    @NonnullAfterInit
    public ProcessBuilder getProcessBuilder() {
        return processBuilder;
    }

    /**
     * Get path to Servlet container base.
     * 
     * @return path to Servlet container base
     */
    public Path getServletContainerBasePath() {
        return pathToContainerBase;
    }

    /**
     * Get path to Servlet container home.
     * 
     * @return path to Servlet container home
     */
    public Path getServletContainerHomePath() {
        return pathToContainerHome;
    }

    /**
     * Set additional commands to start the server process.
     * 
     * @param commands additional commands
     * @return this server
     */
    @Nonnull
    public AbstractServerProcess setAdditionalCommands(@Nullable final List<String> commands) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (commands != null) {
            additionalCommands = commands;
        }
        return this;
    }

    /**
     * Set path to Servlet container base.
     * 
     * @param containerBasePath path to Servlet container base
     * @return this server
     */
    @Nonnull
    public AbstractServerProcess setServletContainerBasePath(@Nonnull final Path containerBasePath) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        pathToContainerBase = Constraint.isNotNull(containerBasePath, "Path to Servlet container base cannot be null");
        return this;
    }

    /**
     * Set path to Servlet container home.
     * 
     * @param containerHomePath path to Servlet container home
     * @return this server
     */
    @Nonnull
    public AbstractServerProcess setServletContainerHomePath(@Nonnull final Path containerHomePath) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        pathToContainerHome = Constraint.isNotNull(containerHomePath, "Path to Servlet container home cannot be null");
        return this;
    }

    /**
     * Set status page URL.
     * 
     * @param URL status page URL
     * @return this server
     */
    @Nonnull
    public AbstractServerProcess setStatusPageURL(@Nonnull @NotEmpty final String URL) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        statusPageURL = Constraint.isNotNull(StringSupport.trimOrNull(URL), "Status page URL cannot be null nor empty");
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        try {
            processBuilder.command(buildCommands());
            log.debug("Will start server using command '{}'", processBuilder.command());
            final Stopwatch stopwatch = Stopwatch.createStarted();
            log.debug("Starting the server process");
            process = processBuilder.start();
            waitForStatusPage();
            stopwatch.stop();
            log.debug("Server process started in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.error("Unable to start server process", e);
            throw new RuntimeException("Unable to start server process", e);
        }
    }

    /**
     * Wait up to 60 seconds for the IdP status page, trying every half-second.
     * 
     * @throws RuntimeException if the actual status page text is not expected
     * @throws Exception if an error occurs
     */
    public void waitForStatusPage() throws Exception {
        log.debug("Waiting for server to start ...");

        final String statusPageText = getStatusPageText(120, 500);

        if (!statusPageText.startsWith(StatusTest.STARTS_WITH)) {
            log.error("Unable to determine if server has started.");
            throw new RuntimeException("Unable to determine if server has started.");
        }
    }

    /**
     * Get the text of the IdP status page.
     * 
     * Retries must be greater than 1 to enable the retry handlers.
     * 
     * @param retries maximum number of times to retry
     * @param millis length of time to sleep in milliseconds between retry attempts
     * @return the text of the IdP status page or <code>null</code>
     * @throws Exception if an error occurs
     */
    @Nullable
    public String getStatusPageText(@Nonnull final int retries, @Nonnull final int millis) throws Exception {

        final HttpClientBuilder builder = new HttpClientBuilder();
        if (retries > 1) {
            builder.setHttpRequestRetryHandler(new FiniteWaitHttpRequestRetryHandler(retries / 2, millis));
            builder.setServiceUnavailableRetryHandler(new FiniteWaitServiceUnavailableRetryStrategy(retries / 2, millis));
        }
        builder.setConnectionCloseAfterResponse(false);
        builder.setConnectionDisregardTLSCertificate(true);
        final HttpClient httpClient = builder.buildClient();

        final HttpGet httpget = new HttpGet(statusPageURL);
        final HttpResponse response = httpClient.execute(httpget);
        log.trace("Status page response  '{}'", response);

        try {
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                long len = entity.getContentLength();
                if (len != -1 && len < 2048) {
                    final String statusPageText = EntityUtils.toString(entity);
                    log.trace("Status page text '{}'", statusPageText);
                    return statusPageText;
                }
            }
        } finally {
            if (response instanceof CloseableHttpResponse) {
                ((CloseableHttpResponse) response).close();
            }
            if (httpClient instanceof CloseableHttpClient) {
                ((CloseableHttpClient) httpClient).close();
            }
        }

        return null;
    }

    /**
     * A {@link HttpRequestRetryHandler} which retries requests until a maximum number of attempts has been made and
     * which sleeps between retry attempts.
     */
    public class FiniteWaitHttpRequestRetryHandler implements HttpRequestRetryHandler {

        /** Maximum number of retry attempts. */
        private final int maxRetries;

        /** Length of time to sleep in milliseconds between retry attempts. */
        private final int sleepMillis;

        /**
         * Constructor.
         *
         * @param retries maximum number of times to retry
         * @param millis length of time to sleep in milliseconds between retry attempts
         */
        public FiniteWaitHttpRequestRetryHandler(@Nullable final int retries, @Nonnull final int millis) {
            maxRetries = retries;
            sleepMillis = millis;
        }

        /** {@inheritDoc} */
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            log.trace("Request retry handler exception msg '{}'", exception.getMessage());
            log.trace("Request retry handler execution count '{}'", executionCount);

            if (sleepMillis > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (executionCount <= maxRetries) {
                return true;
            }

            return false;
        }
    }

    /**
     * A {@link ServiceUnavailableRetryStrategy} which retries requests until a maximum number of attempts has been made
     * and which sleeps between retry attempts. This strategy retries response status codes of
     * {@link HttpStatus#SC_SERVICE_UNAVAILABLE} and {@link HttpStatus#SC_NOT_FOUND}.
     */
    public class FiniteWaitServiceUnavailableRetryStrategy implements ServiceUnavailableRetryStrategy {

        /** Maximum number of retry attempts. */
        private final int maxRetries;

        /** Length of time to sleep in milliseconds between retry attempts. */
        private final int sleepMillis;

        /**
         * Constructor.
         *
         * @param retries maximum number of times to retry
         * @param millis length of time to sleep in milliseconds between retry attempts
         */
        public FiniteWaitServiceUnavailableRetryStrategy(int retries, int retryInterval) {
            maxRetries = retries;
            sleepMillis = retryInterval;
        }

        @Override
        public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
            log.trace("Service unavailable retry strategy response '{}'", response);
            log.trace("Service unavailable retry strategy execution count '{}'", executionCount);
            return executionCount <= maxRetries
                    && (response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
                            || response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND);
        }

        /** {@inheritDoc} */
        public long getRetryInterval() {
            return sleepMillis;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        waitForServerToStop(10, 500); // wait 5s for server to stop
        if (process != null) {
            log.trace("Stopping process");
            process.destroy();
            try {
                log.trace("Waiting for process to exit");
                process.waitFor();
                log.trace("Done waiting");
            } catch (InterruptedException e) {
                throw new RuntimeException("Unable to wait for server process", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Wait for server to stop.
     * 
     * If server status page is available, wait until it is not available.
     * 
     * @param retries maximum number of times to retry
     * @param millis length of time to sleep in milliseconds between retry attempts
     */
    public void waitForServerToStop(@Nonnull final int retries, @Nonnull final int millis) {
        try {
            log.debug("Waiting for server to stop ...");
            if (getStatusPageText(1, 0) == null) {
                log.debug("Server appears to be stopped.");
                return;
            }
            int executionCount = 0;
            while (executionCount < retries) {
                log.debug("Server still running, waiting '{}'ms for server to stop ...", millis);
                Thread.sleep(millis);
                if (getStatusPageText(1, 0) == null) { // is server up ?
                    log.debug("Server appears to be stopped.");
                    return;
                }
            }
            log.warn("Server did not stop.");
        } catch (final ConnectException e) {
            if (e.getMessage().endsWith("Connection refused (Connection refused)")) {
                log.debug("Server appears to be stopped.");
            } else {
                log.warn("Server might be stopped", e);
            }
        } catch (final Exception e) {
            log.warn("An error occurred waiting for server to stop", e);
        }
    }

}
