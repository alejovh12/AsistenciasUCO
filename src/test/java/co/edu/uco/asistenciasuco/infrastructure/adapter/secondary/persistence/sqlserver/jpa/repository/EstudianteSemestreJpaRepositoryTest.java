package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarSemestreEstudianteCommand;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Migracion 63: usp_actualizar_semestre_estudiante_programa por JPA nativo con binding nombrado. */
class EstudianteSemestreJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID ESTUDIANTE = UUID.fromString("60606060-6060-6060-6060-606060606060");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private EstudianteSemestreJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new EstudianteSemestreJpaRepository(new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private ActualizarSemestreEstudianteCommand comando(final Integer semestre) {
        return new ActualizarSemestreEstudianteCommand(ESTUDIANTE, PROGRAMA, semestre, EJECUTOR);
    }

    @Test
    void constructor_rechaza_dependencia_nula() {
        assertThrows(NullPointerException.class, () -> new EstudianteSemestreJpaRepository(null));
    }

    @Test
    void ejecuta_el_sp_con_binding_nombrado_y_devuelve_el_mensaje_de_la_db() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Semestre actualizado.", "DBCODE=EST_007|", true}));

        final String mensaje = repository.actualizarSemestre(comando(3));

        assertEquals("Semestre actualizado.", mensaje);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_semestre_estudiante_programa"));
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("semestreActual", 3);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
    }

    @Test
    void semestre_nulo_se_envia_nulo_para_limpiar() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "DBCODE=EST_007|", true}));

        repository.actualizarSemestre(comando(null));

        verify(query).setParameter("semestreActual", null);
    }

    @Test
    void sec_002_es_prohibido() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "No autorizado", "DBCODE=SEC_002|titularidad", false}));
        final var cmd = comando(2);
        assertThrows(ForbiddenException.class, () -> repository.actualizarSemestre(cmd));
    }

    @Test
    void est_008_es_no_encontrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "No vinculado", "DBCODE=EST_008|sin vinculo", false}));
        final var cmd = comando(2);
        assertThrows(ResourceNotFoundException.class, () -> repository.actualizarSemestre(cmd));
    }

    @Test
    void est_009_es_validacion_con_el_texto_de_la_db() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "El semestre debe estar entre 1 y 10.", "DBCODE=EST_009|rango", false}));
        final var cmd = comando(11);
        final ValidationException e = assertThrows(ValidationException.class, () -> repository.actualizarSemestre(cmd));
        assertEquals("El semestre debe estar entre 1 y 10.", e.getMessage());
    }

    @Test
    void fallo_tecnico_se_encapsula() {
        when(query.getResultList()).thenThrow(new PersistenceException("caida"));
        final var cmd = comando(2);
        assertThrows(DatabaseOperationException.class, () -> repository.actualizarSemestre(cmd));
    }
}
