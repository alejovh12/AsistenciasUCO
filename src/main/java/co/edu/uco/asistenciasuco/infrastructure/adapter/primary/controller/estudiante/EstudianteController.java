package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiantes")
public final class EstudianteController {

    private final ConsultarEstudiantesInputPort consultarEstudiantesInputPort;
    private final ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort;

    public EstudianteController(
            final ConsultarEstudiantesInputPort consultarEstudiantesInputPort,
            final ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort
    ) {
        if (ObjectHelper.isNull(consultarEstudiantesInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarEstudiantesInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarEstudiantePorIdInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarEstudiantePorIdInputPort es obligatorio.");
        }
        this.consultarEstudiantesInputPort = consultarEstudiantesInputPort;
        this.consultarEstudiantePorIdInputPort = consultarEstudiantePorIdInputPort;
    }

    @GetMapping
    public ResponseEntity<EstudiantePaginaDTO> consultarEstudiantes(
            @RequestParam(required = false) final UUID tipoIdentificacionId,
            @RequestParam(required = false) final Integer numeroIdentificacion,
            @RequestParam(required = false) final String nombre,
            @RequestParam(required = false) final String correo,
            @RequestParam(required = false) final UUID institucionId,
            @RequestParam(required = false) final UUID facultadId,
            @RequestParam(required = false) final UUID programaId,
            @RequestParam(required = false) final UUID grupoId,
            @RequestParam(required = false) final Boolean activo,
            @RequestParam(defaultValue = "0") final Integer page,
            @RequestParam(defaultValue = "20") final Integer size
    ) {
        return ResponseEntity.ok(consultarEstudiantesInputPort.execute(new ConsultarEstudiantesDTO(
                tipoIdentificacionId,
                numeroIdentificacion,
                nombre,
                correo,
                institucionId,
                facultadId,
                programaId,
                grupoId,
                activo,
                page,
                size
        )));
    }

    @GetMapping("/{estudianteId}")
    public ResponseEntity<EstudianteDetalleDTO> consultarEstudiantePorId(@PathVariable final UUID estudianteId) {
        return ResponseEntity.ok(consultarEstudiantePorIdInputPort.execute(estudianteId));
    }
}
