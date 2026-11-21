package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.PrevisualizarHorarioGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizacionHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper.GrupoHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.PrevisualizarHorarioGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * SES-GEN-01: previsualizacion, de solo lectura, del calendario de sesiones de un horario propuesto para un grupo nuevo o existente. Es un
 * BUSINESS_COMMAND de lectura (el resultado se calcula de un cuerpo estructurado, no es un recurso guardado), por eso POST y 200 (no 201):
 * no crea nada. Solo el COORDINADOR ({@code /api/v1/coordinador/**}); su ambito sobre los programas de la asignatura o del grupo lo fuerza la DB,
 * igual que en las escrituras. La confirmacion (POST/PUT {@code /api/v1/grupos}) vuelve a validar todo.
 */
@RestController
@RequestMapping("/api/v1/coordinador/grupos")
public final class PrevisualizacionHorarioGrupoController {

    private final PrevisualizarHorarioGrupoInputPort previsualizarHorarioGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PrevisualizacionHorarioGrupoController(
            final PrevisualizarHorarioGrupoInputPort previsualizarHorarioGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.previsualizarHorarioGrupoInputPort = Objects.requireNonNull(previsualizarHorarioGrupoInputPort,
                "PrevisualizarHorarioGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/previsualizacion-horario")
    public ResponseEntity<ApiDataResponse<PrevisualizacionHorarioGrupoDTO>> previsualizar(
            @Valid @RequestBody final PrevisualizarHorarioGrupoRequest request
    ) {
        final var resultado = previsualizarHorarioGrupoInputPort.execute(
                GrupoHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }
}
