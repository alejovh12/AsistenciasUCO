package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort.SesionCalendario;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ATA D6 y D7, contrato JPA/SP con SQL Server real: la solicitud que crea el docente aparece en su lista (pendiente) y NO en su
 * calendario hasta que el coordinador la aprueba; aprobada, el calendario la trae como extraordinaria y la lista del docente la
 * muestra con estado APROBADA. Los rangos invalidos responden {@code CAL_001} y el perfil incorrecto {@code SEC_001}. Las sesiones
 * de prueba se eliminan al terminar.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionCalendarioSqlServerIT {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;
    @Autowired
    private SesionManualRepositoryPort manualPort;
    @Autowired
    private SesionCalendarioRepositoryPort calendarioPort;
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
    void la_solicitud_pendiente_va_a_la_lista_del_docente_y_solo_entra_al_calendario_al_aprobarse() {
        final UUID coordinador = coordinadorDelGrupo();
        // DEC-072: la sesion empieza y termina el mismo dia LOCAL; la hora se elige en la zona institucional y se guarda en UTC.
        final ZoneId zona = ZoneId.of(zonaIana());
        final LocalDate dia = LocalDate.now(zona).plusDays(300 + RANDOM.nextInt(300));
        final LocalDateTime inicio = dia.atTime(6 + RANDOM.nextInt(12), 0).atZone(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        final String nombre = "IT-ATA-D6D7-" + UUID.randomUUID().toString().substring(0, 8);
        sesionRepositoryPort.crearSesion(new CrearSesionRepositoryDTO(
                fixture.grupoId(), nombre, inicio, inicio.plusHours(1), fixture.docenteUsuarioId(), ContratoHorarioSesion.UTC_CONFIRMADO_V2));
        final SesionProcedenciaRepositoryProjection creada = procedenciaPort.consultarSesionesPorGrupo(fixture.grupoId()).stream()
                .filter(s -> nombre.equals(s.nombre())).findFirst().orElseThrow(() -> new AssertionError("No se encontro " + nombre));
        sesiones.add(creada.sesion());

        // D6: la lista del docente trae la pendiente (pendientes y todas) con las mismas columnas que la bandeja del coordinador
        final PaginaSolicitudesSesion pendientes = manualPort.consultarSolicitudesDocente(fixture.docenteUsuarioId(), true, fixture.grupoId(), 1, 50);
        final SolicitudSesion fila = pendientes.elementos().stream().filter(s -> s.idSesion().equals(creada.sesion())).findFirst()
                .orElseThrow(() -> new AssertionError("La solicitud pendiente debe estar en la lista del docente"));
        assertEquals("PENDIENTE", fila.estadoSolicitud());
        assertEquals(inicio, fila.fechaHoraInicio());
        assertEquals(fixture.grupoId(), fila.idGrupo());
        assertTrue(pendientes.total() >= 1);
        assertTrue(manualPort.consultarSolicitudesDocente(fixture.otroDocenteUsuarioId(), false, null, 1, 50).elementos().stream()
                .noneMatch(s -> s.idSesion().equals(creada.sesion())), "otro docente no ve solicitudes ajenas");

        // D7: pendiente = no esta en el calendario
        assertTrue(calendarioPort.consultarDocente(fixture.docenteUsuarioId(), dia, dia).stream()
                .noneMatch(s -> s.idSesion().equals(creada.sesion())));

        // aprobada: el calendario la trae como extraordinaria y la lista del docente la muestra APROBADA
        manualPort.aprobarSolicitud(creada.sesion(), null, coordinador);
        final SesionCalendario enCalendario = calendarioPort.consultarDocente(fixture.docenteUsuarioId(), dia, dia).stream()
                .filter(s -> s.idSesion().equals(creada.sesion())).findFirst()
                .orElseThrow(() -> new AssertionError("La sesion aprobada debe estar en el calendario del docente"));
        assertEquals(inicio, enCalendario.fechaHoraInicioUtc());
        assertEquals(inicio.plusHours(1), enCalendario.fechaHoraFinUtc());
        assertTrue(enCalendario.esExtraordinaria());
        assertFalse(enCalendario.estaCancelada());
        assertEquals(fixture.grupoId(), enCalendario.idGrupo());
        assertEquals(fixture.docenteId(), enCalendario.idDocente());
        assertTrue(manualPort.consultarSolicitudesDocente(fixture.docenteUsuarioId(), true, fixture.grupoId(), 1, 50).elementos().stream()
                .noneMatch(s -> s.idSesion().equals(creada.sesion())));
        assertEquals("APROBADA", manualPort.consultarSolicitudesDocente(fixture.docenteUsuarioId(), false, fixture.grupoId(), 1, 50).elementos()
                .stream().filter(s -> s.idSesion().equals(creada.sesion())).findFirst().orElseThrow().estadoSolicitud());
    }

    @Test
    void los_rangos_invalidos_responden_cal_001_y_el_perfil_incorrecto_sec_001() {
        final LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        final UUID docente = fixture.docenteUsuarioId();

        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> calendarioPort.consultarDocente(docente, hoy.plusDays(1), hoy)).getCode());
        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> calendarioPort.consultarDocente(docente, hoy, hoy.plusDays(31))).getCode());
        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> calendarioPort.consultarDocente(docente, null, hoy)).getCode());
        assertTrue(calendarioPort.consultarDocente(docente, hoy, hoy.plusDays(30)).size() >= 0);

        assertThrows(ForbiddenException.class, () -> calendarioPort.consultarEstudiante(docente, hoy, hoy.plusDays(6)));
        assertThrows(ForbiddenException.class, () -> manualPort.consultarSolicitudes(docente, true, null, null, 1, 20));
        assertThrows(ValidationException.class, () -> manualPort.consultarSolicitudesDocente(docente, true, null, 0, 20));
    }

    private String zonaIana() {
        return jdbcTemplate.queryForObject("SELECT valor FROM dbo.uv_parametro WHERE grupo = 'TIEMPO' AND clave = 'ZONA_HORARIA_IANA'", String.class);
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
