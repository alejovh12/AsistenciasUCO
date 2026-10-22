package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.CambiarEstadoDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.VincularDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDocenteProgramaPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDocenteCandidatoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU091: patron JPA (native EXEC con binding nombrado) del comando y las consultas de docentes del programa. */
class DocenteProgramaJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID TIPO = UUID.fromString("60606060-6060-6060-6060-606060606060");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private DocenteProgramaJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new DocenteProgramaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new DocenteProgramaJpaRepository(null, new JpaProcedureExecutor(entityManager)));
        assertThrows(NullPointerException.class, () -> new DocenteProgramaJpaRepository(entityManager, null));
    }

    @Test
    void DP_PAT_001_vincular_usa_native_exec_con_binding_nombrado_y_correlation() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));

        repository.vincular(new VincularDocenteProgramaCommand(null, null, TIPO, 1017555444, "Jorge", null, "Henao", null,
                "jorge@uco.edu.co", "HASH", TIPO, PROGRAMA, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_vincular_docente_programa"));
        verify(query).setParameter("idDocente", null);
        verify(query).setParameter("idUsuario", null);
        verify(query).setParameter("idTipoIdentificacion", TIPO);
        verify(query).setParameter("numeroIdentificacion", 1017555444);
        verify(query).setParameter("primerNombre", "Jorge");
        verify(query).setParameter("primerApellido", "Henao");
        verify(query).setParameter("correo", "jorge@uco.edu.co");
        verify(query).setParameter("password", "HASH");
        verify(query).setParameter("idTipoVinculacion", TIPO);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void DP_PAT_002_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> repository.vincular(comando()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void DP_PAT_003_resultado_sin_forma_canonica_se_rechaza() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> repository.vincular(comando()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void DP_PAT_004_consultar_enlaza_todos_los_filtros_del_sp_paginado() {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        final UUID docente = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();

        final var pagina = repository.consultar(EJECUTOR,
                new DocentesProgramaFiltro(PROGRAMA, 2, 5, "ros", true, TIPO, docente, usuario));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_docentes_programa"),
                eq(SpDocenteProgramaPaginadoEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("busqueda", "ros");
        verify(query).setParameter("estaActivo", true);
        verify(query).setParameter("idTipoVinculacion", TIPO);
        verify(query).setParameter("idDocente", docente);
        verify(query).setParameter("idUsuario", usuario);
        assertEquals(0, pagina.total());
        assertTrue(pagina.elementos().isEmpty());
    }

    @Test
    void DP_PAT_005_consultar_candidatos_enlaza_busqueda_y_programa() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var lista = repository.consultarCandidatos(EJECUTOR, PROGRAMA, "ros");

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_candidatos_docente_programa"),
                eq(SpDocenteCandidatoEntity.class));
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("busqueda", "ros");
        assertTrue(lista.isEmpty());
    }

    @Test
    void DP_PAT_006_fallo_en_consulta_se_traduce_a_error_de_operacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarCandidatos(EJECUTOR, PROGRAMA, "ros"));
    }

    @Test
    void DP_PAT_007_actualizar_usa_native_exec_con_binding_nombrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));
        final UUID docente = UUID.randomUUID();

        repository.actualizar(new ActualizarDocenteProgramaCommand(docente, PROGRAMA, TIPO, "Nuevo", null, "Apellido", "", EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_docente_programa"));
        verify(query).setParameter("idDocente", docente);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idTipoVinculacion", TIPO);
        verify(query).setParameter("primerNombre", "Nuevo");
        verify(query).setParameter("segundoNombre", null);
        verify(query).setParameter("primerApellido", "Apellido");
        verify(query).setParameter("segundoApellido", "");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
    }

    @Test
    void DP_PAT_008_actualizar_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        assertThrows(DatabaseOperationException.class,
                () -> repository.actualizar(new ActualizarDocenteProgramaCommand(UUID.randomUUID(), PROGRAMA, null, "N", null, null, null, EJECUTOR)));
    }

    @Test
    void DP_PAT_009_cambiar_estado_usa_native_exec_con_binding_nombrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));
        final UUID docente = UUID.randomUUID();

        repository.cambiarEstado(new CambiarEstadoDocenteProgramaCommand(docente, PROGRAMA, false, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_docente_programa"));
        verify(query).setParameter("idDocente", docente);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("estaActivo", false);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
    }

    @Test
    void DP_PAT_010_cambiar_estado_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        assertThrows(DatabaseOperationException.class,
                () -> repository.cambiarEstado(new CambiarEstadoDocenteProgramaCommand(UUID.randomUUID(), PROGRAMA, true, EJECUTOR)));
    }

    private static VincularDocenteProgramaCommand comando() {
        return new VincularDocenteProgramaCommand(UUID.randomUUID(), null, null, null, null, null, null, null, null,
                null, TIPO, PROGRAMA, EJECUTOR);
    }
}
