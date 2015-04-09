package com.noveogroup.envers;

import com.noveogroup.envers.api.SimpleSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author Andrey Sokolov
 */
public class SpringContextListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringContextListener.class);

    private boolean initialized;

    private SimpleSessionFactory simpleSessionFactory;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (CfgStoreHibernateIntegrator.isEnabled() && !initialized) {
            try {
                LOGGER.info("Initializing untracked revisions");
                new RevisionDataStartUpInitializer(simpleSessionFactory,
                        new AuditionRootEntitiesFinder()).initRevisionsData();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            initialized = true;
        }
    }

    public SimpleSessionFactory getSimpleSessionFactory() {
        return simpleSessionFactory;
    }

    public void setSimpleSessionFactory(final SimpleSessionFactory simpleSessionFactory) {
        this.simpleSessionFactory = simpleSessionFactory;
    }
}
