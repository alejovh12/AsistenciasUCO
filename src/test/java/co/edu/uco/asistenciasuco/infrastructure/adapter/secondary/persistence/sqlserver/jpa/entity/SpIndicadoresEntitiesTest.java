package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las filas de SP exponen sus columnas sin transformarlas: una fila vacia devuelve nulos, nunca valores por defecto. */
class SpIndicadoresEntitiesTest {

    @Test
    void las_filas_de_sp_no_inventan_valores_por_defecto() throws Exception {
        final List<Class<?>> tipos = List.of(SpKpisCoordinadorEntity.class, SpAlertaTempranaEntity.class,
                SpBitacoraSesionEntity.class, SpFichaEstudianteEntity.class, SpResumenAsignaturaEntity.class);
        for (final Class<?> tipo : tipos) {
            final var constructor = tipo.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Object fila = constructor.newInstance();
            for (final Method m : tipo.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()) {
                    assertNull(m.invoke(fila), tipo.getSimpleName() + "." + m.getName());
                }
            }
        }
    }
}
