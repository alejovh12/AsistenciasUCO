package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.ActualizarSemestreEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ActualizarSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ResultadoSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.ActualizarSemestreEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataMensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/** El coordinador registra el semestre que cursa un estudiante en uno de sus programas (migracion 63 de la BD). */
@RestController
public final class CoordinadorEstudianteSemestreController {

    private final ActualizarSemestreEstudianteInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorEstudianteSemestreController(final ActualizarSemestreEstudianteInputPort inputPort,
                                                   final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort, "ActualizarSemestreEstudianteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @AuditableOperation(action = "ACTUALIZAR_SEMESTRE_ESTUDIANTE", resourceType = "ESTUDIANTE", resourceIdPathVariable = "idEstudiante", metadataRequestFields = {"semestreActual"})
    @PutMapping("/api/v1/coordinador/estudiantes/{idEstudiante}/programas/{idPrograma}/semestre")
    public ResponseEntity<ApiDataMensajeResponse<ActualizarSemestreEstudianteRequest>> actualizarSemestre(
            @PathVariable("idEstudiante") final UUID idEstudiante,
            @PathVariable("idPrograma") final UUID idPrograma,
            @Valid @RequestBody final ActualizarSemestreEstudianteRequest request
    ) {
        final ResultadoSemestreEstudianteDTO resultado = inputPort.execute(new ActualizarSemestreEstudianteDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idEstudiante, idPrograma, request.getSemestreActual()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, request, resultado.mensajeUsuario()));
    }
}
