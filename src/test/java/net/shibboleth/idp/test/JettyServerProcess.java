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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;

/**
 * Start Jetty server in a new process via start.jar. Waits for the Jetty server to start until a regex is found in the
 * process log.
 */
public class JettyServerProcess extends AbstractInitializableComponent implements Lifecycle {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JettyServerProcess.class);

    /** Path to jetty.base. */
    @Nonnull private final Path pathToJettyBase;

    /** Path to jetty.home. */
    @Nonnull private final Path pathToJettyHome;

    /** The string indicating that the Jetty server has finished starting. */
    @Nonnull public final String startedRegex = "Server:main: Started \\@";

    /** Process builder used to create the Jetty server process. */
    @NonnullAfterInit private ProcessBuilder processBuilder;

    /** Jetty server process. */
    @Nullable private Process process;

    /** Commands used to start the Jetty server process. */
    @NonnullAfterInit private List<String> commands;

    /** Whether the Jetty server process is running. */
    @Nonnull private boolean isRunning = false;

    /**
     * Constructor.
     * 
     * @param jettyBasePath path to jetty.base
     * @param jettyHomePath path to jetty.home
     */
    public JettyServerProcess(@Nonnull final Path jettyBasePath, @Nonnull final Path jettyHomePath) {
        pathToJettyBase = Constraint.isNotNull(jettyBasePath, "Path to jetty.base cannot be null");
        pathToJettyHome = Constraint.isNotNull(jettyHomePath, "Path to jetty.home cannot be null");
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {

        // Throw exception if jetty.base does not exist.
        if (!pathToJettyBase.toAbsolutePath().toFile().exists()) {
            log.error("Path to jetty.base '{}' not found", pathToJettyBase);
            throw new ComponentInitializationException("Path to jetty.base '" + pathToJettyBase + "' not found.");
        }

        // Throw exception if jetty.home does not exist.
        if (!pathToJettyHome.toAbsolutePath().toFile().exists()) {
            log.error("Path to jetty.home '{}' not found", pathToJettyHome);
            throw new ComponentInitializationException("Path to jetty.home '" + pathToJettyHome + "' not found.");
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

        // Setup commands to start the Jetty process.
        commands = new ArrayList<String>();
        commands.add("java");
        commands.add("-Didp.home=" + idpHome);
        commands.add("-jar");
        commands.add(pathToJettyHome.toAbsolutePath().toString() + "/start.jar");

        // Create the process builder.
        processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(pathToJettyBase.toAbsolutePath().toFile());
    }

    /** {@inheritDoc} */
    public void start() {
        try {
            log.debug("Starting the Jetty server process");
            process = processBuilder.start();
            waitForJettyServerToStart();
            log.debug("Jetty server process started");
        } catch (IOException e) {
            log.error("Unable to start Jetty server process", e);
            throw new RuntimeException("Unable to start Jetty server process", e);
        }
    }

    /**
     * Simple method to wait for the Jetty server process to start.
     * 
     * Wait until the {@link #startedRegex} is found in the Jetty server process output.
     * 
     * This method will block indefinitely if the {@link #startedRegex} is not found.
     * 
     * @throws IOException if an I/O error occurs reading the Jetty server process output
     */
    // TODO timeout ?
    // TODO manually set log level to at least INFO ?
    public void waitForJettyServerToStart() throws IOException {
        log.debug("Waiting for Jetty server to start ...");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final Pattern pattern = Pattern.compile(startedRegex);
        String line = "";
        while ((line = reader.readLine()) != null) {
            log.trace("Jetty log matches '{}' line '{}", pattern.matcher(line).find(), line);
            if (pattern.matcher(line).find()) {
                break;
            }
        }
        isRunning = true;
    }

    /** {@inheritDoc} */
    public void stop() {
        if (process != null) {
            process.destroy();
        }
    }

    /** {@inheritDoc} */
    public boolean isRunning() {
        return isRunning;
    }

}
