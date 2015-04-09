package com.noveogroup.envers.api;

import org.hibernate.Session;

/**
 * @author Andrey Sokolov
 */
public interface SimpleSessionFactory {
    Session getSession();
}
