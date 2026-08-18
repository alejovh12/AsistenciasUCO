package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@SpringBootTest
class SqlSecurityViewContractIT {

    private static final String SQL_SECURITY_VIEW_COLUMNS = """
            SELECT
                v.name AS viewName,
                c.name AS columnName
            FROM sys.views v
            INNER JOIN sys.schemas s ON s.schema_id = v.schema_id
            INNER JOIN sys.columns c ON c.object_id = v.object_id
            WHERE s.name = 'dbo'
              AND v.name IN ('uv_usuario_autenticacion', 'uv_usuario_perfil')
            ORDER BY v.name, c.column_id
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void vistas_criticas_de_seguridad_mantienen_columnas_consumidas_por_backend() {
        final Map<String, List<String>> actualColumns = jdbcTemplate.query(
                SQL_SECURITY_VIEW_COLUMNS,
                resultSet -> {
                    final Map<String, List<String>> result = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        result.computeIfAbsent(resultSet.getString("viewName"), key -> new ArrayList<>())
                                .add(resultSet.getString("columnName"));
                    }
                    return result;
                }
        );

        assertViewColumns("uv_usuario_autenticacion", List.of(
                "idUsuario",
                "correo",
                "password",
                "correoConfirmado",
                "estaActivoUsuario"
        ), actualColumns);
        assertViewColumns("uv_usuario_perfil", List.of(
                "idUsuario",
                "idPerfil",
                "codigoPerfil",
                "nombrePerfil",
                "estado"
        ), actualColumns);
    }

    private void assertViewColumns(
            final String viewName,
            final List<String> expectedColumns,
            final Map<String, List<String>> actualColumns
    ) {
        final List<String> viewColumns = actualColumns.get(viewName);
        assertTrue(
                viewColumns != null && !viewColumns.isEmpty(),
                () -> "Contrato DB incompatible:\n" + viewName + " no existe o no tiene columnas visibles"
        );
        for (final String expectedColumn : expectedColumns) {
            assertTrue(
                    viewColumns.contains(expectedColumn),
                    () -> "Contrato DB incompatible:\n" + viewName + " no contiene la columna " + expectedColumn
            );
        }
    }
}
