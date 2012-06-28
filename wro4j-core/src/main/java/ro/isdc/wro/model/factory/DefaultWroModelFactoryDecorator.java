package ro.isdc.wro.model.factory;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.manager.callback.LifecycleCallbackRegistry;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.group.processor.Injector;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.support.ResourceAuthorizationManager;
import ro.isdc.wro.util.AbstractDecorator;
import ro.isdc.wro.util.DestroyableLazyInitializer;
import ro.isdc.wro.util.ObjectDecorator;
import ro.isdc.wro.util.StopWatch;
import ro.isdc.wro.util.Transformer;


/**
 * Decorates the model factory with callback registry calls & other useful factories. Another responsibility of this
 * decorator is make model creation thread safe.
 * <p/>
 * This class doesn't extend {@link AbstractDecorator} because we have to enhance the decorated object with new
 * decorators.
 * 
 * @author Alex Objelean
 * @created 13 Mar 2011
 * @since 1.4.6
 */
public class DefaultWroModelFactoryDecorator
    implements WroModelFactory, ObjectDecorator<WroModelFactory> {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultWroModelFactoryDecorator.class);

  private final WroModelFactory decorated;
  @Inject
  private LifecycleCallbackRegistry callbackRegistry;
  @Inject
  private ResourceAuthorizationManager authorizationManager;
  @Inject
  private WroConfiguration config;
  @Inject
  private Injector injector;
  /**
   * Responsible for model caching
   */
  private final DestroyableLazyInitializer<WroModel> modelInitializer = new DestroyableLazyInitializer<WroModel>() {
    @Override
    protected WroModel initialize() {
      callbackRegistry.onBeforeModelCreated();
      final StopWatch watch = new StopWatch("Create Model");
      watch.start("createModel");
      WroModel model = null;
      try {
        final WroModelFactory modelFactory = decorate(decorated);
        injector.inject(modelFactory);
        model = modelFactory.create();
        return model;
      } finally {
        authorizeModelResources(model);
        callbackRegistry.onAfterModelCreated();
        watch.stop();
        LOG.debug(watch.prettyPrint());
      }
    }
    
    /**
     * Decorate with several useful aspects, like: fallback, caching & model transformer ability.
     */
    private WroModelFactory decorate(final WroModelFactory decorated) {
      return new ModelTransformerFactory(new FallbackAwareWroModelFactory(decorated)).setTransformers(modelTransformers);
    }
    
    /**
     * Authorizes all resources of the model to be accessed as proxy resources (only in dev mode).
     * 
     * @param model
     *          {@link WroModel} created by decorated factory.
     */
    private void authorizeModelResources(final WroModel model) {
      if (model != null && config.isDebug()) {
        for (Resource resource : model.getAllResources()) {
          authorizationManager.add(resource.getUri());
        }
      }
    }
  };

  private final List<Transformer<WroModel>> modelTransformers;
  
  public DefaultWroModelFactoryDecorator(final WroModelFactory decorated,
      final List<Transformer<WroModel>> modelTransformers) {
    this.modelTransformers = modelTransformers;
    this.decorated = decorated;
    Validate.notNull(modelTransformers);
  }
  
  /**
   * {@inheritDoc}
   */
  public WroModel create() {
    return modelInitializer.get();
  }
  
  /**
   * {@inheritDoc}
   */
  public void destroy() {
    LOG.debug("Destroy model");
    modelInitializer.destroy();
    getDecoratedObject().destroy();
    authorizationManager.clear();
  }
  
  /**
   * {@inheritDoc}
   */
  public WroModelFactory getDecoratedObject() {
    return this.decorated;
  }
  

  /**
   * {@inheritDoc}
   */
  public WroModelFactory getOriginalDecoratedObject() {
    return AbstractDecorator.getOriginalDecoratedObject(getDecoratedObject());
  }
}
