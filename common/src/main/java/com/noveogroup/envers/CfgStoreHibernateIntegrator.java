package com.noveogroup.envers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.event.spi.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Andrey Sokolov
 */
public class CfgStoreHibernateIntegrator implements Integrator {

    private static final String ENABLED = "com.noveogroup.envers.legacy_revision_tracking.enabled";

    private static boolean enabled;

    private static AuditConfiguration enversConfiguration;

    public static boolean isEnabled() {
        return enabled;
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @SuppressWarnings("PMD.AssignmentToNonFinalStatic")
    public void integrate(
            final Configuration configuration,
            final SessionFactoryImplementor sessionFactory,
            final SessionFactoryServiceRegistry serviceRegistry) {
        enabled = ConfigurationHelper.getBoolean(ENABLED, configuration.getProperties(), false);
        if (enabled) {
            enversConfiguration = AuditConfiguration.getFor(configuration,
                    serviceRegistry.getService(ClassLoaderService.class));
        }

    }

    public void integrate(final MetadataImplementor metadata,
                          final SessionFactoryImplementor sessionFactory,
                          final SessionFactoryServiceRegistry serviceRegistry) {
    }


    public void disintegrate(final SessionFactoryImplementor sessionFactory,
                             final SessionFactoryServiceRegistry serviceRegistry) {
        if (enversConfiguration != null) {
            enversConfiguration.destroy();
        }
    }

    /**
     * Post insert listener class to use stored envers configuration.
     */
    static class CfgStoredEnversPostInsertEventListenerImpl extends EnversPostInsertEventListenerImpl {

        protected CfgStoredEnversPostInsertEventListenerImpl() {
            super(enversConfiguration);
        }
    }

    /**
     * Post collection recreate listener class to use stored envers configuration.
     */
    static class CfgStoredEnversPostCollectionRecreateEventListenerImpl
            extends EnversPostCollectionRecreateEventListenerImpl {

        protected CfgStoredEnversPostCollectionRecreateEventListenerImpl() {
            super(enversConfiguration);
        }
    }


}
