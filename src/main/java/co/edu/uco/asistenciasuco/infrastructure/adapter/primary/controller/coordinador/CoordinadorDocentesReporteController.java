package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.ConsultarFichaDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.ConsultarResumenDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Reportes del directorio docente del coordinador autenticado: ficha de un docente con su historial de grupos y resumen
 * de carga (HU094) y consolidado del directorio por estado y tipo de vinculacion (HU095).
 */
@RestController
@RequestMapping("/api/v1/coordinador/docentes")
public final class CoordinadorDocentesReporteController {

    private static final int TAMANO_MAXIMO = 100;

    private final ConsultarFichaDocenteInputPort ficha;
    private final ConsultarResumenDocentesInputPort resumen;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorDocentesReporteController(final ConsultarFichaDocenteInputPort ficha,
                                                final ConsultarResumenDocentesInputPort resumen,
                                                final AuthenticatedUserResolver authenticatedUserResolver) {
        this.ficha = Objects.requireNonNull(ficha, "ConsultarFichaDocenteInputPort es obligatorio.");
        this.resumen = Objects.requireNonNull(resumen, "ConsultarResumenDocentesInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiDataResponse<ResumenDocentesProgramaDTO>> consultarResumen() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, resumen.execute(authenticatedUserResolver.requireAuthenticatedUserId())));
    }

    @GetMapping("/{idDocente}/ficha")
    public ResponseEntity<ApiDataResponse<FichaDocenteDTO>> consultarFicha(
            @PathVariable("idDocente") final UUID idDocente,
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "10") final int tamano
    ) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (pagina < 1) {
            builder.add("pagina", ValidationErrorType.OUT_OF_RANGE, "La pagina debe ser mayor o igual a 1.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            builder.add("tamano", ValidationErrorType.OUT_OF_RANGE, "El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        RequestValidationGuard.validate(builder.build());

        return ResponseEntity.ok(new ApiDataResponse<>(true, ficha.execute(new ConsultarFichaDocenteDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idDocente, idPeriodoAcademico, pagina, tamano))));
    }
}
