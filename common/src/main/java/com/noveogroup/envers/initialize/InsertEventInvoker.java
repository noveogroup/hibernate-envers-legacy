package com.noveogroup.envers.initialize;

import com.noveogroup.envers.ReflectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;

import java.io.Serializable;
import java.util.List;

/**
 * @author Andrey Sokolov
 */
public class InsertEventInvoker {


    private final EnversPostInsertEventListenerImpl insertListener;
    private final AuditReader auditReader;
    private final EventSource source;

    public InsertEventInvoker(final EnversPostInsertEventListenerImpl insertListener,
                              final AuditReader auditReader,
                              final EventSource source) {
        this.insertListener = insertListener;
        this.auditReader = auditReader;
        this.source = source;
    }

    public boolean invokeInsertListener(final SessionFactory sessionFactory, final Object entity) {
        final EntityPersister persister = source.getEntityPersister(null, entity);
        final ClassMetadata metadata = sessionFactory.getClassMetadata(persister.getEntityName());
        final Serializable id = ReflectionUtils.getValue(metadata.getIdentifierPropertyName(), entity);
        final List<Number> revisions = auditReader.getRevisions(entity.getClass(), persister.getEntityName(), id);
        if (revisions == null || revisions.size() == 0) {
            final Object[] state = persister.getPropertyValuesToInsert(entity, null, source);
            final PostInsertEvent event = new PostInsertEvent(entity, id, state, persister, source);
            insertListener.onPostInsert(event);
            return true;
        }
        return false;
    }
}
