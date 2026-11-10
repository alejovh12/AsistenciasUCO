package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudAprobada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudRechazada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DEC-065 (2), contrato JPA/SP con SQL Server real: la sesion que crea el docente queda como solicitud PENDIENTE (mensaje SES_022 y
 * estado visible en las lecturas v1 y v2), el coordinador del programa la consulta en su bandeja, ve los destinatarios (docente y
 * estudiantes), la aprueba (SES_026; un segundo intento es SES_024) o la rechaza con un motivo del catalogo (SES_027, sesion cancelada).
 * Las sesiones de prueba se eliminan al terminar.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionSolicitudSqlServerIT {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;
    @Autowired
    private SesionManualRepositoryPort manualPort;
    @Autowired
    private SesionProcedenciaQueryPort procedenciaPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AttendanceCommandParityFixture fixture;
    private final List<UUID> sesiones = new ArrayList<>();

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        fixture.prepare();
    }

    @AfterEach
    void limpiar() {
        try {
            for (final UUID sesion : sesiones) {
                jdbcTemplate.update("DELETE FROM dbo.SesionSolicitud WHERE sesion = ?", sesion.toString());
                jdbcTemplate.update("DELETE FROM dbo.SesionCancelacion WHERE sesion = ?", sesion.toString());
                jdbcTemplate.update("DELETE FROM dbo.Sesion WHERE id = ?", sesion.toString());
            }
            fixture.cleanup();
        } finally {
            sesiones.clear();
            CorrelationIdContext.clear();
        }
    }

    @Test
    void el_docente_crea_una_solicitud_pendiente_y_el_coordinador_la_aprueba_una_sola_vez() {
        final UUID coordinador = coordinadorDelGrupo();
        final String nombre = "IT-DEC065-A-" + UUID.randomUUID().toString().substring(0, 8);

        final String mensaje = crearComoDocente(nombre);
        assertTrue(mensaje.toLowerCase().contains("solicitud"), "SES_022 debe indicar que quedo como solicitud: " + mensaje);
        final SesionProcedenciaRepositoryProjection pendiente = localizar(nombre);
        sesiones.add(pendiente.sesion());
        assertEquals("PENDIENTE", pendiente.estadoSolicitud());
        assertTrue(pendiente.pendienteAprobacion());
        final var v1 = sesionRepositoryPort.consultarSesion(new ConsultarSesionRepositoryDTO(pendiente.sesion()));
        assertEquals("PENDIENTE", v1.getEstadoSolicitud());
        assertTrue(v1.isPendienteAprobacion());

        final List<DestinatarioSesionManual> alCrear = manualPort.consultarDestinatarios(pendiente.sesion(), fixture.docenteUsuarioId());
        assertTrue(alCrear.stream().anyMatch(d -> DestinatarioSesionManual.TIPO_COORDINADOR.equals(d.tipo())),
                "El aviso de la solicitud necesita al menos un coordinador destinatario");

        final PaginaSolicitudesSesion bandeja = manualPort.consultarSolicitudes(coordinador, true, null, fixture.grupoId(), 1, 50);
        assertTrue(bandeja.elementos().stream().anyMatch(s -> s.idSesion().equals(pendiente.sesion()) && "PENDIENTE".equals(s.estadoSolicitud())));
        assertTrue(bandeja.total() >= 1);

        final List<DestinatarioSesionManual> destinatarios = manualPort.consultarDestinatariosSolicitud(pendiente.sesion(), coordinador);
        assertTrue(destinatarios.stream().anyMatch(d -> DestinatarioSesionManual.TIPO_DOCENTE.equals(d.tipo())));

        final SolicitudAprobada aprobada = manualPort.aprobarSolicitud(pendiente.sesion(), "Adelante", coordinador);
        assertEquals(pendiente.sesion(), aprobada.idSesion());
        assertEquals(fixture.grupoId(), aprobada.idGrupo());
        assertNotNull(aprobada.mensajeUsuario());
        assertFalse(aprobada.mensajeUsuario().isBlank());

        final SesionProcedenciaRepositoryProjection aprobadaFila = procedenciaPort.consultarSesion(pendiente.sesion()).orElseThrow();
        assertEquals("APROBADA", aprobadaFila.estadoSolicitud());
        assertFalse(aprobadaFila.pendienteAprobacion());
        assertNull(aprobadaFila.cancelacion());

        final ConflictException repetida = assertThrows(ConflictException.class,
                () -> manualPort.aprobarSolicitud(pendiente.sesion(), null, coordinador));
        assertEquals("SES_024", repetida.getCode());
    }

    @Test
    void el_coordinador_rechaza_la_solicitud_con_un_motivo_del_catalogo_y_la_sesion_queda_cancelada() {
        final UUID coordinador = coordinadorDelGrupo();
        final String nombre = "IT-DEC065-R-" + UUID.randomUUID().toString().substring(0, 8);
        crearComoDocente(nombre);
        final SesionProcedenciaRepositoryProjection pendiente = localizar(nombre);
        sesiones.add(pendiente.sesion());
        final List<MotivoCancelacion> motivos = manualPort.consultarMotivosCancelacion(coordinador);
        assumeTrue(!motivos.isEmpty(), "El catalogo de motivos de cancelacion esta vacio.");

        final SolicitudRechazada rechazada = manualPort.rechazarSolicitud(pendiente.sesion(), motivos.getFirst().codigo(), "IT rechazo", coordinador);

        assertEquals(pendiente.sesion(), rechazada.idSesion());
        assertNotNull(rechazada.fechaCancelacionUtc());
        assertNotNull(rechazada.mensajeUsuario());
        final SesionProcedenciaRepositoryProjection fila = procedenciaPort.consultarSesion(pendiente.sesion()).orElseThrow();
        assertEquals("RECHAZADA", fila.estadoSolicitud());
        assertFalse(fila.pendienteAprobacion());
        assertNotNull(fila.cancelacion(), "La sesion rechazada queda cancelada");
        final PaginaSolicitudesSesion historial = manualPort.consultarSolicitudes(coordinador, false, null, fixture.grupoId(), 1, 50);
        assertTrue(historial.elementos().stream().anyMatch(s -> s.idSesion().equals(pendiente.sesion()) && "RECHAZADA".equals(s.estadoSolicitud())));
        assertFalse(manualPort.consultarSolicitudes(coordinador, true, null, fixture.grupoId(), 1, 50).elementos().stream()
                .anyMatch(s -> s.idSesion().equals(pendiente.sesion())));

        final ConflictException repetida = assertThrows(ConflictException.class,
                () -> manualPort.rechazarSolicitud(pendiente.sesion(), motivos.getFirst().codigo(), null, coordinador));
        assertTrue(List.of("SES_024", "SES_011").contains(repetida.getCode()), "Rechazar dos veces es un conflicto: " + repetida.getCode());
    }

    @Test
    void un_docente_no_puede_aprobar_ni_consultar_la_bandeja() {
        final String nombre = "IT-DEC065-D-" + UUID.randomUUID().toString().substring(0, 8);
        crearComoDocente(nombre);
        final SesionProcedenciaRepositoryProjection pendiente = localizar(nombre);
        sesiones.add(pendiente.sesion());

        assertThrows(RuntimeException.class, () -> manualPort.aprobarSolicitud(pendiente.sesion(), null, fixture.docenteUsuarioId()));
        assertThrows(RuntimeException.class,
                () -> manualPort.consultarSolicitudes(fixture.docenteUsuarioId(), true, null, null, 1, 20));
    }

    private String crearComoDocente(final String nombre) {
        // Dias a futuro con hora aleatoria: evita el solapamiento (SES_017) con las sesiones del seed y de otras pruebas.
        final LocalDateTime inicio = LocalDateTime.now(ZoneOffset.UTC).plusDays(300 + RANDOM.nextInt(300)).withMinute(0).withSecond(0).withNano(0)
                .withHour(6 + RANDOM.nextInt(14));
        return sesionRepositoryPort.crearSesion(new CrearSesionRepositoryDTO(
                fixture.grupoId(), nombre, inicio, inicio.plusHours(1), fixture.docenteUsuarioId(), ContratoHorarioSesion.UTC_CONFIRMADO_V2));
    }

    private SesionProcedenciaRepositoryProjection localizar(final String nombre) {
        return procedenciaPort.consultarSesionesPorGrupo(fixture.grupoId()).stream()
                .filter(s -> nombre.equals(s.nombre()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontro la sesion creada " + nombre));
    }

    /** Usuario del coordinador de algun programa del grupo del fixture; sin coordinador en el seed la prueba se omite. */
    private UUID coordinadorDelGrupo() {
        final List<String> usuarios = jdbcTemplate.query("""
                SELECT TOP 1 CAST(c.usuario AS NVARCHAR(36)) AS usuario
                FROM dbo.Programa p
                INNER JOIN dbo.Coordinador c ON c.id = p.coordinador
                CROSS APPLY dbo.ufn_programas_grupo(?) pg
                WHERE pg.idPrograma = p.id""", (rs, n) -> rs.getString("usuario"), fixture.grupoId().toString());
        assumeTrue(!usuarios.isEmpty(), "El grupo del fixture no tiene un programa con coordinador.");
        return UUID.fromString(usuarios.getFirst());
    }
}
