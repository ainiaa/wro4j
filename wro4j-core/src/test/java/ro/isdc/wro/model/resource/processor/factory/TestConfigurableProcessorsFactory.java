/**
 * Copyright@2011 wro4j
 */
package ro.isdc.wro.model.resource.processor.factory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.model.resource.processor.ResourceProcessor;
import ro.isdc.wro.model.resource.processor.decorator.ExtensionsAwareProcessorDecorator;


/**
 * @author Alex Objelean
 */
public class TestConfigurableProcessorsFactory {
  private ConfigurableProcessorsFactory victim;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    victim = new ConfigurableProcessorsFactory();
  }
  
  @Test
  public void shouldReturnEmptyListOfProcessors() {
    assertEquals(Collections.EMPTY_LIST, victim.getPreProcessors());
    assertEquals(Collections.EMPTY_LIST, victim.getPostProcessors());
  }
  
  @Test(expected = WroRuntimeException.class)
  public void testInvalidPreProcessorSet() {
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, "invalid");
    victim.setProperties(props);
    victim.getPreProcessors();
  }
  
  @Test(expected = WroRuntimeException.class)
  public void testInvalidPostProcessorSet() {
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, "invalid");
    victim.setProperties(props);
    victim.getPostProcessors();
  }
  
  @Test
  public void testGetValidPreProcessorSet() {
    final Map<String, ResourceProcessor> map = new HashMap<String, ResourceProcessor>();
    map.put("valid", Mockito.mock(ResourceProcessor.class));
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS, "valid");
    victim.setPreProcessorsMap(map);
    victim.setProperties(props);
    assertEquals(1, victim.getPreProcessors().size());
  }
  
  @Test
  public void testGetValidPostProcessorSet() {
    final Map<String, ResourceProcessor> map = new HashMap<String, ResourceProcessor>();
    map.put("valid", Mockito.mock(ResourceProcessor.class));
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS, "valid");
    victim.setPostProcessorsMap(map);
    victim.setProperties(props);
    assertEquals(1, victim.getPostProcessors().size());
  }
  
  @Test
  public void cannotAcceptExtensionAwareConfigurationForPostProcessors() {
    final Map<String, ResourceProcessor> map = new HashMap<String, ResourceProcessor>();
    final String extension = "js";
    final String processorName = "valid";
    map.put(processorName, Mockito.mock(ResourceProcessor.class));
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_POST_PROCESSORS,
        String.format("%s.%s", processorName, extension));
    victim.setPreProcessorsMap(map);
    victim.setProperties(props);
    assertEquals(0, victim.getPreProcessors().size());
  }

  @Test
  public void shouldDecorateWithExtensionAwareProcessorDecorator() {
    genericShouldDecorateWithExtension("valid", "js");
  }
  
  @Test
  public void shouldDecorateWithExtensionAwareProcessorDecoratorWhenProcessorNameContainsDots() {
    genericShouldDecorateWithExtension("valid.processor.name", "js");
  }
  
  private void genericShouldDecorateWithExtension(final String processorName, final String extension) {
    final Map<String, ResourceProcessor> map = new HashMap<String, ResourceProcessor>();
    map.put(processorName, Mockito.mock(ResourceProcessor.class));
    final Properties props = new Properties();
    props.setProperty(ConfigurableProcessorsFactory.PARAM_PRE_PROCESSORS,
        String.format("%s.%s", processorName, extension));
    victim.setPreProcessorsMap(map);
    victim.setProperties(props);
    assertEquals(1, victim.getPreProcessors().size());
    assertTrue(victim.getPreProcessors().iterator().next() instanceof ExtensionsAwareProcessorDecorator);
  }
}
