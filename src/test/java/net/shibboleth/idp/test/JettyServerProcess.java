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

import java.nio.file.Path;
import java.nio.file.Paths;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Start Jetty via start.jar. */
public class JettyServerProcess extends AbstractServerProcess {

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        // Add JETTY_BASE to environment
        getProcessBuilder().environment().put("JETTY_BASE", getServletContainerBasePath().toAbsolutePath().toString());

        // Start Jetty via start.jar
        final Path pathToJava = Paths.get(System.getProperty("java.home"), "bin", "java");
        getCommands().add(pathToJava.toAbsolutePath().toString());
        getCommands().add("-Didp.home=" + System.getProperty("idp.home"));
        getCommands().add("-jar");
        getCommands().add(getServletContainerHomePath().toAbsolutePath().toString() + "/start.jar");
    }

}
