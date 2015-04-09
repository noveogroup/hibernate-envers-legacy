package com.noveogroup.envers.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Andrey Sokolov
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface AuditionRoot {
}
