package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.1B (ACT-001/ACT-007/ACT-008): configuracion REAL (application.yml + perfil) que activa el
 * provider de la query de asistencia. Se carga la configuracion versionada con
 * {@link ConfigDataApplicationContextInitializer}; el routing se observa por el comportamiento del
 * puerto (que tecnologia recibe la llamada), no por lectura de un bean interno.
 *
 * <p>Politica: perfiles {@code local}/{@code dev} => jpa; sin perfil (base/prod-like) => jdbc.
 * Rollback explicito por la propiedad/variable de entorno => jdbc.</p>
 */
class AsistenciaQueryProviderActivationTest {

    private static final String PROPERTY = "app.adapters.persistence.asistencia-query-provider";
    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

    private ApplicationContextRunner runnerWithRealConfig(final String... properties) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(properties)
                .withBean(NamedParameterJdbcOperations.class, () -> jdbc)
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(TransactionOperations.class, () -> mock(TransactionOperations.class))
                .withBean(EntityManagerFactory.class, () -> entityManagerFactory)
                .withUserConfiguration(
                        SqlServerProcedureSupportConfiguration.class,
                        SqlServerCoreRepositoryAdapterConfiguration.class
                );
    }

    private static ConsultarAsistenciasPorGrupoRepositoryDTO query() {
        return new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION);
    }

    private void assertQueryRoutesToJpa(final org.springframework.context.ApplicationContext context) {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
        assertThrows(DatabaseOperationException.class,
                () -> context.getBean(AsistenciaRepositoryPort.class).consultarAsistenciasPorGrupo(query()));
        verify(entityManagerFactory).createEntityManager();
        verify(jdbc, never()).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    private void assertQueryRoutesToJdbc(final org.springframework.context.ApplicationContext context) {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
        context.getBean(AsistenciaRepositoryPort.class).consultarAsistenciasPorGrupo(query());
        verify(jdbc).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
        verifyNoInteractions(entityManagerFactory);
    }

    @Test
    void perfil_local_activa_la_query_jpa() {
        runnerWithRealConfig("spring.profiles.active=local").run(this::assertQueryRoutesToJpa);
    }

    @Test
    void perfil_dev_activa_la_query_jpa() {
        runnerWithRealConfig("spring.profiles.active=dev").run(this::assertQueryRoutesToJpa);
    }

    @Test
    void sin_perfil_la_query_permanece_en_jdbc_para_no_activar_jpa_implicitamente_en_prod() {
        runnerWithRealConfig().run(this::assertQueryRoutesToJdbc);
    }

    @Test
    void rollback_explicito_a_jdbc_gana_sobre_el_perfil_local() {
        runnerWithRealConfig("spring.profiles.active=local", PROPERTY + "=jdbc").run(this::assertQueryRoutesToJdbc);
    }

    @Test
    void roll_forward_explicito_a_jpa_funciona_sin_perfil() {
        runnerWithRealConfig(PROPERTY + "=jpa").run(this::assertQueryRoutesToJpa);
    }
}
