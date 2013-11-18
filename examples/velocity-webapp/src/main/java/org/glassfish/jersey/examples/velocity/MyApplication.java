/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.examples.velocity;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.velocity.VelocityMvcFeature;

import org.apache.commons.collections.ExtendedProperties;
import static org.apache.velocity.runtime.RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS;
import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.USER_PROPERTIES;

/**
 * Velocity application configuration.
 *
 * @author Paul K Moore (paulkmoore at gmail.com)
 * @since 2.5
 */
public class MyApplication extends ResourceConfig {

    public MyApplication() {
        // Enable Velocity MVC handling
        register(VelocityMvcFeature.class);

        // Basic configuration - uncomment to experiment with these
        // property(MODEL_ATTRIBUTE_NAME, "model");
        // property(TEMPLATES_BASE_PATH, "/WEB-INF/templates/, /WEB-INF/templates/common/");

        // Advanced configuration
        ExtendedProperties ve = new ExtendedProperties();
        // Velocity Engine properties
        ve.addProperty("webapp.resource.loader.path", "/WEB-INF/templates/");
        // Note: the multiple calls to set the webapp.resource.loader.path property - these are cumulative
        ve.addProperty("webapp.resource.loader.path", "/WEB-INF/templates/common/");
        // Velocity Tools properties
        ve.addProperty("tools.toolbox", "request");
        ve.addProperty("tools.request.example", "org.glassfish.jersey.examples.velocity.tools.ExampleTool");
        ve.addProperty(RUNTIME_LOG_LOGSYSTEM_CLASS, ""); // Un-configure our default Jersey logger
        property(USER_PROPERTIES, ve); // Store the properties at a known location, for Velocity initialisation

        // Register our resource(s) (Controllers)
        packages("org.glassfish.jersey.examples.velocity.resources"); // packages will be scanned by Jersey
    }
}
