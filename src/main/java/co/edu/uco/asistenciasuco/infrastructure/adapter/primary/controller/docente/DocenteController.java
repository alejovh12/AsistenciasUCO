package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.RegistrarDocenteDesdeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador primario REST para operaciones relacionadas con docentes.
 */
@RestController
@RequestMapping("/api/v1/docentes")
public final class DocenteController {

    private final ConsultarDocentesInputPort consultarDocentesInputPort;
    private final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort;
    private final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort;
    private final RegistrarDocenteDesdeUsuarioInputPort registrarDocenteDesdeUsuarioInputPort;
    private final AsignarDocenteAGrupoInputPort asignarDocenteAGrupoInputPort;

    public DocenteController(
            final ConsultarDocentesInputPort consultarDocentesInputPort,
            final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort,
            final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort,
            final RegistrarDocenteDesdeUsuarioInputPort registrarDocenteDesdeUsuarioInputPort,
            final AsignarDocenteAGrupoInputPort asignarDocenteAGrupoInputPort
    ) {
        if (ObjectHelper.isNull(consultarDocentesInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarDocentesInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarDocentePorIdInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarDocentePorIdInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarAsignacionesAcademicasDocenteInputPort)) {
            throw new CrosscuttingException(
                    "El puerto de entrada ConsultarAsignacionesAcademicasDocenteInputPort es obligatorio."
            );
        }
        if (ObjectHelper.isNull(registrarDocenteDesdeUsuarioInputPort)) {
            throw new CrosscuttingException(
                    "El puerto de entrada RegistrarDocenteDesdeUsuarioInputPort es obligatorio."
            );
        }
        if (ObjectHelper.isNull(asignarDocenteAGrupoInputPort)) {
            throw new CrosscuttingException("El puerto de entrada AsignarDocenteAGrupoInputPort es obligatorio.");
        }

        this.consultarDocentesInputPort = consultarDocentesInputPort;
        this.consultarDocentePorIdInputPort = consultarDocentePorIdInputPort;
        this.consultarAsignacionesAcademicasDocenteInputPort = consultarAsignacionesAcademicasDocenteInputPort;
        this.registrarDocenteDesdeUsuarioInputPort = registrarDocenteDesdeUsuarioInputPort;
        this.asignarDocenteAGrupoInputPort = asignarDocenteAGrupoInputPort;
    }

    @GetMapping
    public ResponseEntity<List<DocenteIdentidadDTO>> consultarDocentes() {
        return ResponseEntity.ok(consultarDocentesInputPort.execute());
    }

    @PostMapping("/consultas/id")
    public ResponseEntity<Map<String, Object>> consultarDocentePorId(
            @RequestBody final ConsultarDocentePorIdDTO dto
    ) {
        final DocenteIdentidadDTO docente = consultarDocentePorIdInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("datos", docente);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/consultas/asignaciones")
    public ResponseEntity<Map<String, Object>> consultarAsignacionesAcademicas(
            @RequestBody final ConsultarAsignacionesAcademicasDocenteDTO dto
    ) {
        final List<DocenteAsignacionAcademicaDTO> asignaciones =
                consultarAsignacionesAcademicasDocenteInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("datos", asignaciones);
        response.put("total", asignaciones.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<RegistrarDocenteDesdeUsuarioResultadoDTO> registrarDocenteDesdeUsuario(
            @RequestBody final RegistrarDocenteDesdeUsuarioDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarDocenteDesdeUsuarioInputPort.execute(dto));
    }

    @PostMapping("/asignaciones/grupo")
    public ResponseEntity<AsignarDocenteAGrupoResultadoDTO> asignarDocenteAGrupo(
            @RequestBody final AsignarDocenteAGrupoDTO dto
    ) {
        return ResponseEntity.ok(asignarDocenteAGrupoInputPort.execute(dto));
    }
}
