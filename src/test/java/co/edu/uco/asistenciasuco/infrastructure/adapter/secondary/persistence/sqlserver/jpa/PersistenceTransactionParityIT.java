package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LB-008 JPA-02A, TRANSACTION_MANAGER_PARITY: el {@code TransactionOperations} que consume el legacy JDBC
 * (p. ej. {@code GrupoJpaRepository}) debe conservar la semantica transaccional observable
 * tras cambiar el bootstrap. No fija el tipo del {@code PlatformTransactionManager}; afirma el
 * comportamiento sobre el recurso ligado por Spring: dentro de la frontera hay una transaccion activa, la
 * conexion JDBC es la misma que el transaction manager liga (autocommit desactivado), las fronteras
 * anidadas reutilizan esa conexion sin abrir otra transaccion y una excepcion libera el recurso y restaura
 * autocommit. Debe pasar igual antes y despues del cambio de bootstrap.
 *
 * <p>No usa {@code @@TRANCOUNT}: el driver mssql-jdbc no lo expone de forma fiable en este patron (ver
 * JPA-02A, evidencia de la baseline).</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class PersistenceTransactionParityIT {

    private static final String SPID_SQL = "SELECT @@SPID";

    @Autowired
    private TransactionOperations transactionOperations;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void jdbc_dentro_de_la_frontera_usa_la_conexion_ligada_por_el_transaction_manager() {
        final FrontieraProbe probe = transactionOperations.execute(status -> {
            final Connection primera = DataSourceUtils.getConnection(dataSource);
            final Connection segunda = DataSourceUtils.getConnection(dataSource);
            try {
                return new FrontieraProbe(
                        TransactionSynchronizationManager.isActualTransactionActive(),
                        autoCommit(primera),
                        primera == segunda,
                        jdbcTemplate.queryForObject(SPID_SQL, Integer.class),
                        jdbcTemplate.queryForObject(SPID_SQL, Integer.class)
                );
            } finally {
                DataSourceUtils.releaseConnection(segunda, dataSource);
                DataSourceUtils.releaseConnection(primera, dataSource);
            }
        });

        assertTrue(probe.activa());
        assertFalse(probe.autoCommit());
        assertTrue(probe.mismaConexionLigada());
        assertEquals(probe.spidPrimera(), probe.spidSegunda());
    }

    @Test
    void frontera_anidada_reutiliza_la_conexion_de_la_transaccion_externa() {
        final Boolean mismaConexion = transactionOperations.execute(outer -> {
            final Connection conexionExterna = DataSourceUtils.getConnection(dataSource);
            try {
                final Connection conexionAnidada = transactionOperations.execute(inner -> {
                    final Connection conexionInterna = DataSourceUtils.getConnection(dataSource);
                    try {
                        return conexionInterna;
                    } finally {
                        DataSourceUtils.releaseConnection(conexionInterna, dataSource);
                    }
                });
                return conexionExterna == conexionAnidada;
            } finally {
                DataSourceUtils.releaseConnection(conexionExterna, dataSource);
            }
        });

        assertTrue(mismaConexion);
    }

    @Test
    void excepcion_dentro_de_la_frontera_libera_el_recurso_y_restaura_autocommit() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                transactionOperations.execute(status -> {
                    jdbcTemplate.queryForObject(SPID_SQL, Integer.class);
                    throw new IllegalStateException("rollback esperado");
                }));

        assertEquals("rollback esperado", exception.getMessage());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertFalse(TransactionSynchronizationManager.hasResource(dataSource));
        final Connection fueraDeLaFrontera = DataSourceUtils.getConnection(dataSource);
        try {
            assertTrue(autoCommit(fueraDeLaFrontera));
        } finally {
            DataSourceUtils.releaseConnection(fueraDeLaFrontera, dataSource);
        }
    }

    private static boolean autoCommit(final Connection connection) {
        try {
            return connection.getAutoCommit();
        } catch (final SQLException exception) {
            throw new IllegalStateException("No fue posible leer autocommit de la conexion de prueba.", exception);
        }
    }

    private record FrontieraProbe(
            boolean activa,
            boolean autoCommit,
            boolean mismaConexionLigada,
            Integer spidPrimera,
            Integer spidSegunda
    ) {
    }
}




