package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador primario REST para operaciones relacionadas con asistencias.
 */
@RestController
@Profile("mock")
@RequestMapping("/api/v1/asistencias")
public final class AsistenciaController {

    private final RegistrarAsistenciaInputPort registrarAsistenciaInputPort;
    private final ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort;
    private final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort;

    public AsistenciaController(
            final RegistrarAsistenciaInputPort registrarAsistenciaInputPort,
            final ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort,
            final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort
    ) {
        if (ObjectHelper.isNull(registrarAsistenciaInputPort)) {
            throw new CrosscuttingException("El puerto de entrada RegistrarAsistenciaInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarAsistenciasPorGrupoInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarAsistenciasPorGrupoInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(solicitarRevisionAsistenciaInputPort)) {
            throw new CrosscuttingException("El puerto de entrada SolicitarRevisionAsistenciaInputPort es obligatorio.");
        }

        this.registrarAsistenciaInputPort = registrarAsistenciaInputPort;
        this.consultarAsistenciasPorGrupoInputPort = consultarAsistenciasPorGrupoInputPort;
        this.solicitarRevisionAsistenciaInputPort = solicitarRevisionAsistenciaInputPort;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarAsistencia(
            @RequestBody final RegistrarAsistenciaDTO dto
    ) {
        registrarAsistenciaInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("mensaje", "Asistencia registrada correctamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultas/grupo")
    public ResponseEntity<Map<String, Object>> consultarAsistenciasPorGrupo(
            @RequestBody final ConsultarAsistenciasPorGrupoDTO dto
    ) {
        final List<AsistenciaConsultadaDTO> asistencias = consultarAsistenciasPorGrupoInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("datos", asistencias);
        response.put("total", asistencias.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/revisiones")
    public ResponseEntity<Map<String, Object>> solicitarRevision(
            @RequestBody final SolicitarRevisionAsistenciaDTO dto
    ) {
        solicitarRevisionAsistenciaInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("mensaje", "Solicitud de revision registrada correctamente.");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
