package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudiantePortalController {

    private final ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort;
    private final ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort;
    private final ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public EstudiantePortalController(
            final ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort,
            final ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort,
            final ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort,
            final AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.consultarMateriasEstudianteInputPort = consultarMateriasEstudianteInputPort;
        this.consultarHorariosEstudianteInputPort = consultarHorariosEstudianteInputPort;
        this.consultarSesionesMateriaEstudianteInputPort = consultarSesionesMateriaEstudianteInputPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/materias")
    public ResponseEntity<ApiListResponse<MateriaEstudianteDTO>> consultarMaterias() {
        final var data = consultarMateriasEstudianteInputPort.execute(authenticatedUserProvider.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<HorarioEstudianteDTO>> consultarHorarios() {
        final var data = consultarHorariosEstudianteInputPort.execute(authenticatedUserProvider.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/materias/{materiaId}/sesiones")
    public ResponseEntity<ApiListResponse<SesionMateriaEstudianteDTO>> consultarSesionesMateria(
            @PathVariable final UUID materiaId
    ) {
        final var data = consultarSesionesMateriaEstudianteInputPort.execute(
                authenticatedUserProvider.requireAuthenticatedUserId(),
                materiaId
        );
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/materias/{materiaId}/prerrequisitos")
    public ResponseEntity<ApiListResponse<Void>> consultarPrerrequisitos(@PathVariable final UUID materiaId) {
        throw unavailable("consultar prerrequisitos");
    }

    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<Void>> consultarReclamos() { throw unavailable("consultar reclamos"); }
    @PostMapping("/reclamos")
    public ResponseEntity<ApiDataResponse<Void>> crearReclamo() { throw unavailable("crear reclamo"); }
    @DeleteMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> eliminarReclamo(@PathVariable final UUID id) { throw unavailable("eliminar reclamo"); }
    @PostMapping("/solicitudes-matricula")
    public ResponseEntity<ApiDataResponse<Void>> crearSolicitudMatricula() { throw unavailable("crear solicitud de matricula"); }
    @PostMapping("/matricular-grupo")
    public ResponseEntity<ApiDataResponse<Void>> matricularGrupo() { throw unavailable("matricular grupo"); }

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion de estudiante no esta disponible: " + operation + ".");
    }
}
