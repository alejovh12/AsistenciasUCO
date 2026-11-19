package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.FaltaReclamable;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Resultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.EstudianteFaltasReclamablesController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
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
 * ATA D10: {@code GET /api/v1/estudiante/faltas-reclamables} entrega la pagina de tomas reclamables con horas UTC ({@code Z}); acepta los
 * parametros {@code numeroPagina}/{@code tamanoPagina} (addendum J.2) y los alias {@code pagina}/{@code tamano}; el perfil incorrecto es 403
 * y la pagina invalida 400.
 */
class EstudianteFaltasReclamablesControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/estudiante/faltas-reclamables";
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TOMA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final ConsultarFaltasReclamablesInputPort consultar = mock(ConsultarFaltasReclamablesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new EstudianteFaltasReclamablesController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static FaltaReclamable falta() {
        return new FaltaReclamable(GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, Instant.parse("2026-11-17T13:00:00Z"),
                Instant.parse("2026-11-17T15:00:00Z"), TOMA, 2, Instant.parse("2026-11-17T13:40:00Z"), Instant.parse("2026-11-23T05:00:00Z"));
    }

    @Test
    void devuelve_la_pagina_con_el_contrato_exacto() throws Exception {
        when(consultar.execute(new Comando(ACTOR, 2, 10))).thenReturn(new Resultado(List.of(falta()), 2, 10, 11));

        mvc.perform(get(RUTA).param("numeroPagina", "2").param("tamanoPagina", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(10))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.elementos[0].idGrupo").value(GRUPO.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreGrupo").value("G01"))
                .andExpect(jsonPath("$.elementos[0].idAsignatura").value(ASIGNATURA.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.elementos[0].idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.elementos[0].numeroSesion").value(4))
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicioUtc").value("2026-11-17T13:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaHoraFinUtc").value("2026-11-17T15:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.elementos[0].numeroToma").value(2))
                .andExpect(jsonPath("$.elementos[0].fechaPuntoControlUtc").value("2026-11-17T13:40:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaLimiteReclamoUtc").value("2026-11-23T05:00:00Z"));
    }

    @Test
    void sin_parametros_delega_nulos_y_los_alias_pagina_y_tamano_se_aceptan() throws Exception {
        when(consultar.execute(new Comando(ACTOR, null, null))).thenReturn(new Resultado(List.of(), 1, 20, 0));
        when(consultar.execute(new Comando(ACTOR, 3, 5))).thenReturn(new Resultado(List.of(), 3, 5, 0));

        mvc.perform(get(RUTA)).andExpect(status().isOk()).andExpect(jsonPath("$.elementos").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0)).andExpect(jsonPath("$.totalPaginas").value(0));
        mvc.perform(get(RUTA).param("pagina", "3").param("tamano", "5")).andExpect(status().isOk()).andExpect(jsonPath("$.numeroPagina").value(3));
    }

    @Test
    void los_nulos_de_hora_salen_explicitos() throws Exception {
        when(consultar.execute(any())).thenReturn(new Resultado(List.of(new FaltaReclamable(GRUPO, null, ASIGNATURA, null, SESION, 1, null, null,
                TOMA, 1, null, null)), 1, 20, 1));

        mvc.perform(get(RUTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicioUtc").value(nullValue()))
                .andExpect(jsonPath("$.elementos[0].fechaLimiteReclamoUtc").value(nullValue()));
    }

    @Test
    void el_perfil_incorrecto_responde_403_y_la_pagina_invalida_400() throws Exception {
        when(consultar.execute(new Comando(ACTOR, null, null))).thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));
        when(consultar.execute(new Comando(ACTOR, 0, 20))).thenThrow(new ValidationException("PAG_001", "Pagina invalida."));

        mvc.perform(get(RUTA)).andExpect(status().isForbidden());
        mvc.perform(get(RUTA).param("numeroPagina", "0").param("tamanoPagina", "20")).andExpect(status().isBadRequest());
    }

    @Test
    void un_parametro_no_numerico_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        mvc.perform(get(RUTA).param("numeroPagina", "uno")).andExpect(status().isBadRequest());
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new EstudianteFaltasReclamablesController(null, identity));
        assertThrows(NullPointerException.class, () -> new EstudianteFaltasReclamablesController(consultar, null));
    }
}
