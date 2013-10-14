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

package org.glassfish.jersey.server.mvc.velocity.internal;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.VelocityView;

import javax.servlet.ServletContext;

/**
 * {@code JerseyVelocityView} extends {@link VelocityView} to enable {@link VelocityEngine} properties
 * to be discovered from a known ServletContext attribute.
 *
 * @author Paul K Moore (paulkmoore at gamil.com)
 * @since 2.3.2
 */
public class JerseyVelocityView extends VelocityView {

  public JerseyVelocityView(JeeConfig config) {
    super(config);
  }

  /**
   * Configure the VelocityEngine from the {@link JeeConfig}.
   * <p/>
   * We extend the functionality of
   * {@link VelocityView#configure(org.apache.velocity.tools.view.JeeConfig, org.apache.velocity.app.VelocityEngine)}
   * so that the {@link VelocityView#PROPERTIES_KEY} is also used to look for an {@link ExtendedProperties}
   * object in the ServletContext.
   * Note: this parallels the configuration of the VelocityTools using the {@link VelocityView#TOOLS_KEY}.
   *
   * @param config the {@link JeeConfig}, effectively a wrapper for the ServletContext
   * @param velocity the (uninitialised) {@link VelocityEngine}
   */
  // TODO raise a JIRA for this to be added to VelocityTools as there is currently no method to initialise VE with an ExtendedProperties - we could then retire JerseyVelocityView
  @Override
  protected void configure(final JeeConfig config, final VelocityEngine velocity) {
    super.configure(config, velocity);

    // Check for user props stored in the ServletContext
    setProps(velocity, servletContext, PROPERTIES_KEY, false);
  }

  /**
   * Helper to set the VelocityEngine properties from an attribute key in the ServletContext.
   * @param velocityEngine the {@link VelocityEngine}
   * @param servletContext the {@link ServletContext}
   * @param key the attribute key used to lookup the {@link ExtendedProperties} from the ServletContext
   * @param require whether the configuration is required to succeed
   * @return <code>true</code> if the configuration succeeds, otherwise <code>false</code>
   */
  private boolean setProps(VelocityEngine velocityEngine, ServletContext servletContext, String key, boolean require) {
    if (velocityEngine == null || servletContext == null | key == null) {
      return false;
    }

    debug("Configuring Velocity with extended properties from: %s", key);

    ExtendedProperties properties = (ExtendedProperties)servletContext.getAttribute(key);

    // these props will override those already set
    velocity.setExtendedProperties(properties);
    // notify that new props were set
    return true;
  }

  /**
   * Direct copy of the {@link VelocityView#setProps(org.apache.velocity.app.VelocityEngine, String, boolean)}
   * method.
   * <p/>
   * Annoyingly we have to reproduce it here, as it's declared <code>private</code> and therefore
   * can't be called from a subclass.
   */
  // TODO raise a JIRA for this to be addressed in VelocityTools
  private boolean setProps(VelocityEngine velocity, String path, boolean require) {
    if (path == null) {
      // only bother with this if a path was given
      return false;
    }

    // this will throw an exception if require is true and there
    // are no properties at the path.  if require is false, this
    // will return null when there's no properties at the path
    ExtendedProperties props = getProperties(path, require);
    if (props == null) {
      return false;
    }

    debug("Configuring Velocity with properties at: %s", path);

    // these props will override those already set
    velocity.setExtendedProperties(props);
    // notify that new props were set
    return true;
  }
}
