package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.Comando;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.ConsultarCalendarioDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.ConsultarCalendarioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.exception.CalendarioErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.DocenteSesionCalendarioController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.EstudianteSesionCalendarioController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ATA D7: {@code GET /api/v1/estudiante/sesiones/calendario} y {@code GET /api/v1/docente/sesiones/calendario} entregan la lista de
 * sesiones con horas UTC ({@code Z}); {@code CAL_001} es 400 y el perfil incorrecto 403.
 */
class SesionCalendarioControllersTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String RUTA_ESTUDIANTE = "/api/v1/estudiante/sesiones/calendario";
    private static final String RUTA_DOCENTE = "/api/v1/docente/sesiones/calendario";
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 16);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 22);

    private final ConsultarCalendarioEstudianteInputPort estudiante = mock(ConsultarCalendarioEstudianteInputPort.class);
    private final ConsultarCalendarioDocenteInputPort docente = mock(ConsultarCalendarioDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new EstudianteSesionCalendarioController(estudiante, identity), new DocenteSesionCalendarioController(docente, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static SesionCalendario sesion(final UUID id) {
        return new SesionCalendario(id, UUID.fromString("22222222-2222-2222-2222-222222222222"), 3, "G03",
                UUID.fromString("33333333-3333-3333-3333-333333333333"), "MAT101", "Calculo", UUID.randomUUID(), "Maria Docente",
                Instant.parse("2026-11-16T13:00:00Z"), Instant.parse("2026-11-16T15:00:00Z"), true, true, "Dia festivo", true, "Festivo X");
    }

    @Test
    void estudiante_devuelve_las_sesiones_con_el_contrato_exacto() throws Exception {
        final UUID sesion = UUID.randomUUID();
        when(estudiante.execute(new Comando(ACTOR, DESDE, HASTA))).thenReturn(List.of(sesion(sesion)));

        mvc.perform(get(RUTA_ESTUDIANTE).param("desde", "2026-11-16").param("hasta", "2026-11-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos[0].idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.datos[0].idGrupo").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.datos[0].codigoGrupo").value(3))
                .andExpect(jsonPath("$.datos[0].nombreGrupo").value("G03"))
                .andExpect(jsonPath("$.datos[0].idAsignatura").value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.datos[0].codigoAsignatura").value("MAT101"))
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.datos[0].nombreDocente").value("Maria Docente"))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value("2026-11-16T13:00:00Z"))
                .andExpect(jsonPath("$.datos[0].fechaHoraFinUtc").value("2026-11-16T15:00:00Z"))
                .andExpect(jsonPath("$.datos[0].esExtraordinaria").value(true))
                .andExpect(jsonPath("$.datos[0].estaCancelada").value(true))
                .andExpect(jsonPath("$.datos[0].motivoCancelacion").value("Dia festivo"))
                .andExpect(jsonPath("$.datos[0].esCancelacionAutomatica").value(true))
                .andExpect(jsonPath("$.datos[0].nombreDiaFestivo").value("Festivo X"));
    }

    @Test
    void docente_devuelve_las_sesiones_y_los_nulos_salen_explicitos() throws Exception {
        when(docente.execute(new Comando(ACTOR, DESDE, HASTA))).thenReturn(List.of(new SesionCalendario(UUID.randomUUID(), UUID.randomUUID(),
                null, null, UUID.randomUUID(), null, null, UUID.randomUUID(), null, null, null, false, false, null, false, null)));

        mvc.perform(get(RUTA_DOCENTE).param("desde", "2026-11-16").param("hasta", "2026-11-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].esExtraordinaria").value(false))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value(nullValue()))
                .andExpect(jsonPath("$.datos[0].motivoCancelacion").value(nullValue()))
                .andExpect(jsonPath("$.datos[0].nombreDiaFestivo").value(nullValue()));
    }

    @Test
    void sin_fechas_delega_nulos_para_que_la_db_responda_cal_001_con_400() throws Exception {
        when(estudiante.execute(new Comando(ACTOR, null, null))).thenThrow(new ValidationException(CalendarioErrorCode.CAL_001));
        when(docente.execute(new Comando(ACTOR, null, null))).thenThrow(new ValidationException(CalendarioErrorCode.CAL_001));

        mvc.perform(get(RUTA_ESTUDIANTE)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CAL_001"));
        mvc.perform(get(RUTA_DOCENTE)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CAL_001"));
    }

    @Test
    void una_fecha_con_formato_invalido_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        mvc.perform(get(RUTA_ESTUDIANTE).param("desde", "16/11/2026").param("hasta", "2026-11-22")).andExpect(status().isBadRequest());
        mvc.perform(get(RUTA_DOCENTE).param("desde", "2026-11-16").param("hasta", "hoy")).andExpect(status().isBadRequest());
    }

    @Test
    void el_perfil_incorrecto_responde_403() throws Exception {
        when(estudiante.execute(any())).thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));
        when(docente.execute(any())).thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));

        mvc.perform(get(RUTA_ESTUDIANTE).param("desde", "2026-11-16").param("hasta", "2026-11-22")).andExpect(status().isForbidden());
        mvc.perform(get(RUTA_DOCENTE).param("desde", "2026-11-16").param("hasta", "2026-11-22")).andExpect(status().isForbidden());
    }

    @Test
    void los_constructores_exigen_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new EstudianteSesionCalendarioController(null, identity));
        assertThrows(NullPointerException.class, () -> new EstudianteSesionCalendarioController(estudiante, null));
        assertThrows(NullPointerException.class, () -> new DocenteSesionCalendarioController(null, identity));
        assertThrows(NullPointerException.class, () -> new DocenteSesionCalendarioController(docente, null));
    }
}
