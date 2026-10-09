package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.EstudianteProgramaJdbcBaseline;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Paridad JPA-05 de estudiantes por programa contra SQL Server real (LB-008 JPA-05).
 *
 * <p>Fixture autocontenido: un {@code EstudiantePrograma} con UUID conocido sobre un estudiante y un programa
 * existentes. La fila se elimina en {@code finally} aunque falle una aserción. Se comparan filas, campos y orden
 * entre el baseline JDBC ({@link EstudianteProgramaJdbcBaseline}, solo src/test) y la consulta JPA.</p>
 *
 * <p>Invariante "una EstudiantePrograma = una proyeccion" con varios grupos: la certifica el gate DB
 * ({@code VIEW_ESTUDIANTE_PROGRAMA_CARDINALITY}); aqui se certifica el {@code id} unico desplegado.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class EstudianteProgramaJpaParityIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void consulta_jpa_por_programa_coincide_con_baseline_jdbc_y_expone_una_fila_por_estudiante_programa() {
        final List<String> estudiantes = jdbc.queryForList("SELECT TOP 1 CAST(id AS NVARCHAR(40)) FROM dbo.Estudiante ORDER BY id", String.class);
        final List<String> programas = jdbc.queryForList("SELECT TOP 1 CAST(id AS NVARCHAR(40)) FROM dbo.uv_programa ORDER BY id", String.class);
        assumeTrue(!estudiantes.isEmpty() && !programas.isEmpty(), "Se requiere al menos un estudiante y un programa vivos.");

        final UUID estudiante = UUID.fromString(estudiantes.getFirst());
        final UUID programa = UUID.fromString(programas.getFirst());
        final UUID estudiantePrograma = UUID.randomUUID();

        final EstudianteProgramaJdbcBaseline before = new EstudianteProgramaJdbcBaseline(new NamedParameterJdbcTemplate(jdbc));
        final EstudianteProgramaJpaRepository after = new EstudianteProgramaJpaRepository(entityManager);

        jdbc.update("INSERT INTO dbo.EstudiantePrograma (id, estudiante, programa) VALUES (?, ?, ?)",
                estudiantePrograma, estudiante, programa);
        try {
            final List<EstudianteProgramaProjection> esperado = before.consultarEstudiantesPorPrograma(programa);
            final List<EstudianteProgramaProjection> obtenido = after.consultarEstudiantesPorPrograma(programa);

            assertEquals(esperado, obtenido, "La proyeccion JPA debe coincidir con el baseline JDBC (filas, campos y orden).");

            final long filasDelEstudiantePrograma = obtenido.stream()
                    .filter(fila -> fila.id().equals(estudiantePrograma))
                    .count();
            assertEquals(1L, filasDelEstudiantePrograma, "Una EstudiantePrograma debe producir exactamente una proyeccion.");
            assertTrue(obtenido.stream().map(EstudianteProgramaProjection::id).distinct().count() == obtenido.size(),
                    "Los id de la consulta por programa deben ser unicos.");
        } finally {
            jdbc.update("DELETE FROM dbo.EstudiantePrograma WHERE id = ?", estudiantePrograma);
        }
    }
}




