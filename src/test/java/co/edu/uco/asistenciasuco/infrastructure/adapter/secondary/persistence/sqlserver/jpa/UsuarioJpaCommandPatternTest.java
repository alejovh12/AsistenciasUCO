package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-03 — patron del command {@code usp_sincronizar_usuario}. El provider publico existe ahora en la DB
 * final (TD-043 cerrado) y se prohibe sustituirlo por {@code usp_sincronizar_usuario_interno} (contrato OUTPUT
 * distinto, no consumible desde el backend). El test verifica el SQL emitido y los parametros, no la existencia
 * del provider.
 */
class UsuarioJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("12121212-1212-1212-1212-121212121212");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final TypedQuery<Object> typedQuery = mock(TypedQuery.class);
    private UsuarioJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new UsuarioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Usuario sincronizado.", "", true}));
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void USU_PAT_001_sincronizar_usuario_usa_native_query_exec_con_parametros_del_contrato() {
        command.crearUsuario(dto());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_sincronizar_usuario"));
        verify(query).setParameter("idTipoIdIdentificacion", TIPO_IDENTIFICACION);
        verify(query).setParameter("numeroIdentificacion", 123456);
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("password", "Clave123!");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
        verify(entityManager, never()).createNativeQuery(contains("usp_sincronizar_usuario_interno"));
    }

    @Test
    void USU_PAT_002_sp_ausente_se_clasifica_como_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("Could not find stored procedure"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearUsuario(dto()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void USU_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearUsuario(dto()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    private static CrearUsuarioRepositoryDTO dto() {
        return new CrearUsuarioRepositoryDTO(TIPO_IDENTIFICACION, 123456, "PEREZ", "GOMEZ", "ANA", "MARIA",
                "ana@example.test", "Clave123!");
    }
}




