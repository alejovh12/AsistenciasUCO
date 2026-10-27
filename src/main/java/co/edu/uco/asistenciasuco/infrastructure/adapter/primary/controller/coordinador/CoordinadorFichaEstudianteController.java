package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.ConsultarBitacoraEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.ConsultarFichaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bitacora cronologica y ficha integral de un estudiante del programa del coordinador autenticado (HU067, HU069). */
@RestController
@RequestMapping("/api/v1/coordinador/estudiantes")
public final class CoordinadorFichaEstudianteController {

    private final ConsultarBitacoraEstudianteInputPort bitacora;
    private final ConsultarFichaEstudianteInputPort ficha;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorFichaEstudianteController(final ConsultarBitacoraEstudianteInputPort bitacora, final ConsultarFichaEstudianteInputPort ficha,
                                                final AuthenticatedUserResolver authenticatedUserResolver) {
        this.bitacora = Objects.requireNonNull(bitacora, "ConsultarBitacoraEstudianteInputPort es obligatorio.");
        this.ficha = Objects.requireNonNull(ficha, "ConsultarFichaEstudianteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/{idEstudiante}/bitacora")
    public ResponseEntity<ApiListResponse<BitacoraSesionDTO>> consultarBitacora(
            @PathVariable("idEstudiante") final UUID idEstudiante,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano,
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        final BitacoraPaginaDTO resultado = bitacora.execute(new ConsultarBitacoraEstudianteDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idEstudiante, idPeriodoAcademico, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.sesiones(), resultado.totalRegistros()));
    }

    @GetMapping("/{idEstudiante}/ficha")
    public ResponseEntity<ApiDataResponse<FichaEstudianteDTO>> consultarFicha(
            @PathVariable("idEstudiante") final UUID idEstudiante,
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                ficha.execute(new ConsultarFichaEstudianteDTO(authenticatedUserResolver.requireAuthenticatedUserId(), idEstudiante, idPeriodoAcademico))));
    }
}
