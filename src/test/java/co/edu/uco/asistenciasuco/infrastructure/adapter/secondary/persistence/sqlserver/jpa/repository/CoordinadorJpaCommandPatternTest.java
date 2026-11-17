package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;



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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-03 — patron del command {@code usp_crear_coordinador} sobre {@code EntityManager + createNativeQuery}.
 */
class CoordinadorJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COORDINADOR = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID FACULTAD = UUID.fromString("60606060-6060-6060-6060-606060606060");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CoordinadorJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Coordinador creado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void COO_PAT_001_crear_coordinador_usa_native_query_exec_con_binding_nombrado() {
        command.crearCoordinador(COORDINADOR, "1001", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@example.test",
                PROGRAMA, FACULTAD, "Clave123!", EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("numeroIdentificacion", "1001");
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("password", "Clave123!");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void COO_PAT_002_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearCoordinador(COORDINADOR, "1001", "ANA", null, "PEREZ", null, "ana@example.test",
                        PROGRAMA, FACULTAD, "Clave123!", EJECUTOR));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void COO_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearCoordinador(COORDINADOR, "1001", "ANA", null, "PEREZ", null, "ana@example.test",
                        PROGRAMA, FACULTAD, "Clave123!", EJECUTOR));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    // CIERRE-W1B (punto 3): el listado del decano se pagina y filtra en el SP con el usuario ejecutor.
    @Test
    void COO_PAT_004_consultar_coordinadores_facultad_ejecuta_el_sp_con_filtros_y_mapea_filas_y_total() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(java.util.List.of(
                new Object[]{COORDINADOR, idUsuario, 1017555444L, "Ana Maria Perez Gomez", PROGRAMA, "Ingenieria", 1,
                        "Ana", "Maria", "Perez", "Gomez", "ana@uco.edu.co", true, "Facultad de Ingenieria", 3, 2L, 7},
                new Object[]{COORDINADOR.toString(), idUsuario.toString(), null, "Jorge Henao", null, null, 0,
                        "Jorge", null, "Henao", null, null, 0, null, null, null, 7}));

        final var pagina = command.consultarCoordinadoresFacultad(EJECUTOR, "ana", true, null, 2, 5);

        verify(entityManager).createNativeQuery(CoordinadorJpaRepository.SQL_CONSULTAR_COORDINADORES_FACULTAD);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("busqueda", "ana");
        verify(query).setParameter("estaActivo", true);
        verify(query).setParameter("sinPrograma", null);
        assertEquals(7, pagina.total());
        assertEquals(2, pagina.elementos().size());
        final var primero = pagina.elementos().getFirst();
        assertEquals(COORDINADOR, primero.id());
        assertEquals("1017555444", primero.numeroIdentificacion());
        assertEquals("Ana", primero.primerNombre());
        assertEquals("Maria", primero.segundoNombre());
        assertEquals("Gomez", primero.segundoApellido());
        assertEquals(PROGRAMA, primero.idPrograma());
        assertTrue(primero.estaActivoCoordinador());
        assertTrue(primero.estaActivoRolCoordinador());
        assertEquals("Facultad de Ingenieria", primero.nombreFacultad());
        assertEquals(3, primero.totalDocentes());
        assertEquals(2, primero.totalGrupos());
        final var segundo = pagina.elementos().getLast();
        assertEquals(null, segundo.idPrograma());
        assertFalse(segundo.estaActivoCoordinador());
        assertEquals(0, segundo.totalDocentes());
        assertEquals(0, segundo.totalGrupos());
        assertTrue(CoordinadorJpaRepository.SQL_CONSULTAR_COORDINADORES_FACULTAD
                .contains("EXEC dbo.usp_consultar_coordinadores_facultad_paginado"));
    }

    @Test
    void COO_PAT_005_consultar_coordinadores_facultad_sin_filas_devuelve_total_cero() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = command.consultarCoordinadoresFacultad(EJECUTOR, null, null, true, 1, 10);

        assertTrue(pagina.elementos().isEmpty());
        assertEquals(0, pagina.total());
        verify(query).setParameter("sinPrograma", true);
    }

    @Test
    void COO_PAT_010_perfil_sin_permiso_del_sp_se_traduce_a_403_y_fallo_tecnico_a_database_operation_error() {
        org.mockito.Mockito.doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil")))
                .when(query).getResultList();
        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException.class,
                () -> command.consultarCoordinadoresFacultad(EJECUTOR, null, null, null, 1, 10));

        org.mockito.Mockito.doThrow(new PersistenceException("deadlock")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class,
                () -> command.consultarCoordinadoresFacultad(EJECUTOR, null, null, null, 1, 10));
    }

    // HU121/HU122 (CONTRACT HU121-HU122 §3.1): comandos de edicion y estado.

    @Test
    void COO_PAT_006_actualizar_coordinador_usa_native_query_con_binding_nombrado_y_devuelve_el_usuario() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "Coordinador actualizado.", "", true, idUsuario}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.actualizarCoordinador(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertEquals(idUsuario, resultado.idUsuario());
        assertFalse(resultado.personaSinRolesActivos());
    }

    @Test
    void COO_PAT_007_cambiar_estado_usa_native_query_y_devuelve_usuario_y_persona_sin_roles_activos() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "Coordinador actualizado.", "", true, idUsuario, true}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("estaActivo", false);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertEquals(idUsuario, resultado.idUsuario());
        assertTrue(resultado.personaSinRolesActivos());
    }

    // Regresion hallada en el E2E: SQL Server entrega los uniqueidentifier como texto en consultas nativas.
    @Test
    void COO_PAT_011_el_usuario_del_coordinador_puede_llegar_como_texto() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION.toString(), "ok", "", true, idUsuario.toString(), 1}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR);

        assertEquals(idUsuario, resultado.idUsuario());
        assertTrue(resultado.personaSinRolesActivos());
    }

    @Test
    void COO_PAT_012_un_usuario_con_formato_invalido_es_violacion_de_contrato() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "ok", "", true, "no-es-un-uuid"}));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.actualizarCoordinador(COORDINADOR, "ANA", null, "PEREZ", null, "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void COO_PAT_008_estado_rechazado_por_la_db_se_traduce_a_conflicto_de_negocio() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "No se puede desactivar al coordinador mientras tenga un programa activo.",
                "DBCODE=COO_001|el coordinador coordina un programa activo", false, null, false}));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ConflictException.class,
                () -> command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR));
    }

    @Test
    void COO_PAT_009_coordinador_ajeno_o_inexistente_se_traduce_a_recurso_no_encontrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "El Coordinador especificado no existe en el sistema.",
                "DBCODE=GEN_001|Coordinador", false, null}));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException.class,
                () -> command.actualizarCoordinador(COORDINADOR, "ANA", null, "PEREZ", null, "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()));
    }
}




