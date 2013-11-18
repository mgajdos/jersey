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

package org.glassfish.jersey.server.mvc.velocity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.internal.DefaultTemplateProcessor;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.config.PropertiesFactoryConfiguration;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.tools.view.VelocityView;
import org.apache.velocity.tools.view.ViewToolContext;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER;
import static org.apache.velocity.runtime.RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS;
import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.MODEL_ATTRIBUTE_NAME;
import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.TEMPLATES_BASE_PATH;
import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.USER_PROPERTIES;

import com.google.common.collect.Lists;

/**
 * {@code VelocityTemplateProcessor} is a Velocity template processor.
 * <p/>
 * <a href="http://velocity.apache.org/" target="_blank">Velocity</a> is packaged as a series of
 * <a href="http://velocity.apache.org/#Apache_Velocity_projects" target="_blank">distinct projects</a>,
 * and the MVC integration here uses:
 * <ul>
 * <li><a href="http://velocity.apache.org/engine/releases/velocity-1.7/" target="_blank">Velocity Engine</a>
 * - actual templating engine which does all the work</li>
 * <li><a href="http://velocity.apache.org/tools/releases/2.0/" target="_blank">Velocity Tools</a>
 * - tools and infrastructure to integrate web (and non-web) applications using the
 * Velocity engine</li>
 * </ul>
 * <p/>
 * The main work of the VelocityTemplateProcessor is configuration and initialisation
 * of a {@link VelocityView}. The VelocityView integrates the Tools + Engine,
 * and provides servlet integration.
 * <p/>
 * Configuration<p/>
 * <b>Important:</b> you <b>MUST</b> add the following to your web.xml (or create one if required):
 * <pre>
 * {@code
 * <context-param>
 *   <param-name>org.apache.velocity.tools.view.class</param-name>
 *   <param-value>org.glassfish.jersey.server.mvc.velocity.JerseyVelocityView</param-value>
 * </context-param>}</pre>
 * In keeping with other 'jersey-mvc-*' implementations, we provide basic configuration
 * options through the use of {@link org.glassfish.jersey.server.mvc.velocity.VelocityProperties}.
 *
 * However, both Engine and Tools are highly configurable, and each supports multiple
 * configuration methods (properties, file, xml). Whilst these options remain
 * available to you, the configuration has been centralised into a single property that
 * expects an {@link ExtendedProperties} object.  This property supports Engine and
 * Tool configuration options.
 * For example, in a subclass of {@link org.glassfish.jersey.server.ResourceConfig}:
 * <pre>
 * {@code
 * import org.apache.commons.collections.ExtendedProperties;
 * import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.*;
 * ...
 * ExtendedProperties ve = new ExtendedProperties();
 * ve.addProperty("webapp.resource.loader.path", "/WEB-INF/templates/");
 * ve.addProperty("webapp.resource.loader.path", "/WEB-INF/templates/common/");
 * ve.addProperty("tools.toolbox", "request");
 * ve.addProperty("tools.request.example", "org.glassfish.jersey.examples.velocity.tools.ExampleTool");
 * property(USER_PROPERTIES, ve); // Store the properties at a known location, for Velocity initialisation}</pre>
 * Note: properties can be added multiple times where appropriate, as "webapp.resource.loader.path" (above).
 *
 * For general usage, we provide sensible defaults:
 * <ul>
 * <li>Configuration of a {@link org.apache.velocity.tools.view.WebappResourceLoader}</li>
 * <li>Template caching is switched on</li>
 * <li>Velocity logging is integrated with Java Util Logging (JUL) - as per Jersey</li>
 * </ul>
 *
 * Advanced users will require configuration of the ExtendedProperties (as above).
 *
 * @author Paul K Moore (paulkmoore at gmail.com)
 * @since 2.5
 */
@Provider
final class VelocityTemplateProcessor extends DefaultTemplateProcessor<Template> {

    private static final String DEFAULT_MODEL_ATTRIBUTE_NAME = "it"; // Default consistent with the mvc-jsp implementation

    private final VelocityView velocityView;
    private String modelAttributeName = DEFAULT_MODEL_ATTRIBUTE_NAME;

    @Inject
    private javax.inject.Provider<Ref<HttpServletRequest>> requestProviderRef;
    @Inject
    private javax.inject.Provider<Ref<HttpServletResponse>> responseProviderRef;

