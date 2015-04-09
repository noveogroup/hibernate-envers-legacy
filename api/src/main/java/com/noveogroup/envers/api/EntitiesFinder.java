package com.noveogroup.envers.api;

import org.hibernate.Session;

import java.util.List;

/**
 * @author Andrey Sokolov
 */
public interface EntitiesFinder {

    List<Object> getEntities(Session session);

}
