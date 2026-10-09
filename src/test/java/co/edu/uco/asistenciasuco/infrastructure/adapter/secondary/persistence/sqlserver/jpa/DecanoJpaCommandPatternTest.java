package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.CrearDecanoCommand;
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
 * LB-008 JPA-03 — patron del command {@code usp_crear_decano} sobre {@code EntityManager + createNativeQuery}.
 * {@code tipoIdentificacionId} no se envia a la SP (contrato actual): el test lo verifica por ausencia.
 */
class DecanoJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DECANO = UUID.fromString("70707070-7070-7070-7070-707070707070");
    private static final UUID FACULTAD = UUID.fromString("60606060-6060-6060-6060-606060606060");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("12121212-1212-1212-1212-121212121212");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private DecanoJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new DecanoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Decano creado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void DEC_PAT_001_crear_decano_usa_native_query_exec_sin_tipo_identificacion() {
        command.crearDecano(comando());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_decano"));
        verify(query).setParameter("idDecano", DECANO);
        verify(query).setParameter("numeroIdentificacion", 7);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("nombreFacultad", "FACULTAD PRUEBA");
        verify(query).setParameter("password", "Clave123!");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query, never()).setParameter(org.mockito.ArgumentMatchers.eq("idTipoIdIdentificacion"), any());
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void DEC_PAT_002_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearDecano(comando()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void DEC_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearDecano(comando()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    private static CrearDecanoCommand comando() {
        return new CrearDecanoCommand(DECANO, TIPO_IDENTIFICACION, 7, "ANA", null, "PEREZ", null,
                "ana@example.test", FACULTAD, "FACULTAD PRUEBA", "Clave123!", EJECUTOR);
    }
}




