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

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** Start Jetty via start.jar. */
public class JettyServerProcess extends AbstractServerProcess {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(JettyServerProcess.class);

    /** Port to use to shutdown Jetty. Defaults to 8005. */
    @Nonnull private int shutdownPort = 8005;

    /** Passphrase to use to shutdown Jetty. Defaults to SHUTDOWN. */
    @Nonnull private String shutdownKey = "SHUTDOWN";

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        // Add JETTY_BASE to environment
        getProcessBuilder().environment().put("JETTY_BASE", getServletContainerBasePath().toAbsolutePath().toString());

        // Start Jetty via start.jar
        // Prefer Java located at idp.java.home system property
        final Path pathToJava = Paths.get(System.getProperty("idp.java.home", System.getProperty("java.home")), "bin", "java");
        log.debug("Will use Java located at '{}'", pathToJava);
        getCommands().add(pathToJava.toAbsolutePath().toString());
        getCommands().add("-jar");
        getCommands().add(getServletContainerHomePath().toAbsolutePath().toString() + "/start.jar");
        getCommands().add("STOP.KEY=" + shutdownKey);
        nextAvailableShutdownPort();
        getCommands().add("STOP.PORT=" + Integer.toString(shutdownPort));
    }

    /**
     * Set Jetty shutdown passphrase.
     * 
     * Passed to start.jar as STOP.KEY.
     * 
     * @param key key to use to shutdown Jetty
     */
    @Nonnull
    public void setShutdownKey(@Nonnull @NotEmpty final String key) {
        shutdownKey = key;
    }

    /**
     * Configure the next available port in the range 20000-30000 to shutdown Jetty.
     * 
     * @return the next available port to use to shutdown Tomcat, by default '8005' if the '8080' system property is
     *         <code>true</code>
     * @throws ComponentInitializationException if catalina.properties cannot be modified
     */
    @Nonnull
    public int nextAvailableShutdownPort() throws ComponentInitializationException {
        if (Boolean.getBoolean("8080")) {
            log.debug("System property '8080' is true, using default shutdown port {}", shutdownPort);
            return shutdownPort;
        }
        shutdownPort = SocketUtils.findAvailableTcpPort(20000, 30000);
        log.debug("Selecting Jetty shutdown port {}", shutdownPort);
        return shutdownPort;
    }

    /**
     * Attempt to shutdown Jetty.
     * 
     * Send the stop command to the STOP.PORT.
     * 
     * @param hostname the name of the remote host
     * @param port the port to connect to on the remote host
     * @param password the shutdown password
     */
    public void shutdown(@Nonnull final String hostname, final int port, @Nonnull final String password) {
        Constraint.isNotNull(hostname, "Hostname cannot be null");
        Constraint.isNotNull(password, "Password cannot be null");

        log.debug("Attempting to shutdown Jetty at '{}:{}'", hostname, port);

        final int timeout = 5;

        try {
            try (final Socket s = new Socket(InetAddress.getByName(hostname), port)) {
                s.setSoTimeout(timeout * 1000);
                try (OutputStream out = s.getOutputStream()) {
                    out.write((password + "\r\nstop\r\n").getBytes());
                    out.flush();
                    log.debug("Waiting {} seconds for jetty to stop", timeout);
                    final LineNumberReader lin = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                    String response;
                    while ((response = lin.readLine()) != null) {
                       log.debug("Received '{}'", response);
                       if ("Stopped".equals(response)) {
                            log.debug("Server reports itself as Stopped");
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            log.warn("Timed out waiting for stop confirmation");
        } catch (ConnectException e) {
            log.warn("Server might be stopped", e);
        } catch (Exception e) {
            log.warn("An error occurred waiting for server to stop", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        shutdown("127.0.0.1", shutdownPort, shutdownKey);
        super.stop();
    }
}
