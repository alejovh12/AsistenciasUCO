package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaQueryExecutorTest {

    private static final String OPERATION = "consultarAlgo";
    private static final String MESSAGE = "No fue posible consultar algo.";

    @Test
    void resultado_exitoso_devuelve_el_valor_de_la_query() {
        final List<String> resultado = JpaQueryExecutor.execute(OPERATION, MESSAGE, () -> List.of("a", "b"));

        assertEquals(List.of("a", "b"), resultado);
    }

    @Test
    void persistence_exception_se_traduce_preservando_la_causa() {
        final PersistenceException causa = new PersistenceException("fallo jpa");

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute(OPERATION, MESSAGE, falla(causa))
        );

        assertSame(causa, exception.getCause());
        assertEquals(MESSAGE, exception.getMessage());
    }

    @Test
    void illegal_state_exception_se_traduce_a_database_operation_exception() {
        final IllegalStateException causa = new IllegalStateException("em cerrado");

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute(OPERATION, MESSAGE, falla(causa))
        );

        assertSame(causa, exception.getCause());
    }

    @Test
    void illegal_argument_exception_se_traduce_a_database_operation_exception() {
        final IllegalArgumentException causa = new IllegalArgumentException("hql invalido");

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute(OPERATION, MESSAGE, falla(causa))
        );

        assertSame(causa, exception.getCause());
    }

    @Test
    void arithmetic_exception_se_traduce_a_database_operation_exception() {
        final ArithmeticException causa = new ArithmeticException("conteo fuera de rango");

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> JpaQueryExecutor.execute(OPERATION, MESSAGE, falla(causa))
        );

        assertSame(causa, exception.getCause());
    }

    @Test
    void excepcion_funcional_no_se_convierte_en_error_tecnico() {
        final UnsupportedOperationException funcional = new UnsupportedOperationException("regla de dominio");

        final UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> JpaQueryExecutor.execute(OPERATION, MESSAGE, falla(funcional))
        );

        assertSame(funcional, exception);
    }

    private static Supplier<String> falla(final RuntimeException exception) {
        return () -> {
            throw exception;
        };
    }
}
