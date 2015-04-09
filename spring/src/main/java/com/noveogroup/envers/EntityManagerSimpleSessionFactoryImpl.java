package com.noveogroup.envers;

import com.noveogroup.envers.api.SimpleSessionFactory;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * @author Andrey Sokolov
 */
public class EntityManagerSimpleSessionFactoryImpl implements SimpleSessionFactory {

    private EntityManagerFactory entityManagerFactory;

    @Override
    public Session getSession() {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        return entityManager.unwrap(Session.class);
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public void setEntityManagerFactory(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
}
