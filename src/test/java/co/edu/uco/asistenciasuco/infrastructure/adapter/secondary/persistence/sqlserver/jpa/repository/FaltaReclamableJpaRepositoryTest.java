package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.FilaFaltaReclamable;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.PaginaFaltasReclamables;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ATA D10: adaptador JPA de {@code usp_consultar_faltas_reclamables_estudiante}. */
class FaltaReclamableJpaRepositoryTest {

    private static final UUID EG = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TOMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USUARIO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final FaltaReclamableJpaRepository repository = new FaltaReclamableJpaRepository(entityManager);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void mapea_las_catorce_columnas_y_envia_los_parametros() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 17, 13, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{EG.toString(), GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, inicio, inicio.plusHours(2), TOMA, (short) 2,
                        inicio.plusMinutes(40), LocalDateTime.of(2026, 11, 23, 5, 0), 2},
                new Object[]{EG, GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, inicio, inicio.plusHours(2), UUID.randomUUID(), 3,
                        null, null, 2}));

        final PaginaFaltasReclamables pagina = repository.consultarEstudiante(USUARIO, 2, 10);

        assertEquals(2, pagina.total());
        assertEquals(2, pagina.filas().size());
        assertEquals(new FilaFaltaReclamable(GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, Instant.parse("2026-11-17T13:00:00Z"),
                Instant.parse("2026-11-17T15:00:00Z"), TOMA, 2, Instant.parse("2026-11-17T13:40:00Z"), Instant.parse("2026-11-23T05:00:00Z")),
                pagina.filas().getFirst());
        assertEquals(3, pagina.filas().get(1).numeroToma());
        assertNull(pagina.filas().get(1).fechaPuntoControlUtc());
        verify(entityManager).createNativeQuery(FaltaReclamableJpaRepository.SQL_ESTUDIANTE);
        assertTrue(FaltaReclamableJpaRepository.SQL_ESTUDIANTE.contains("usp_consultar_faltas_reclamables_estudiante"));
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", correlacion);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 10);
    }

    @Test
    void sin_filas_devuelve_una_pagina_vacia_con_total_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final PaginaFaltasReclamables pagina = repository.consultarEstudiante(USUARIO, 1, 20);

        assertEquals(0, pagina.total());
        assertTrue(pagina.filas().isEmpty());
    }

    @Test
    void la_pagina_sin_filas_es_inmutable_aunque_se_cree_con_nulo() {
        assertTrue(new PaginaFaltasReclamables(null, 0).filas().isEmpty());
    }

    @Test
    void traduce_los_dbcode_de_perfil_y_de_pagina() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"))
                .thenThrow(new PersistenceException("DBCODE=PAG_001|pagina invalida"));

        assertThrows(ForbiddenException.class, () -> repository.consultarEstudiante(USUARIO, 1, 20));
        assertThrows(ValidationException.class, () -> repository.consultarEstudiante(USUARIO, 0, 20));
    }

    @Test
    void un_resultado_con_columnas_distintas_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.<Object>of((Object) new Object[]{EG, GRUPO}));

        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class, () -> repository.consultarEstudiante(USUARIO, 1, 20));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void el_constructor_exige_el_entity_manager() {
        assertThrows(NullPointerException.class, () -> new FaltaReclamableJpaRepository(null));
    }
}
