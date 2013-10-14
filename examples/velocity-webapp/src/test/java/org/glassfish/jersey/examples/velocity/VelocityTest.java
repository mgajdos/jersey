package org.glassfish.jersey.examples.velocity;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.velocity.VelocityMvcFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.glassfish.jersey.server.mvc.velocity.VelocityProperties.TEMPLATES_BASE_PATH;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul K Moore (paulkmoore at gmail.com)
 */
public class VelocityTest extends JerseyTest {
  private static String WEBAPP_NAME = "velocity-webapp";

  @Override
  protected Application configure() {
//    enable(TestProperties.LOG_TRAFFIC);
//    enable(TestProperties.DUMP_ENTITY);

    ResourceConfig config = new ResourceConfig();
    config.setApplicationName(WEBAPP_NAME);

    // Enable Velocity MVC handling
    config.register(VelocityMvcFeature.class);

    // Basic configuration - uncomment to experiment with these
    config.property(TEMPLATES_BASE_PATH, "/WEB-INF/templates/, /WEB-INF/templates/common/");

    // Register our resource(s) (Controllers)
    config.packages("org.glassfish.jersey.examples.velocity.resources"); // packages will be scanned by Jersey

    return config;
  }

  @Override
  protected URI getBaseUri() {
    return UriBuilder.fromUri(super.getBaseUri()).path(WEBAPP_NAME).build();
  }

  @Override
  public TestContainerFactory getTestContainerFactory() {
    return new GrizzlyTestContainerFactory();
  }

  @Test
  public void testApex() {
    assertTrue(target().path("").request().get(String.class).contains("Congratulations"));
  }
}
