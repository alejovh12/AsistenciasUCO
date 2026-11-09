package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.ActualizarFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.CambiarEstadoDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.CambiarEstadoFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.ConsultarHistorialCierresMasivosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F-ADM-006: endpoint GET /api/v1/admin/cierre-masivo/historial para reportes historicos de cierre semestral. */
class AdminPortalCierreMasivoHistorialControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final ConsultarDecanosInputPort consultarDecanos = mock(ConsultarDecanosInputPort.class);
    private final CrearDecanoInputPort crearDecano = mock(CrearDecanoInputPort.class);
    private final EjecutarCierreMasivoInputPort ejecutarCierreMasivo = mock(EjecutarCierreMasivoInputPort.class);
    private final ConsultarInstitucionesInputPort consultarInstituciones = mock(ConsultarInstitucionesInputPort.class);
    private final ConsultarFacultadesInputPort consultarFacultades = mock(ConsultarFacultadesInputPort.class);
    private final CrearFacultadInputPort crearFacultad = mock(CrearFacultadInputPort.class);
    private final ActualizarFacultadInputPort actualizarFacultad = mock(ActualizarFacultadInputPort.class);
    private final CambiarEstadoFacultadInputPort cambiarEstadoFacultad = mock(CambiarEstadoFacultadInputPort.class);
    private final ConsultarAreasInputPort consultarAreas = mock(ConsultarAreasInputPort.class);
    private final CambiarEstadoDecanoInputPort cambiarEstadoDecano = mock(CambiarEstadoDecanoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final ConsultarHistorialCierresMasivosInputPort historialInputPort = mock(ConsultarHistorialCierresMasivosInputPort.class);

    private final AdminPortalController controller = new AdminPortalController(
            consultarDecanos, crearDecano, ejecutarCierreMasivo, consultarInstituciones,
            consultarFacultades, crearFacultad, actualizarFacultad, cambiarEstadoFacultad,
            consultarAreas, cambiarEstadoDecano, identity, historialInputPort);

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void consultarHistorialCierre_retorna_lista_de_reportes() throws Exception {
        final OffsetDateTime fecha = OffsetDateTime.parse("2026-11-08T02:00:00Z");
        when(historialInputPort.execute()).thenReturn(List.of(new CierrePeriodoReporteDTO(
                ID, "2026-2", fecha, 150, 12, 135, 15, "COMPLETADO", "ADMIN_SISTEMA")));

        mvc.perform(get("/api/v1/admin/cierre-masivo/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].periodoCodigo").value("2026-2"))
                .andExpect(jsonPath("$.datos[0].totalEstudiantesProcesados").value(150))
                .andExpect(jsonPath("$.datos[0].totalReprobadosFallas").value(15))
                .andExpect(jsonPath("$.datos[0].estado").value("COMPLETADO"));
    }
}
