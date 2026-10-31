package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo;

import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.ReenviarVerificacionCorreoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: reenvio del enlace de verificacion de correo a un estudiante registrado manualmente. El ambito (docente
 * titular del grupo o coordinador de su programa), la espera minima y el estado del correo los decide la base de datos;
 * un estudiante ajeno responde 404 sin distinguir el caso.
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoVerificacionCorreoController {

    private final ReenviarVerificacionCorreoInputPort reenviarVerificacionCorreoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GrupoVerificacionCorreoController(
            final ReenviarVerificacionCorreoInputPort reenviarVerificacionCorreoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.reenviarVerificacionCorreoInputPort = Objects.requireNonNull(reenviarVerificacionCorreoInputPort,
                "ReenviarVerificacionCorreoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/{grupoId}/estudiantes/{estudianteId}/reenviar-verificacion")
    public ResponseEntity<ApiDataResponse<Void>> reenviarVerificacion(
            @PathVariable final UUID grupoId,
            @PathVariable final UUID estudianteId
    ) {
        reenviarVerificacionCorreoInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId(), grupoId, estudianteId);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
