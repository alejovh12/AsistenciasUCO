package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper.RegistrarEstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con grupos.
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoController {

    private final RegistrarEstudianteInputPort registrarEstudianteInputPort;
    private final ConsultarGruposInputPort consultarGruposInputPort;

    public GrupoController(
            final RegistrarEstudianteInputPort registrarEstudianteInputPort,
            final ConsultarGruposInputPort consultarGruposInputPort
    ) {
        if (ObjectHelper.isNull(registrarEstudianteInputPort)) {
            throw new CrosscuttingException("El puerto de entrada RegistrarEstudianteInputPort es obligatorio.");
        }
        if (ObjectHelper.isNull(consultarGruposInputPort)) {
            throw new CrosscuttingException("El puerto de entrada ConsultarGruposInputPort es obligatorio.");
        }
        this.registrarEstudianteInputPort = registrarEstudianteInputPort;
        this.consultarGruposInputPort = consultarGruposInputPort;
    }

    @GetMapping
    public ResponseEntity<List<GrupoDTO>> consultarGrupos() {
        return ResponseEntity.ok(consultarGruposInputPort.execute());
    }

    @PostMapping("/{grupoId}/estudiantes")
    @AuditableOperation(
            action = "REGISTRAR_ESTUDIANTE_EN_GRUPO",
            resourceType = "GRUPO",
            resourceIdPathVariable = "grupoId"
    )
    public ResponseEntity<RegistrarEstudianteResultadoDTO> registrarEstudiante(
            @PathVariable final UUID grupoId,
            @Valid @RequestBody final RegistrarEstudianteRequest request
    ) {
        final RegistrarEstudianteDTO dto = RegistrarEstudianteHttpMapper.toApplicationDTO(grupoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarEstudianteInputPort.execute(dto));
    }
}
