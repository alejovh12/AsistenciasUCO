package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.1 (JPA-Q-009/010/011): seleccion del provider de la query de asistencia en el
 * Composition Root. Solo se prueba routing/delegacion con fakes; estos tests NO certifican que JPA
 * funcione (eso lo certifica el IT de paridad contra SQL Server real).
 */
class AsistenciaQueryProviderCompositionRootTest {

    private static final String PROPERTY = "app.adapters.persistence.asistencia-query-provider";
    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    private ApplicationContextRunner runner(final String... properties) {
        return new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.provider=sqlserver")
                .withPropertyValues(properties)
                .withBean(NamedParameterJdbcOperations.class, () -> jdbc)
                .withBean(org.springframework.jdbc.core.JdbcTemplate.class,
                        () -> mock(org.springframework.jdbc.core.JdbcTemplate.class))
                .withBean(org.springframework.transaction.support.TransactionOperations.class,
                        () -> mock(org.springframework.transaction.support.TransactionOperations.class))
                .withBean(EntityManagerFactory.class, () -> entityManagerFactory)
                .withUserConfiguration(
                        SqlServerProcedureSupportConfiguration.class,
                        SqlServerCoreRepositoryAdapterConfiguration.class
                );
    }

    @SuppressWarnings("unchecked")
    private void stubJdbcQueryReturningEmpty() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
    }

    @Test
    void sin_propiedad_la_query_usa_jdbc_y_no_toca_jpa() {
        stubJdbcQueryReturningEmpty();
        runner().run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());

            final List<AsistenciaRepositoryProjection> result = context.getBean(AsistenciaRepositoryPort.class)
                    .consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION));

            assertTrue(result.isEmpty());
            verify(jdbc).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
            verifyNoInteractions(entityManagerFactory);
        });
    }

    @Test
    void provider_jdbc_explicito_usa_jdbc_y_no_toca_jpa() {
        stubJdbcQueryReturningEmpty();
        runner(PROPERTY + "=jdbc").run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());

            context.getBean(AsistenciaRepositoryPort.class)
                    .consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null));

            verify(jdbc).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
            verifyNoInteractions(entityManagerFactory);
        });
    }

    @Test
    void provider_jpa_resuelve_un_unico_puerto_y_ejecuta_la_query_por_jpa_sin_jdbc() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        runner(PROPERTY + "=jpa").run(context -> {
            assertNotNull(context.getBeansOfType(AsistenciaRepositoryPort.class));
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());

            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);
            assertThrows(DatabaseOperationException.class,
                    () -> port.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION)));

            verify(entityManagerFactory).createEntityManager();
            verify(jdbc, never()).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        });
    }

    @Test
    void provider_jpa_mantiene_los_commands_en_jdbc_y_no_abre_entity_manager() {
        CorrelationIdContext.set(UUID.randomUUID());
        stubJdbcQueryReturningEmpty();
        runner(PROPERTY + "=jpa").run(context -> {
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            // El executor canonico exige exactamente una fila; con [] falla DESPUES de invocar JDBC.
            assertThrows(DatabaseOperationException.class, () -> port.registrarAsistenciasSesion(
                    new RegistrarAsistenciasSesionRepositoryDTO(
                            SESION,
                            List.of(new RegistroAsistenciaSesionRepositoryDTO(UUID.randomUUID(), "AN")),
                            UUID.randomUUID())));

            verify(jdbc).query(contains("usp_registrar_asistencias_sesion"), any(SqlParameterSource.class),
                    any(RowMapper.class));
            verifyNoInteractions(entityManagerFactory);
        });
    }

    @Test
    void provider_jpa_conserva_la_semantica_de_dto_nulo() {
        runner(PROPERTY + "=jpa").run(context -> {
            final CrosscuttingException exception = assertThrows(CrosscuttingException.class,
                    () -> context.getBean(AsistenciaRepositoryPort.class).consultarAsistenciasPorGrupo(null));

            assertEquals("El dominio para consultar asistencias por grupo es obligatorio.", exception.getMessage());
            verifyNoInteractions(entityManagerFactory);
        });
    }

    @Test
    void valor_desconocido_del_selector_falla_el_arranque_en_lugar_de_caer_a_otro_provider() {
        runner(PROPERTY + "=hibrido-inventado").run(context -> assertFalse(
                context.getStartupFailure() == null,
                "Un valor no soportado del selector debe fallar cerrado."));
    }
}
