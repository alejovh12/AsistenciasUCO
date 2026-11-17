package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QUALITY-PR15 J07: identidad de las claves compuestas de vistas. Hibernate deduplica filas por
 * esta identidad dentro del contexto de persistencia; si un componente se ignorara, dos filas
 * distintas de la vista (p.ej. el mismo decano en dos facultades) colapsarian en una sola.
 */
class ViewCompositeIdentityTest {

    private static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("30000000-0000-0000-0000-000000000003");

    static Stream<Arguments> compositeIds() {
        return Stream.of(
                id("UvDecanoEntityId(id, idFacultad)", UvDecanoEntityId::new),
                id("UvCoordinadorEntityId(id, idPrograma)", UvCoordinadorEntityId::new),
                id("UvEstudianteEntityId(id, idGrupo)", UvEstudianteEntityId::new),
                id("UvHorarioEstudianteEntityId(id, idEstudiante)", UvHorarioEstudianteEntityId::new));
    }

    @ParameterizedTest
    @MethodSource("compositeIds")
    void mismos_componentes_son_la_misma_fila(final BiFunction<UUID, UUID, Object> factory) {
        final Object key = factory.apply(FIRST, SECOND);

        assertTrue(key.equals(key), "La identidad debe ser reflexiva.");
        assertEquals(key, factory.apply(FIRST, SECOND));
        assertEquals(key.hashCode(), factory.apply(FIRST, SECOND).hashCode());
    }

    @ParameterizedTest
    @MethodSource("compositeIds")
    void cualquier_componente_distinto_es_otra_fila(final BiFunction<UUID, UUID, Object> factory) {
        final Object key = factory.apply(FIRST, SECOND);

        assertNotEquals(key, factory.apply(OTHER, SECOND));
        assertNotEquals(key, factory.apply(FIRST, OTHER));
        assertNotEquals(key, factory.apply(FIRST, null));
        assertNotEquals(key, null);
        assertNotEquals(key, FIRST);
    }

    private static Arguments id(final String name, final BiFunction<UUID, UUID, Object> factory) {
        return Arguments.of(Named.of(name, factory));
    }
}
