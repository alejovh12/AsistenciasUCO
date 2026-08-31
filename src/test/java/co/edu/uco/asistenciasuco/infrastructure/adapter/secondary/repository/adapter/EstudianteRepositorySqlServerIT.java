package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
class EstudianteRepositorySqlServerIT {

    @Autowired
    private EstudianteRepositoryPort estudianteRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void consultar_estudiantes_ejecuta_sql_real_paginado_sin_duplicar_estudiante() {
        final EstudiantePaginaRepositoryProjection pagina = estudianteRepositoryPort.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10
                )
        );

        assumeTrue(pagina.totalItems() > 0, "No hay estudiantes para validar consulta real.");
        assertEquals(0, pagina.page());
        assertEquals(10, pagina.size());
        assertTrue(pagina.totalPages() >= 1);
        assertTrue(pagina.items().size() <= 10);
        assertEquals(pagina.items().size(), new HashSet<>(pagina.items().stream().map(item -> item.id()).toList()).size());
        assertNotNull(pagina.items().get(0).id());
        assertNotNull(pagina.items().get(0).idUsuario());
        assertNotNull(pagina.items().get(0).tipoIdentificacionId());
    }

    @Test
    void consultar_estudiante_por_id_ejecuta_sql_real_y_separa_contexto_academico() {
        final Optional<UUID> estudiante = firstUuid("""
                SELECT TOP 1 e.id
                FROM dbo.Estudiante e
                INNER JOIN dbo.Usuario u ON e.usuario = u.id
                ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
                """);

        assumeTrue(estudiante.isPresent(), "No hay estudiantes para validar detalle real.");

        final var detalle = estudianteRepositoryPort.consultarEstudiantePorId(estudiante.get());

        assertTrue(detalle.isPresent());
        assertEquals(estudiante.get(), detalle.get().datosPersonales().id());
        assertNotNull(detalle.get().datosPersonales().idUsuario());
        assertNotNull(detalle.get().contextosAcademicos());
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }
}
