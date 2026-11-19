package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.FilaFaltaReclamable;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.PaginaFaltasReclamables;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ATA D10, contrato JPA/SP con SQL Server real: la toma en SJC de una sesion SIN aviso no es reclamable; con la fila de
 * {@code AvisoFaltaSesion} aparece con las 12 columnas del contrato y {@code totalRegistros}; el docente recibe {@code SEC_001} y una
 * pagina invalida {@code PAG_001}. Las sesiones y marcas de prueba se eliminan al terminar.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class FaltaReclamableSqlServerIT {

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;
    @Autowired
    private FaltaReclamableRepositoryPort faltaPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AttendanceCommandParityFixture fixture;
    private final List<UUID> sesionesAvisadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        fixture.prepare();
    }

    @AfterEach
    void limpiar() {
        try {
            for (final UUID sesion : sesionesAvisadas) {
                jdbcTemplate.update("DELETE FROM dbo.AvisoFaltaSesion WHERE sesion = ?", sesion.toString());
            }
            fixture.cleanup();
        } finally {
            sesionesAvisadas.clear();
            CorrelationIdContext.clear();
        }
    }

    @Test
    void la_toma_en_sjc_solo_es_reclamable_cuando_la_sesion_ya_fue_avisada() {
        final UUID sesion = fixture.newSessionA();
        final UUID estudiante = fixture.estudiantes().getFirst();
        final String usuario = jdbcTemplate.queryForObject("SELECT CAST(usuario AS NVARCHAR(36)) FROM dbo.Estudiante WHERE id = ?", String.class,
                estudiante.toString());
        final String estudianteGrupo = jdbcTemplate.queryForObject(
                "SELECT CAST(id AS NVARCHAR(36)) FROM dbo.EstudianteGrupo WHERE estudiante = ? AND grupo = ?", String.class,
                estudiante.toString(), fixture.grupoId().toString());
        final String toma = UUID.randomUUID().toString();
        final String asistencia = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO dbo.TomaAsistencia (id, sesion, numero, modo, estado, esImplicita, docenteApertura, fechaAperturaUtc,
                                                fechaCierreUtc, idCorrelacionApertura)
                SELECT ?, ?, 1, (SELECT m.id FROM dbo.ModoTomaAsistencia m WHERE m.usaCodigoQr = 0),
                       (SELECT e.id FROM dbo.EstadoTomaAsistencia e WHERE e.estaAbierta = 0), 0, ?, SYSUTCDATETIME(), SYSUTCDATETIME(), NEWID()""",
                toma, sesion.toString(), fixture.docenteId().toString());
        jdbcTemplate.update("INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (?, ?, ?)", asistencia, estudianteGrupo, sesion.toString());
        jdbcTemplate.update("""
                INSERT INTO dbo.DetalleAsistencia (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin, toma,
                                                   registradoPorEstudiante, fechaRegistroUtc)
                SELECT NEWID(), NEXT VALUE FOR dbo.seq_detalle_asistencia_codigo, ?, 0, rc.id, s.fechaHoraInicio, s.fechaHoraFin, ?, 0, SYSUTCDATETIME()
                FROM dbo.RazonCausa rc CROSS JOIN dbo.Sesion s WHERE rc.codigo = 'SJC' AND s.id = ?""", asistencia, toma, sesion.toString());
        final UUID usuarioId = UUID.fromString(usuario);

        // sin la marca de aviso la falta no es reclamable
        assertTrue(faltaPort.consultarEstudiante(usuarioId, 1, 50).filas().stream().noneMatch(f -> f.idSesion().equals(sesion)));

        jdbcTemplate.update("INSERT INTO dbo.AvisoFaltaSesion (sesion, fechaAvisoUtc, totalEstudiantesAvisados, idCorrelacion) VALUES (?, SYSUTCDATETIME(), 1, NEWID())",
                sesion.toString());
        sesionesAvisadas.add(sesion);

        final PaginaFaltasReclamables pagina = faltaPort.consultarEstudiante(usuarioId, 1, 50);
        final FilaFaltaReclamable fila = pagina.filas().stream().filter(f -> f.idSesion().equals(sesion)).findFirst()
                .orElseThrow(() -> new AssertionError("La toma en SJC de una sesion avisada debe ser reclamable"));
        assertEquals(fixture.grupoId(), fila.idGrupo());
        assertEquals(UUID.fromString(toma), fila.idToma());
        assertEquals(1, fila.numeroToma());
        assertNotNull(fila.nombreGrupo());
        assertNotNull(fila.idAsignatura());
        assertNotNull(fila.nombreAsignatura());
        assertTrue(fila.numeroSesion() >= 1);
        assertNotNull(fila.fechaHoraInicioUtc());
        assertNotNull(fila.fechaHoraFinUtc());
        assertNotNull(fila.fechaPuntoControlUtc());
        assertTrue(fila.fechaLimiteReclamoUtc().isAfter(Instant.now()), "el plazo del reclamo sigue vigente");
        assertTrue(pagina.total() >= 1);

        // un estudiante sin esa marca no la ve
        final String otroUsuario = jdbcTemplate.queryForObject("SELECT CAST(usuario AS NVARCHAR(36)) FROM dbo.Estudiante WHERE id = ?", String.class,
                fixture.estudiantes().get(1).toString());
        assertTrue(faltaPort.consultarEstudiante(UUID.fromString(otroUsuario), 1, 50).filas().stream().noneMatch(f -> f.idSesion().equals(sesion)));
    }

    @Test
    void el_perfil_incorrecto_responde_sec_001_y_la_pagina_invalida_pag_001() {
        final UUID docente = fixture.docenteUsuarioId();
        assertThrows(ForbiddenException.class, () -> faltaPort.consultarEstudiante(docente, 1, 20));

        final String estudiante = jdbcTemplate.queryForObject("SELECT CAST(usuario AS NVARCHAR(36)) FROM dbo.Estudiante WHERE id = ?", String.class,
                fixture.estudiantes().getFirst().toString());
        final UUID usuario = UUID.fromString(estudiante);
        assertThrows(ValidationException.class, () -> faltaPort.consultarEstudiante(usuario, 0, 20));
        assertThrows(ValidationException.class, () -> faltaPort.consultarEstudiante(usuario, 1, 100000));
    }
}