    /**
     * Instantiate and initialise the {@link VelocityView}.
     * <p/>
     * Here we use {@link ServletUtils#getVelocityView} to setup the Velocity Engine and Tools,
     * and provide servlet integration.
     * <p/>
     * The configuration steps are:
     * <ol>
     * <li>Setup our default properties for Jersey integration</li>
     * <li>Check for User properties, as set in the
     * {@link org.glassfish.jersey.server.mvc.velocity.VelocityProperties#USER_PROPERTIES}, and merge
     * with our default properties.  Note: User property trumps default property</li>
     * <li>Export the (merged) properties to the ServletContext for {@link JerseyVelocityView} to initialise the
     * {@link VelocityEngine}</li>
     * <li>Extract the VelocityTools configuration from the merged properties into a {@link PropertiesFactoryConfiguration}</li>
     * <li>Export the Tools configuration to the ServletContext for {@link VelocityView} to initialise the Tools</li>
     * </ol>
     *
     * @param config the {@link Configuration}
     * @param context the {@link ServletContext}
     */
    public VelocityTemplateProcessor(@Context final Configuration config, @Context ServletContext context) {
        super(config);

        // Set our ModelAttributeName
        if (config.getProperty(MODEL_ATTRIBUTE_NAME) != null) {
            modelAttributeName = (String) config.getProperty(MODEL_ATTRIBUTE_NAME);
        }

        // Setup Jersey integration configuration
        ExtendedProperties properties = new ExtendedProperties();

        // Use the WebappResourceLoader
        properties.addProperty(RESOURCE_LOADER, "webapp");
        properties.addProperty("webapp.resource.loader.description", "Jersey MVC Velocity Webapp Loader");
        properties.addProperty("webapp.resource.loader.class", "org.apache.velocity.tools.view.WebappResourceLoader");
        if (config.getProperty(TEMPLATES_BASE_PATH) != null) {
            properties.addProperty("webapp.resource.loader.path", config.getProperty(TEMPLATES_BASE_PATH));
        } else {
            // Default is the webapp root
            properties.addProperty("webapp.resource.loader.path", "");
        }

        // Use template caching
        properties.addProperty("webapp.resource.loader.cache", Boolean.TRUE.toString());
        properties.addProperty("webapp.resource.loader.modificationCheckInterval", "0");

        // Turn off automatic creation of sessions - see ViewToolManager.publishToolboxes(HttpServletRequest)
        properties.addProperty("tools.property.createSession", "false");

        // Configure logging bridge (LogChute -> JUL)
        properties.addProperty(RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.JdkLogChute");

        // Handle User's configuration
        if (config.getProperty(USER_PROPERTIES) != null) {
            // Get the Velocity properties
            ExtendedProperties userProperties = (ExtendedProperties) config.getProperty(USER_PROPERTIES);

            // Merge into the Jersey properties
            properties.combine(userProperties);  // User properties trump our standard configuration
        }

        // Export the merged properties to the ServletContext for (Jersey)VelocityView
        context.setAttribute(VelocityView.PROPERTIES_KEY, properties); // VelocityEngine properties (tool properties are ignored)
        context.setAttribute(VelocityView.TOOLS_KEY, getToolsConfiguration(properties,
                this.getClass().getName())); // VelocityTool properties, exported as a FactoryConfiguration

        // Create the VelocityView - this initialises Velocity Engine and Tools
        velocityView = ServletUtils.getVelocityView(context);
    }

    /**
     * Configures the file extension(s) supported by Velocity
     *
     * @return List of support extensions
     */
    @Override
    protected List<String> getExtensions() {
        return Lists.newArrayList(".vm");
    }

    /**
     * Override of <code>resolve</code> to take advantage of the {@link VelocityEngine#resourceExists}
     * method.
     * <p/>
     * This offers some performance advantage over (multiple) failed calls to {@link VelocityEngine#getTemplate}.
     *
     * @param name the template name.
     * @param mediaType requested media type of the template.
     * @return a {@link Template} if found, or null otherwise
     */
    @Override
    public Template resolve(String name, MediaType mediaType) {
        VelocityEngine ve = velocityView.getVelocityEngine();
        for (String templateName : getPossibleTemplateNames(name)) {
            if (ve.resourceExists(templateName)) {
                return ve.getTemplate(templateName);
            }
        }
        return null;
    }

    /**
     * Render the template (view) and viewable (model) into the outputStream.
     * <p/>
     * We use the {@link VelocityView} to generate a Velocity {@link ViewToolContext}
     * for the purposes of rendering the template.  The context ensures that any request
     * based tools (non thread safe) are instantiated (on demand) in the context, and
     * provides access to (shared) thread safe tools.
     *
     * @param template the {@link Template} to be written to the outputStream
     * @param viewable the viewable that contains the model to be passed to the template.
     * @param mediaType media type the {@code templateReference} should be transformed into.
     * @param outputStream the recipient of the merged template and viewable
     * @throws IOException if there was an error processing the template.
     * @see ViewToolContext
     * @see VelocityView
     */
    @Override
    public void writeTo(Template template, Viewable viewable, MediaType mediaType, OutputStream outputStream) throws IOException {
        // Create a (Velocity) context and map the Viewable model data into it
        ViewToolContext context = velocityView.createContext(requestProviderRef.get().get(), responseProviderRef.get().get());
        context.put(modelAttributeName, viewable.getModel());

        // Render the template into a StringWriter
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        // Append to the output stream
        outputStream.write(writer.toString().getBytes());
    }

    /**
     * Helper that renders a {@link PropertiesFactoryConfiguration} from
     * a general {@link ExtendedProperties}
     *
     * @param properties general (non-VelocityTool specific) Velocity properties.
     * @param source String based 'id' to note the source of the configuration.
     * @return a {@link PropertiesFactoryConfiguration} containing only the VelocityTools configuration.
     * @see PropertiesFactoryConfiguration
     * @see ExtendedProperties
     */
    private PropertiesFactoryConfiguration getToolsConfiguration(ExtendedProperties properties, String source) {
        PropertiesFactoryConfiguration toolsPFC = new PropertiesFactoryConfiguration(source);

        // Setup the factory from the properties
        ExtendedProperties toolsProperties = properties.subset("tools");
        if (toolsProperties != null) {
            toolsPFC.read(toolsProperties);
        }

        return toolsPFC;
    }
}

