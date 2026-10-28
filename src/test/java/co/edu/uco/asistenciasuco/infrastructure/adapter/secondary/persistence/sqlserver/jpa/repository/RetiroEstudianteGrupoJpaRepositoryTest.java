package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU051: EXEC del retiro y de la consulta de destinatarios con binding nombrado y correlacion (RP-01..RP-05). */
class RetiroEstudianteGrupoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ESTUDIANTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private RetiroEstudianteGrupoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new RetiroEstudianteGrupoJpaRepository(new JpaProcedureExecutor(entityManager), entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new RetiroEstudianteGrupoJpaRepository(null, entityManager));
        assertThrows(NullPointerException.class,
                () -> new RetiroEstudianteGrupoJpaRepository(new JpaProcedureExecutor(entityManager), null));
    }

    @Test
    void RP01_retirar_ejecuta_el_sp_con_grupo_estudiante_motivo_y_ejecutor() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));

        repository.retirarEstudiante(GRUPO, ESTUDIANTE, "Inasistencia reiterada", ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_retirar_estudiante_grupo"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("motivo", "Inasistencia reiterada");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void RP02_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.retirarEstudiante(GRUPO, ESTUDIANTE, "Motivo", null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void RP03_destinatarios_mapea_rol_usuario_contacto_y_grupo() {
        final UUID usuario = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{"ESTUDIANTE", usuario, "Ana Perez", "ana@uco.edu.co", GRUPO, 1, "Grupo 1", "Calculo"},
                new Object[]{"COORDINADOR", UUID.randomUUID(), "Coord", "coord@uco.edu.co", GRUPO, 1, "Grupo 1", "Calculo"}));

        final var destinatarios = repository.consultarDestinatarios(GRUPO, ESTUDIANTE, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_destinatarios_retiro_estudiante_grupo"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        assertEquals(2, destinatarios.size());
        final var primero = destinatarios.getFirst();
        assertEquals("ESTUDIANTE", primero.rol());
        assertEquals(usuario, primero.idUsuario());
        assertEquals("ana@uco.edu.co", primero.correo());
        assertEquals(1, primero.codigoGrupo());
        assertEquals("Calculo", primero.nombreAsignatura());
    }

    @Test
    void RP04_destinatarios_vacios_si_no_es_titular() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarDestinatarios(GRUPO, ESTUDIANTE, ACTOR).isEmpty());
    }

    @Test
    void RP05_fallo_tecnico_de_la_consulta_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarDestinatarios(GRUPO, ESTUDIANTE, ACTOR));
    }

    // Contra SQL Server real UNIQUEIDENTIFIER llega como String en Object[]: el cast a UUID fallaba y el aviso de retiro
    // se perdia en silencio (el use case lo trata como mejor esfuerzo).
    @Test
    void RP05_destinatarios_acepta_identificadores_como_texto_del_driver() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{"ESTUDIANTE", ESTUDIANTE.toString().toUpperCase(),
                "Est Prueba", "e@uco.edu.co", GRUPO.toString().toUpperCase(), 1, "Grupo 1", "Calculo"}));

        final var destinatario = repository.consultarDestinatarios(GRUPO, ESTUDIANTE, ACTOR).getFirst();

        assertEquals(ESTUDIANTE, destinatario.idUsuario());
        assertEquals(GRUPO, destinatario.idGrupo());
    }
}
