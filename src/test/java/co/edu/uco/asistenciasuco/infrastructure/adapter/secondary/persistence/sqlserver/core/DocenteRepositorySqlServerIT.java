package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class DocenteRepositorySqlServerIT {

    @Autowired
    private DocenteRepositoryPort docenteRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void uv_docente_identidad_devuelve_resultados_validos() {
        final List<DocenteIdentidadRepositoryProjection> docentes = docenteRepositoryPort.consultarDocentes();

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

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }
}
