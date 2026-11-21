package co.edu.uco.asistenciasuco.infrastructure.failure;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Distingue la caida de SQL Server (no hay conexion) de un error ordinario de SQL. Recorre la cadena de causas porque las
 * excepciones llegan envueltas por los adaptadores ({@code DatabaseOperationException}, {@code InternalApplicationException}).
 *
 * <p>Cuenta como caida: fallos al obtener la conexion (Spring, Hibernate, JDBC, timeout del pool Hikari), recursos de datos no
 * disponibles y un {@code CannotCreateTransactionException} cuyo origen es una conexion. NO cuenta: violaciones de restriccion,
 * deadlocks, SQL invalido ni rechazos de negocio de un SP.</p>
 */
public final class DatabaseUnavailableDetector {

    private static final int PROFUNDIDAD_MAXIMA = 12;
    /** Clase SQLSTATE 08: "connection exception" (08001 no se pudo abrir, 08S01 enlace de comunicacion caido, etc.). */
    private static final String SQLSTATE_CONEXION = "08";

    private DatabaseUnavailableDetector() {
    }

    public static boolean isConnectionFailure(final Throwable error) {
        return find(error) != null;
    }

    /** La primera causa de la cadena que identifica la caida (para el log), o {@code null} si el error no es de conexion. */
    public static Throwable find(final Throwable error) {
        final Map<Throwable, Boolean> vistos = new IdentityHashMap<>();
        Throwable actual = error;
        for (int nivel = 0; actual != null && nivel < PROFUNDIDAD_MAXIMA && vistos.put(actual, true) == null; nivel++) {
            if (esDeConexion(actual)) {
                return actual;
            }
            actual = actual.getCause();
        }
        return null;
    }

    /**
     * {@code CannotCreateTransactionException} no figura aqui a proposito: solo cuenta cuando su causa es de conexion, y esa
     * causa la encuentra el recorrido de {@link #find}.
     */
    private static boolean esDeConexion(final Throwable error) {
        if (error instanceof CannotGetJdbcConnectionException
                || error instanceof DataAccessResourceFailureException
                || error instanceof JDBCConnectionException
                || error instanceof SQLTransientConnectionException
                || error instanceof SQLNonTransientConnectionException
                || error instanceof SQLRecoverableException) {
            return true;
        }
        return error instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith(SQLSTATE_CONEXION);
    }
}
