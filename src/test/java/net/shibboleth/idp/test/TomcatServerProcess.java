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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;
import org.testng.Assert;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

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

        // Add CATALINA_BASE to environment
        getProcessBuilder().environment().put("CATALINA_BASE", getServletContainerBasePath().toAbsolutePath().toString());

        // Name of catalina script, either ending in .sh or .bat.
        final String catalina = "catalina." + suffix;
        final Path pathToCatalina = getServletContainerHomePath().resolve(Paths.get("bin", catalina));
        Assert.assertTrue(pathToCatalina.toAbsolutePath().toFile().exists(), "Path to " + catalina + " not found");

        // Start Tomcat in current window
        getCommands().add(pathToCatalina.toAbsolutePath().toString());
        getCommands().add("run");

        // Randomize Tomcat's shutdown port
        final SortedSet<Integer> ports = SocketUtils.findAvailableTcpPorts(4, 20000, 30000);
        final Iterator<Integer> iterator = ports.iterator();
        int shutdownPort = iterator.next();
        log.info("Selecting shutdown port '{}' for Tomcat", shutdownPort);
        final Path pathToCatalinaProperties = getServletContainerBasePath().resolve(Paths.get("conf", "catalina.properties"));
        try {
            BaseIntegrationTest.replaceFile(pathToCatalinaProperties, "tomcat.shutdown.port=.*", "tomcat.shutdown.port=" + Integer.toString(shutdownPort));
        } catch (IOException e) {
            log.error("Unable to replace file", e);
            throw new ComponentInitializationException(e);
        }
    }

}
