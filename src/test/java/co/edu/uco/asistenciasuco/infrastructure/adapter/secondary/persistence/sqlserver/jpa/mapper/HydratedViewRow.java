package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Fixture de pruebas: construye una entidad de vista {@code Uv*Entity} como la hidrata Hibernate
 * (constructor protegido + acceso por campo, sin setters). A diferencia de un mock con accessors
 * stubbeados, detecta un accessor que devuelva la columna equivocada.
 */
public final class HydratedViewRow {

    private HydratedViewRow() {
    }

    public static <T> T of(final Class<T> entityType, final Map<String, Object> columns) {
        try {
            final Constructor<T> constructor = entityType.getDeclaredConstructor();
            constructor.setAccessible(true);
            final T entity = constructor.newInstance();
            for (final Map.Entry<String, Object> column : columns.entrySet()) {
                final Field field = entityType.getDeclaredField(column.getKey());
                field.setAccessible(true);
                field.set(entity, column.getValue());
            }
            return entity;
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Fixture invalido para " + entityType.getSimpleName(), exception);
        }
    }
}
