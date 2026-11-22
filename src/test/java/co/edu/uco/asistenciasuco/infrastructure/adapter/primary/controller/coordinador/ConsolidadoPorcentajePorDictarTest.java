package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ConsolidadoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor.ConsultasTransversalesInteractor;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ConsolidadoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revision final BD-8: el consolidado entrega al final porcentajePorDictar (lo que aun no se dicta), de modo que asistencia + inasistencia
 * + excusa + sinRegistro + porDictar = 100. El backend lo propaga sin recalcular, para coordinador y decano.
 */
class ConsolidadoPorcentajePorDictarTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    @Test
    void el_interactor_propaga_porcentaje_por_dictar_de_la_proyeccion_al_dto() {
        final ConsultasTransversalesUseCase useCase = mock(ConsultasTransversalesUseCase.class);
        when(useCase.consultarConsolidadoAsistencia(ACTOR, new FiltroConsolidado(ID, null, null, null), 1, 10))
                .thenReturn(new Pagina<>(List.of(new ConsolidadoGrupoProjection(ID, 1, "G1", ID, "2026-2", ID,
                        "Sistemas", ID, "Calculo", ID, "Ana", 20, 8, 150, 10, 0, new BigDecimal("60.00"),
                        new BigDecimal("6.25"), 2, 1, new BigDecimal("12.00"), new BigDecimal("8.50"), new BigDecimal("0.00"), "RIESGO",
                        new BigDecimal("25.25"))), 1));

        final var grupo = new ConsultasTransversalesInteractor(useCase)
                .consultarConsolidadoAsistencia(ACTOR, new FiltroConsolidadoDTO(ID, null, null, null), 1, 10).elementos().getFirst();

        assertEquals(new BigDecimal("25.25"), grupo.porcentajePorDictar());
    }

    @Test
    void la_forma_previa_del_dto_deja_porcentaje_por_dictar_nulo() {
        final var previo = new ConsolidadoGrupoDTO(ID, 1, "G1", ID, "2026-2", ID, "S", ID, "C", ID, "Ana", 0, 0, 0, 0, 0, null, null, 0, 0,
                null, null, null, "REGULAR");

        assertNull(previo.porcentajePorDictar());
    }

    @Test
    void el_json_del_consolidado_de_coordinador_y_decano_trae_porcentaje_por_dictar() throws Exception {
        final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
        final AuthenticatedUserResolver identity = () -> ACTOR;
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConsolidadoAsistenciaController(inputPort, identity))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();
        when(inputPort.consultarConsolidadoAsistencia(ACTOR, new FiltroConsolidadoDTO(ID, null, null, null), 1, 20))
                .thenReturn(new PaginaDTO<>(List.of(new ConsolidadoGrupoDTO(ID, 1, "G1", ID, "2026-2", ID, "Sistemas", ID, "Calculo", ID,
                        "Ana", 20, 8, 150, 10, 0, new BigDecimal("60.00"), new BigDecimal("6.25"), 2, 1, new BigDecimal("12.00"),
                        new BigDecimal("8.50"), new BigDecimal("0.00"), "RIESGO", new BigDecimal("25.25"))), 1));

        for (final String ruta : List.of("/api/v1/coordinador/consolidado-asistencia", "/api/v1/decano/consolidado-asistencia")) {
            mvc.perform(get(ruta).param("idPeriodoAcademico", ID.toString()).param("pagina", "1").param("tamano", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos[0].porcentajePorDictar").value(25.25));
        }
    }
}
