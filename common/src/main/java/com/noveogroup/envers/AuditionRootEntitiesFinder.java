package com.noveogroup.envers;

import com.noveogroup.envers.api.AuditionRoot;
import com.noveogroup.envers.api.EntitiesFinder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Andrey Sokolov
 */
public class AuditionRootEntitiesFinder implements EntitiesFinder {


    public List<Object> getEntities(final Session session) {
        final List<Object> entities = new ArrayList<>();
        final Map<String, ClassMetadata> allMetadata = session.getSessionFactory().getAllClassMetadata();
        for (final ClassMetadata metadata : allMetadata.values()) {
            final Class<?> javaType = metadata.getMappedClass();
            if (javaType.isAnnotationPresent(AuditionRoot.class)) {
                final Criteria criteria = session.createCriteria(metadata.getEntityName());
                @SuppressWarnings("rawtypes")
                final List list = criteria.list();
                for (Object entity : list) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }
}
