package com.noveogroup.envers;

import com.noveogroup.envers.api.EntitiesFinder;
import com.noveogroup.envers.api.SimpleSessionFactory;
import com.noveogroup.envers.initialize.CollectionEventInvoker;
import com.noveogroup.envers.initialize.Initializer;
import com.noveogroup.envers.initialize.InsertEventInvoker;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.event.spi.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.event.spi.EventSource;

import java.util.List;

/**
 * @author Andrey Sokolov
 */
public class RevisionDataStartUpInitializer {

    private final SimpleSessionFactory simpleSessionFactory;
    private final EntitiesFinder entitiesFinder;
    private final EnversPostInsertEventListenerImpl insertListener;
    private final EnversPostCollectionRecreateEventListenerImpl collectionListener;

    private AuditReader auditReader;
    private EventSource source;

    public RevisionDataStartUpInitializer(final SimpleSessionFactory simpleSessionFactory,
                                          final EntitiesFinder entitiesFinder) {
        this.simpleSessionFactory = simpleSessionFactory;
        this.entitiesFinder = entitiesFinder;
        insertListener = new CfgStoreHibernateIntegrator.CfgStoredEnversPostInsertEventListenerImpl();
        collectionListener = new CfgStoreHibernateIntegrator.CfgStoredEnversPostCollectionRecreateEventListenerImpl();
    }

    public void initRevisionsData() throws IllegalAccessException {
        final Session session = simpleSessionFactory.getSession();
        auditReader = AuditReaderFactory.get(session);
        source = (EventSource) session;
        final Transaction tx = source.beginTransaction();
        final List<Object> projectEntities = entitiesFinder.getEntities(session);
        final CollectionEventInvoker collectionEventInvoker = new CollectionEventInvoker(collectionListener, source);
        final InsertEventInvoker insertEventInvoker = new InsertEventInvoker(insertListener, auditReader, source);
        final Initializer initializer = new Initializer(collectionEventInvoker, insertEventInvoker);
        for (Object entity : projectEntities) {
            initializer.deepInit(session, entity);
        }
        tx.commit();
        session.close();
    }

}
