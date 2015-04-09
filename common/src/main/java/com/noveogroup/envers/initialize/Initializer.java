package com.noveogroup.envers.initialize;

import com.noveogroup.envers.ReflectionUtils;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity initializer.
 * <p/>
 * Idea and main procedures of this code "stealed" from project com.javaetmoi.core:javaetmoi-hibernate4-hydrate
 *
 * @author Andrey Sokolov
 * @see <a href="https://github.com/arey/hibernate-hydrate">https://github.com/arey/hibernate-hydrate</a>
 */
public class Initializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Initializer.class);
    private final CollectionEventInvoker collectionEventInvoker;
    private final InsertEventInvoker insertEventInvoker;

    public Initializer(final CollectionEventInvoker collectionEventInvoker,
                       final InsertEventInvoker insertEventInvoker) {
        this.collectionEventInvoker = collectionEventInvoker;
        this.insertEventInvoker = insertEventInvoker;
    }

    public <E> E deepInit(final Session currentSession, final E entity) {
        deepInitEntity(currentSession, entity, new HashSet<String>());
        return entity;
    }


    @SuppressWarnings("unchecked")
    private void deepInitEntity(final Session currentSession,
                                final Object entity,
                                final Set<String> recursiveGuard) {
        if (entity == null) {
            return;
        }

        Class<? extends Object> persistentClass = entity.getClass();
        if (entity instanceof HibernateProxy) {
            persistentClass = ((HibernateProxy) entity).getHibernateLazyInitializer().getPersistentClass();
        }
        final ClassMetadata classMetadata = currentSession.getSessionFactory().getClassMetadata(
                persistentClass);
        if (classMetadata == null) {
            return;
        }
        final Serializable identifier = classMetadata.getIdentifier(entity,
                (SessionImplementor) currentSession);
        final String key = persistentClass.getName() + "|" + identifier;

        if (recursiveGuard.contains(key)) {
            return;
        }
        recursiveGuard.add(key);

        if (insertEventInvoker.invokeInsertListener(currentSession.getSessionFactory(), entity)) {
            final int n = classMetadata.getPropertyNames().length;
            for (int i = 0; i < n; i++) {
                final String propertyName = classMetadata.getPropertyNames()[i];
                final Field field = ReflectionUtils.getField(classMetadata.getMappedClass(), propertyName);
                final Audited annotation = field.getAnnotation(Audited.class);
                if (annotation != null) {
                    final RelationTargetAuditMode relationTargetAuditMode = annotation.targetAuditMode();
                    final Type propertyType = classMetadata.getPropertyType(propertyName);
                    Object propertyValue = null;
                    if (entity instanceof javassist.util.proxy.ProxyObject) {
                        // For javassist proxy, the classMetadata.getPropertyValue(..) method return en
                        // empty collection. So we have to call the property's getter in order to call the
                        // JavassistLazyInitializer.invoke(..) method that will initialize the collection by
                        // loading it from the database.
                        propertyValue = ReflectionUtils.callCollectionGetter(entity, propertyName);
                    } else {
                        propertyValue = classMetadata.getPropertyValue(entity, propertyName);
                    }
                    deepInitProperty(propertyValue, propertyType,
                            relationTargetAuditMode, currentSession, recursiveGuard);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void deepInitProperty(final Object propertyValue,
                                  final Type propertyType,
                                  final RelationTargetAuditMode relationTargetAuditMode,
                                  final Session currentSession,
                                  final Set<String> recursiveGuard) {
        if (propertyValue == null) {
            return;
        }

        if (propertyType.isEntityType() && RelationTargetAuditMode.AUDITED.equals(relationTargetAuditMode)) {
            deepInitEntity(currentSession, propertyValue, recursiveGuard);
        } else if (propertyType.isCollectionType()) {
            // Handle PersistentBag, PersistentList and PersistentIndentifierBag
            if (propertyValue instanceof List) {
                deepInitCollection(currentSession, recursiveGuard, (List) propertyValue, relationTargetAuditMode);
            } else if (propertyValue instanceof Map) {
                LOGGER.debug("Map property initializtion not supported");
            } else if (propertyValue instanceof Set) {
                deepInitCollection(currentSession, recursiveGuard, (Set) propertyValue, relationTargetAuditMode);
            } else {
                throw new UnsupportedOperationException("Unsupported collection type: "
                        + propertyValue.getClass().getSimpleName());
            }
        } else if (propertyType.isComponentType() && propertyType instanceof ComponentType) {
            // i.e. @Embeddable annotation (see https://github.com/arey/hibernate-hydrate/issues/1)
            deepInitComponent(currentSession, propertyValue, (ComponentType) propertyType,
                    relationTargetAuditMode, recursiveGuard);
        }
    }

    private void deepInitComponent(final Session currentSession,
                                   final Object componentValue,
                                   final ComponentType componentType,
                                   final RelationTargetAuditMode relationTargetAuditMode,
                                   final Set<String> recursiveGuard) {
        // No public API to access to the component Hibernate metamodel => force to use
        // introspection instead
        final String[] propertyNames = ReflectionUtils.getValue("propertyNames", componentType);
        final Type[] propertyTypes = ReflectionUtils.getValue("propertyTypes", componentType);

        for (int i = 0; i < propertyNames.length; i++) {
            final String propertyName = propertyNames[i];
            final Type propertyType = propertyTypes[i];
            final Object propertyValue = ReflectionUtils.getValue(propertyName, componentValue);
            deepInitProperty(propertyValue, propertyType, relationTargetAuditMode, currentSession, recursiveGuard);
        }

    }


    private void deepInitCollection(final Session currentSession,
                                    final Set<String> recursiveGuard,
                                    @SuppressWarnings("rawtypes") final Collection collection,
                                    final RelationTargetAuditMode relationTargetAuditMode) {
        if (collection != null && collection.size() > 0) {
            ComponentType collectionType = null;
            if (collection instanceof PersistentCollection && !((PersistentCollection) collection).isUnreferenced()) {
                collectionEventInvoker.invokeCollectionListener((PersistentCollection) collection);
                // The isUnreferenced() test is useful for some persistent bags that does not have any role
                final String role = ((PersistentCollection) collection).getRole();
                final Type type = currentSession.getSessionFactory().getCollectionMetadata(role).getElementType();
                if (type instanceof ComponentType) {
                    // ManyToMany relationship with @Embeddable annotation (see
                    // https://github.com/arey/hibernate-hydrate/issues/3)
                    collectionType = (ComponentType) type;
                }
            }

            if (RelationTargetAuditMode.AUDITED.equals(relationTargetAuditMode)) {
                processCollection(currentSession, recursiveGuard, collection, relationTargetAuditMode, collectionType);
            }
        }
    }

    private void processCollection(final Session currentSession,
                                   final Set<String> recursiveGuard,
                                   final Collection collection,
                                   final RelationTargetAuditMode relationTargetAuditMode,
                                   final ComponentType collectionType) {
        for (Object item : collection) {
            if (item != null) {
                if (collectionType != null) {
                    deepInitComponent(currentSession, item, collectionType,
                            relationTargetAuditMode, recursiveGuard);
                } else {
                    deepInitEntity(currentSession, item, recursiveGuard);
                }
            }
        }
    }
}
