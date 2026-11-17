package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort.CrearCoordinadorConProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.CrearDecanoConFacultadCommand;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEC-065 (5): patron de los comandos compuestos {@code usp_crear_decano_con_facultad} y
 * {@code usp_crear_coordinador_con_programa} sobre {@code EntityManager + createNativeQuery}: parametros nombrados, lectura de las
 * tres columnas extra (uniqueidentifier llega como texto) y rechazo de un contrato roto.
 */
class AltaCompuestaJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DECANO = UUID.fromString("70707070-7070-7070-7070-707070707070");
    private static final UUID COORDINADOR = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID FACULTAD = UUID.fromString("60606060-6060-6060-6060-606060606060");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID INSTITUCION = UUID.fromString("80808080-8080-8080-8080-808080808080");
    private static final UUID TIPO_PROGRAMA = UUID.fromString("81818181-8181-8181-8181-818181818181");
    private static final UUID MODALIDAD = UUID.fromString("82828282-8282-8282-8282-828282828282");
    private static final UUID JORNADA = UUID.fromString("83838383-8383-8383-8383-838383838383");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_ID = UUID.fromString("12121212-1212-1212-1212-121212121212");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private DecanoJpaRepository decanos;
    private CoordinadorJpaRepository coordinadores;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        final var executor = new JpaProcedureExecutor(entityManager);
        decanos = new DecanoJpaRepository(entityManager, executor);
        coordinadores = new CoordinadorJpaRepository(entityManager, executor);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private void devuelve(final Object... fila) {
        when(query.getResultList()).thenReturn(Collections.singletonList(fila));
    }

    // ---- decano con facultad

    @Test
    void decano_con_facultad_ejecuta_el_sp_compuesto_y_lee_las_columnas_extra_como_texto() {
        devuelve(CORRELATION, "Decano creado.", "", true, DECANO.toString().toUpperCase(), FACULTAD, "Facultad de Ingenieria");

        final var resultado = decanos.crearDecanoConFacultad(comandoDecano());

        assertEquals(DECANO, resultado.idDecano());
        assertEquals(FACULTAD, resultado.idFacultad());
        assertEquals("Facultad de Ingenieria", resultado.nombreFacultad());
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_decano_con_facultad"));
        verify(query).setParameter("idDecano", DECANO);
        verify(query).setParameter("numeroIdentificacion", 7);
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", null);
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", null);
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("password", "HASH");
        verify(query).setParameter("nombreFacultad", "Facultad de Ingenieria");
        verify(query).setParameter("codigoFacultad", "ING");
        verify(query).setParameter("idInstitucion", INSTITUCION);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idTipoIdIdentificacion", TIPO_ID);
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void decano_con_facultad_sin_id_en_la_fila_es_contrato_roto() {
        devuelve(CORRELATION, "ok", "", true, null, FACULTAD, "F");

        final var exception = assertThrows(DatabaseOperationException.class, () -> decanos.crearDecanoConFacultad(comandoDecano()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void decano_con_facultad_con_id_mal_formado_es_contrato_roto() {
        devuelve(CORRELATION, "ok", "", true, DECANO, "no-es-uuid", "F");

        final var exception = assertThrows(DatabaseOperationException.class, () -> decanos.crearDecanoConFacultad(comandoDecano()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void decano_con_facultad_traduce_el_fallo_tecnico() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final var exception = assertThrows(DatabaseOperationException.class, () -> decanos.crearDecanoConFacultad(comandoDecano()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
    }

    @Test
    void decano_con_facultad_duplicada_es_conflicto() {
        devuelve(CORRELATION, "Ya existe una facultad con ese nombre.", "DBCODE=GEN_003|duplicada", false, null, null, null);

        assertThrows(ConflictException.class, () -> decanos.crearDecanoConFacultad(comandoDecano()));
    }

    @Test
    void decano_con_facultad_y_institucion_inexistente_es_404() {
        devuelve(CORRELATION, "La institucion no existe.", "DBCODE=GEN_001|institucion", false, null, null, null);

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException.class,
                () -> decanos.crearDecanoConFacultad(comandoDecano()));
    }

    // ---- coordinador con programa

    @Test
    void coordinador_con_programa_ejecuta_el_sp_compuesto_y_lee_las_columnas_extra() {
        devuelve(CORRELATION, "Coordinador creado.", "", true, COORDINADOR, PROGRAMA.toString(), "Ingenieria de Sistemas");

        final var resultado = coordinadores.crearCoordinadorConPrograma(comandoCoordinador());

        assertEquals(COORDINADOR, resultado.idCoordinador());
        assertEquals(PROGRAMA, resultado.idPrograma());
        assertEquals("Ingenieria de Sistemas", resultado.nombrePrograma());
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_coordinador_con_programa"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("numeroIdentificacion", "1001");
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", null);
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", null);
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("password", "HASH");
        verify(query).setParameter("nombrePrograma", "Ingenieria de Sistemas");
        verify(query).setParameter("idTipoPrograma", TIPO_PROGRAMA);
        verify(query).setParameter("idModalidad", MODALIDAD);
        verify(query).setParameter("idJornada", JORNADA);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idTipoIdIdentificacion", TIPO_ID);
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void coordinador_con_programa_sin_id_en_la_fila_es_contrato_roto() {
        devuelve(CORRELATION, "ok", "", true, COORDINADOR, null, "P");

        final var exception = assertThrows(DatabaseOperationException.class,
                () -> coordinadores.crearCoordinadorConPrograma(comandoCoordinador()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void coordinador_con_programa_con_id_mal_formado_es_contrato_roto() {
        devuelve(CORRELATION, "ok", "", true, "no-es-uuid", PROGRAMA, "P");

        final var exception = assertThrows(DatabaseOperationException.class,
                () -> coordinadores.crearCoordinadorConPrograma(comandoCoordinador()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void coordinador_con_programa_traduce_el_fallo_tecnico() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final var exception = assertThrows(DatabaseOperationException.class,
                () -> coordinadores.crearCoordinadorConPrograma(comandoCoordinador()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
    }

    @Test
    void coordinador_con_programa_repetido_es_conflicto() {
        devuelve(CORRELATION, "Ya existe un programa con ese nombre.", "DBCODE=PROG_003|duplicado", false, null, null, null);

        assertThrows(ConflictException.class, () -> coordinadores.crearCoordinadorConPrograma(comandoCoordinador()));
    }

    private static CrearDecanoConFacultadCommand comandoDecano() {
        return new CrearDecanoConFacultadCommand(DECANO, TIPO_ID, 7, "ANA", null, "PEREZ", null, "ana@example.test",
                "HASH", "Facultad de Ingenieria", "ING", INSTITUCION, EJECUTOR);
    }

    private static CrearCoordinadorConProgramaCommand comandoCoordinador() {
        return new CrearCoordinadorConProgramaCommand(COORDINADOR, TIPO_ID, "1001", "ANA", null, "PEREZ", null,
                "ana@example.test", "HASH", "Ingenieria de Sistemas", TIPO_PROGRAMA, MODALIDAD, JORNADA, FACULTAD, EJECUTOR);
    }
}
