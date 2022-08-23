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

package net.shibboleth.idp.integration.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Start Tomcat via 'catalina.sh run'. */
public class TomcatServerProcess extends AbstractServerProcess {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TomcatServerProcess.class);

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        // Windows or not ?
        final String suffix = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "bat" : "sh";

        // Name of setenv script, either ending in .sh or .bat.
        final String setenv = "setenv." + suffix;

        // Append system properties to bin/setenv.sh
        final Path pathToSetenvSh = getServletContainerBasePath().resolve(Paths.get("bin", setenv));
        Assert.assertTrue(pathToSetenvSh.toAbsolutePath().toFile().exists(), "Path to " + setenv + " not found");
        for (final String serverCommand : getAdditionalCommands()) {
            if (serverCommand.startsWith("-D")) {
                try {
                    BaseIntegrationTest.replaceFile(pathToSetenvSh, "\"$", " " + Matcher.quoteReplacement(serverCommand) + "\"");
                } catch (IOException e) {
                    log.error("Unable to replace file", e);
                    throw new ComponentInitializationException(e);
                }
            }
        }

        // Add CATALINA_HOME to environment
        getProcessBuilder().environment().put("CATALINA_HOME", getServletContainerHomePath().toAbsolutePath().toString());
        // Add CATALINA_BASE to environment
        getProcessBuilder().environment().put("CATALINA_BASE", getServletContainerBasePath().toAbsolutePath().toString());

        // Name of startup script, either ending in .sh or .bat.
        final String startup = "startup." + suffix;
        final Path pathToStartup = getServletContainerHomePath().resolve(Paths.get("bin", startup));
        Assert.assertTrue(pathToStartup.toAbsolutePath().toFile().exists(), "Path to " + startup + " not found");

        // Start Tomcat in current window
        getCommands().add(pathToStartup.toAbsolutePath().toString());

        // Configure Tomcat's shutdown port
        setUpShutdownPort();
    }

    /**
     * Configure port to shutdown Tomcat.
     * 
     * @throws ComponentInitializationException if catalina.properties cannot be modified
     */
    @Nonnull
    public void setUpShutdownPort() throws ComponentInitializationException {

        final int shutdownPort = getShutdownPort();
        log.debug("Selecting Tomcat shutdown port {}", shutdownPort);

        final Path pathToCatalinaProp = getServletContainerBasePath().resolve(Paths.get("conf", "catalina.properties"));
        try {
            BaseIntegrationTest.replaceFile(pathToCatalinaProp, "tomcat.shutdown.port=.*",
                    "tomcat.shutdown.port=" + Integer.toString(shutdownPort));
        } catch (IOException e) {
            log.error("Unable to replace file '{}'", pathToCatalinaProp, e);
            throw new ComponentInitializationException(e);
        }
    }

    /**
     * Attempt to shutdown Tomcat via telnet.
     * 
     * Telnet to the shutdown port and send 'SHUTDOWN'.
     * 
     * @param hostname the name of the remote host
     * @param port the port to connect to on the remote host
     * @param password The shutdown password
     */
    public void shutdown(@Nonnull final String hostname, final int port, @Nonnull final String password) {
        Constraint.isNotNull(hostname, "Hostname cannot be null");
        Constraint.isNotNull(password, "Password cannot be null");

        log.debug("Attempting to shutdown Tomcat at '{}:{}'", hostname, port);

        final TelnetClient telnet = new TelnetClient();
        try {
            telnet.connect(hostname, port);
            telnet.getOutputStream().write(password.getBytes());
            telnet.getOutputStream().flush();
            telnet.disconnect();
        } catch (IOException e) {
            log.error("A Telnet I/O error occurred", e);
        }

        // wait half a second for server to shutdown
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error("Unable to sleep", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        shutdown("127.0.0.1", getShutdownPort(), "SHUTDOWN");
        super.stop();
    }

}
