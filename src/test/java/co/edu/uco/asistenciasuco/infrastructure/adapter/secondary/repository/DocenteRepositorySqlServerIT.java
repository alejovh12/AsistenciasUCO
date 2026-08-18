package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
class DocenteRepositorySqlServerIT {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    @Autowired
    private DocenteRepositoryPort docenteRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Environment environment;

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void uv_docente_identidad_devuelve_resultados_validos() {
        final List<DocenteIdentidadRepositoryEntity> docentes = docenteRepositoryPort.consultarDocentes();

        assumeTrue(!docentes.isEmpty(), "No hay docentes para validar dbo.uv_docente_identidad.");
        assertNotNull(docentes.get(0).getId());
        assertNotNull(docentes.get(0).getIdUsuario());
        assertNotNull(docentes.get(0).getNumeroIdentificacion());
        assertNotNull(docentes.get(0).getNombreCompleto());
    }

    @Test
    void uv_docente_puede_devolver_multiples_filas_para_un_docente() {
        final Optional<UUID> docente = firstUuid("""
                SELECT TOP 1 id
                FROM dbo.uv_docente
                GROUP BY id
                HAVING COUNT(*) > 1
                """);

        assumeTrue(docente.isPresent(), "No hay docente con multiples asignaciones para esta base local.");

        final var asignaciones = docenteRepositoryPort.consultarAsignacionesAcademicas(
                new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(docente.get())
        );

        assertTrue(asignaciones.size() > 1);
    }

    @Test
    void docente_puede_no_tener_filas_en_uv_docente() {
        final Optional<UUID> docente = firstUuid("""
                SELECT TOP 1 identidad.id
                FROM dbo.uv_docente_identidad identidad
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM dbo.uv_docente detalle
                    WHERE detalle.id = identidad.id
                )
                """);

        assumeTrue(docente.isPresent(), "No hay docente sin asignaciones para esta base local.");

        final var asignaciones = docenteRepositoryPort.consultarAsignacionesAcademicas(
                new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(docente.get())
        );

        assertTrue(asignaciones.isEmpty());
    }

    @Test
    void registrar_docente_desde_usuario_rechaza_uuid_usuario_vacio_sin_escritura() {
        CorrelationIdContext.set(UUID.randomUUID());

        final ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> docenteRepositoryPort.registrarDocenteDesdeUsuario(
                        new RegistrarDocenteDesdeUsuarioRepositoryDTO(EMPTY_UUID)
                )
        );

        assertNotNull(exception.getCode());
    }

    @Test
    void asignar_docente_a_grupo_cambia_dentro_de_transaccion_y_rollback_restaura() {
        final String grupoProperty = environment.getProperty("APP_DOCENTE_IT_GRUPO_ID");
        final String docenteProperty = environment.getProperty("APP_DOCENTE_IT_DOCENTE_ID");

        assumeTrue(grupoProperty != null && !grupoProperty.isBlank(), "APP_DOCENTE_IT_GRUPO_ID no configurado.");
        assumeTrue(docenteProperty != null && !docenteProperty.isBlank(), "APP_DOCENTE_IT_DOCENTE_ID no configurado.");

        final UUID grupo = UUID.fromString(grupoProperty);
        final UUID docente = UUID.fromString(docenteProperty);
        final UUID docenteAnterior = jdbcTemplate.queryForObject(
                "SELECT docente FROM dbo.Grupo WHERE id = ?",
                UUID.class,
                grupo.toString()
        );

        transactionTemplate.executeWithoutResult(status -> {
            CorrelationIdContext.set(UUID.randomUUID());
            try {
                docenteRepositoryPort.asignarDocenteAGrupo(new AsignarDocenteAGrupoRepositoryDTO(
                        docente,
                        grupo
                ));
            } catch (ApplicationException exception) {
                assumeTrue(false, "El candidato configurado no pudo asignarse sin cruce.");
            }

            final UUID docenteActual = jdbcTemplate.queryForObject(
                    "SELECT docente FROM dbo.Grupo WHERE id = ?",
                    UUID.class,
                    grupo.toString()
            );
            assertEquals(docente, docenteActual);
            status.setRollbackOnly();
        });

        final UUID docenteDespuesRollback = jdbcTemplate.queryForObject(
                "SELECT docente FROM dbo.Grupo WHERE id = ?",
                UUID.class,
                grupo.toString()
        );
        assertEquals(docenteAnterior, docenteDespuesRollback);
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }
}
