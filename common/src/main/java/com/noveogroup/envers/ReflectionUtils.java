package com.noveogroup.envers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Andrey Sokolov
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
        throw new UnsupportedOperationException("Instantiate util class");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(final String fieldName, final Object object) {
        final Class<? extends Object> clazz = object.getClass();
        try {
            final Field field = getField(clazz, fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            throw handleReflectionException(ex, clazz, fieldName);
        }
    }

    public static Field getField(final Class<?> clazz, final String name) {
        Class<?> currentClazz = clazz;
        while (!Object.class.equals(currentClazz) && currentClazz != null) {
            final Field[] fields = currentClazz.getDeclaredFields();
            for (Field field : fields) {
                if (name == null || name.equals(field.getName())) {
                    return field;
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }
        throw new IllegalStateException("The " + clazz.getSimpleName()
                + " class does not have any " + name + " field");
    }

    private static IllegalStateException handleReflectionException(final Exception ex,
                                                                   final Class<? extends Object> clazz,
                                                                   final String fieldName) {
        return new IllegalStateException("Unexpected reflection exception while getting "
                + fieldName + " field of class " + clazz.getSimpleName() + ": " + ex.getMessage(),
                ex);

    }

    /**
     * Generate the getter name of a collection property.
     *
     * @param propertyName name of the collection property (ie. clients)
     * @return name of the corresponding getter (ie. getClients)
     */
    // CHECKSTYLE.OFF: MagicNumber
    public static String getterFromCollection(final String propertyName) {
        return new StringBuilder(propertyName.length() + 3)
                .append("get")
                .append(Character.toTitleCase(propertyName.charAt(0)))
                .append(propertyName.substring(1)).toString();
    }
    // CHECKSTYLE.ON: MagicNumber

    /**
     * Calls the getter of a collection property in order to resolve the javassist lazy proxy
     * object.
     *
     * @param entity       target object
     * @param propertyName name of the collection property (ie. clients)
     * @return the collection
     */
    public static Object callCollectionGetter(final Object entity, final String propertyName) {
        try {
            final Method getter = entity.getClass().getMethod(getterFromCollection(propertyName));
            getter.setAccessible(true);
            return getter.invoke(entity);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return new RuntimeException(e);
        }
    }
}
