package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.InstitutionalScopeJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.ReporteAsistenciaJdbcBaseline;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Paridad JPA-05 de alcance institucional (seguridad) y reporte contra SQL Server real.
 *
 * <p>Seguridad: se recorre la matriz usuarios x recursos (vivos y ajenos) y se compara cada decision booleana,
 * conteo u Optional entre BEFORE (JDBC copiado) y AFTER (JPA). Una divergencia bloquea JPA-05.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AuthorizationReportJpaParityIT {

    private static final String EMAIL_INEXISTENTE = "nadie.inexistente@example.invalid";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private NamedParameterJdbcOperations named;
    private InstitutionalScopeJdbcBaseline before;
    private InstitutionalScopeJpaRepository after;

    @BeforeEach
    void setUp() {
        named = new NamedParameterJdbcTemplate(jdbc);
        before = new InstitutionalScopeJdbcBaseline(named);
        after = new InstitutionalScopeJpaRepository(entityManager);
    }

    @Test
    void find_por_usuario_conserva_optional_y_not_found_para_usuarios_vivos_y_desconocidos() {
        final List<Map<String, Object>> usuarios = jdbc.queryForList("SELECT id, correo FROM dbo.uv_usuario ORDER BY id");
        assumeTrue(!usuarios.isEmpty(), "No hay usuarios vivos para la matriz de alcance.");
        final List<UUID> ids = new java.util.ArrayList<>(usuarios.stream().map(row -> UUID.fromString(String.valueOf(row.get("id")))).toList());
        ids.add(UUID.randomUUID());

        for (UUID usuarioId : ids) {
            assertEquals(before.findUsuarioIdById(usuarioId), after.findUsuarioIdById(usuarioId), "findUsuarioIdById " + usuarioId);
            assertEquals(before.findDocenteIdByUsuario(usuarioId), after.findDocenteIdByUsuario(usuarioId));
            assertEquals(before.findEstudianteIdByUsuario(usuarioId), after.findEstudianteIdByUsuario(usuarioId));
            assertEquals(before.findProgramaIdByCoordinadorUsuario(usuarioId), after.findProgramaIdByCoordinadorUsuario(usuarioId));
            assertEquals(before.findCoordinadorIdByUsuario(usuarioId), after.findCoordinadorIdByUsuario(usuarioId));
            assertEquals(before.findFacultadIdByDecanoUsuario(usuarioId), after.findFacultadIdByDecanoUsuario(usuarioId));
            assertEquals(before.findDecanoIdByUsuario(usuarioId), after.findDecanoIdByUsuario(usuarioId));
        }
        assertEquals(Optional.empty(), after.findUsuarioIdById(UUID.randomUUID()));
    }

    @Test
    void find_por_email_conserva_comparacion_sin_distincion_de_mayusculas_y_not_found() {
        final List<Map<String, Object>> usuarios = jdbc.queryForList("SELECT id, correo FROM dbo.uv_usuario ORDER BY id");
        assumeTrue(!usuarios.isEmpty(), "No hay usuarios vivos para comparar correo.");
        for (Map<String, Object> usuario : usuarios) {
            final String correo = (String) usuario.get("correo");
            assertEquals(before.findUsuarioIdByEmail(correo), after.findUsuarioIdByEmail(correo), "correo exacto");
            assertEquals(before.findUsuarioIdByEmail(correo.toUpperCase()), after.findUsuarioIdByEmail(correo.toUpperCase()),
                    "correo en mayusculas");
        }
        assertEquals(before.findUsuarioIdByEmail(EMAIL_INEXISTENTE), after.findUsuarioIdByEmail(EMAIL_INEXISTENTE));
        assertEquals(Optional.empty(), after.findUsuarioIdByEmail(EMAIL_INEXISTENTE));
    }

    @Test
    void decisiones_de_ownership_y_scope_coinciden_con_jdbc_para_cada_usuario_y_recurso() {
        final List<UUID> usuarios = jdbc.queryForList("SELECT id FROM dbo.uv_usuario ORDER BY id").stream()
                .map(row -> UUID.fromString(String.valueOf(row.get("id")))).toList();
        final List<UUID> grupos = jdbc.queryForList("SELECT id FROM dbo.uv_grupo ORDER BY id").stream()
                .map(row -> UUID.fromString(String.valueOf(row.get("id")))).toList();
        final List<UUID> programas = jdbc.queryForList("SELECT idPrograma FROM dbo.uv_coordinador ORDER BY id").stream()
                .map(row -> UUID.fromString(String.valueOf(row.get("idPrograma")))).toList();
        final List<UUID> facultades = jdbc.queryForList("SELECT idFacultad FROM dbo.uv_decano ORDER BY id").stream()
                .map(row -> UUID.fromString(String.valueOf(row.get("idFacultad")))).toList();
        assumeTrue(!usuarios.isEmpty() && !grupos.isEmpty(), "No hay usuarios o grupos vivos.");

        final List<UUID> usuariosConDesconocido = new java.util.ArrayList<>(usuarios);
        usuariosConDesconocido.add(UUID.randomUUID());
        final List<UUID> gruposConDesconocido = new java.util.ArrayList<>(grupos);
        gruposConDesconocido.add(UUID.randomUUID());
        final List<UUID> programasConDesconocido = new java.util.ArrayList<>(programas);
        programasConDesconocido.add(UUID.randomUUID());
        final List<UUID> facultadesConDesconocido = new java.util.ArrayList<>(facultades);
        facultadesConDesconocido.add(UUID.randomUUID());

        int permitidos = 0;
        int denegados = 0;
        for (UUID usuario : usuariosConDesconocido) {
            for (UUID grupo : grupos) {
                final boolean docente = before.canDocenteAccessGrupo(usuario, grupo);
                assertEquals(docente, after.canDocenteAccessGrupo(usuario, grupo), "docente " + usuario + " " + grupo);
                final boolean estudiante = before.canEstudianteAccessGrupo(usuario, grupo);
                assertEquals(estudiante, after.canEstudianteAccessGrupo(usuario, grupo), "estudiante " + usuario + " " + grupo);
                if (docente || estudiante) {
                    permitidos++;
                } else {
                    denegados++;
                }
            }
            for (UUID programa : programasConDesconocido) {
                final boolean coordinador = before.canCoordinadorAccessPrograma(usuario, programa);
                assertEquals(coordinador, after.canCoordinadorAccessPrograma(usuario, programa));
                if (coordinador) {
                    permitidos++;
                } else {
                    denegados++;
                }
            }
            for (UUID facultad : facultadesConDesconocido) {
                final boolean decano = before.canDecanoAccessFacultad(usuario, facultad);
                assertEquals(decano, after.canDecanoAccessFacultad(usuario, facultad));
                if (decano) {
                    permitidos++;
                } else {
                    denegados++;
                }
            }
        }
        assertTrue(permitidos > 0, "La matriz debe observar al menos un caso de ownership permitido.");
        assertTrue(denegados > 0, "La matriz debe observar al menos un caso denegado.");
        assertFalse(after.canDocenteAccessGrupo(UUID.randomUUID(), grupos.getFirst()),
                "Usuario desconocido no accede a grupo.");
        assertFalse(after.canDecanoAccessFacultad(UUID.randomUUID(), UUID.randomUUID()),
                "Recurso desconocido no se concede.");
    }

    @Test
    void reporte_de_asistencia_conserva_filas_orden_nulos_y_utc_por_grupo() {
        final List<UUID> grupos = jdbc.queryForList("SELECT DISTINCT idGrupo FROM dbo.uv_sesion ORDER BY idGrupo").stream()
                .map(row -> UUID.fromString(String.valueOf(row.get("idGrupo")))).toList();
        assumeTrue(!grupos.isEmpty(), "No hay sesiones vivas para el reporte.");
        final ReporteAsistenciaJdbcBaseline reporteBefore = new ReporteAsistenciaJdbcBaseline(named);
        final ReporteAsistenciaJpaRepository reporteAfter = new ReporteAsistenciaJpaRepository(entityManager);

        for (UUID grupo : grupos) {
            final List<ReporteAsistenciaRow> filasBefore = reporteBefore.consultarReporteAsistenciaGrupo(grupo);
            assertEquals(filasBefore, reporteAfter.consultarReporteAsistenciaGrupo(grupo), "grupo " + grupo);
        }
        assertEquals(List.of(), reporteAfter.consultarReporteAsistenciaGrupo(UUID.randomUUID()));
    }
}




