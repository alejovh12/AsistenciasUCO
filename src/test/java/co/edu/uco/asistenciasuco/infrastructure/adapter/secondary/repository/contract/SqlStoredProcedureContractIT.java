package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.contract;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
@SpringBootTest
class SqlStoredProcedureContractIT {

    private static final String SQL_PARAMETERS = """
            SELECT
                o.name AS procedureName,
                p.name AS parameterName,
                TYPE_NAME(p.user_type_id) AS typeName,
                p.is_output AS output
            FROM sys.objects o
            INNER JOIN sys.parameters p ON p.object_id = o.object_id
            WHERE SCHEMA_NAME(o.schema_id) = 'dbo'
              AND o.type = 'P'
              AND o.name IN (
                  'usp_sincronizar_usuario_interno',
                  'usp_registrar_estudiante_en_grupo_usuario_no_existente',
                  'usp_sincronizar_docente_interno',
                  'usp_registrar_docente_en_grupo_interno'
              )
            ORDER BY o.name, p.parameter_id
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void stored_procedures_criticos_mantienen_contrato_backend_db() {
        final Map<String, List<SqlParameterContract>> actualParameters = jdbcTemplate.query(
                SQL_PARAMETERS,
                resultSet -> {
                    final Map<String, List<SqlParameterContract>> result = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        result.computeIfAbsent(resultSet.getString("procedureName"), key -> new ArrayList<>())
                                .add(new SqlParameterContract(
                                        resultSet.getString("parameterName"),
                                        resultSet.getString("typeName"),
                                        resultSet.getBoolean("output")
                                ));
                    }
                    return result;
                }
        );

        expectedContracts().forEach((procedureName, expectedParameters) ->
                assertProcedureContract(procedureName, expectedParameters, actualParameters.get(procedureName))
        );
    }

    private void assertProcedureContract(
            final String procedureName,
            final List<SqlParameterContract> expectedParameters,
            final List<SqlParameterContract> actualParameters
    ) {
        assertTrue(
                actualParameters != null && !actualParameters.isEmpty(),
                () -> "Contrato SQL incompatible:\n" + procedureName + "\nesperaba procedimiento almacenado existente"
        );

        for (final SqlParameterContract expectedParameter : expectedParameters) {
            if (!actualParameters.contains(expectedParameter)) {
                fail("Contrato SQL incompatible:\n"
                        + procedureName
                        + "\nesperaba parametro "
                        + expectedParameter.parameterName());
            }
        }

        assertEquals(
                expectedParameters,
                actualParameters,
                () -> "Contrato SQL incompatible:\n" + procedureName + "\nparametros actuales no coinciden con el backend"
        );
    }

    private Map<String, List<SqlParameterContract>> expectedContracts() {
        final Map<String, List<SqlParameterContract>> contracts = new LinkedHashMap<>();
        contracts.put("usp_sincronizar_usuario_interno", List.of(
                input("@idTipoIdIdentificacion", "uniqueidentifier"),
                input("@numeroIdentificacion", "int"),
                input("@primerApellido", "nvarchar"),
                input("@segundoApellido", "nvarchar"),
                input("@primerNombre", "nvarchar"),
                input("@segundoNombre", "nvarchar"),
                input("@correo", "nvarchar"),
                input("@password", "nvarchar"),
                input("@idCorrelacion", "uniqueidentifier"),
                output("@mensajeUsuarioResultado", "nvarchar"),
                output("@mensajeTecnicoResultado", "nvarchar"),
                output("@estadoResultado", "bit")
        ));
        contracts.put("usp_registrar_estudiante_en_grupo_usuario_no_existente", List.of(
                input("@idTipoIdIdentificacion", "uniqueidentifier"),
                input("@numeroIdentificacion", "int"),
                input("@primerApellido", "nvarchar"),
                input("@segundoApellido", "nvarchar"),
                input("@primerNombre", "nvarchar"),
                input("@segundoNombre", "nvarchar"),
                input("@correo", "nvarchar"),
                input("@password", "nvarchar"),
                input("@idGrupo", "uniqueidentifier"),
                input("@idCorrelacion", "uniqueidentifier")
        ));
        contracts.put("usp_sincronizar_docente_interno", List.of(
                input("@idUsuario", "uniqueidentifier"),
                input("@idCorrelacion", "uniqueidentifier"),
                output("@mensajeUsuarioResultado", "nvarchar"),
                output("@mensajeTecnicoResultado", "nvarchar"),
                output("@estadoResultado", "bit")
        ));
        contracts.put("usp_registrar_docente_en_grupo_interno", List.of(
                input("@idDocente", "uniqueidentifier"),
                input("@idGrupo", "uniqueidentifier"),
                input("@idCorrelacion", "uniqueidentifier"),
                output("@mensajeUsuarioResultado", "nvarchar"),
                output("@mensajeTecnicoResultado", "nvarchar"),
                output("@estadoResultado", "bit")
        ));
        return contracts;
    }

    private SqlParameterContract input(final String parameterName, final String typeName) {
        return new SqlParameterContract(parameterName, typeName, false);
    }

    private SqlParameterContract output(final String parameterName, final String typeName) {
        return new SqlParameterContract(parameterName, typeName, true);
    }

    private record SqlParameterContract(String parameterName, String typeName, boolean output) {
    }
}
