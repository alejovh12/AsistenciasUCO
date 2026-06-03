package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador primario REST para operaciones relacionadas con grupos.
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoController {

    private final RegistrarEstudianteInputPort registrarEstudianteInputPort;

    public GrupoController(final RegistrarEstudianteInputPort registrarEstudianteInputPort) {
        if (ObjectHelper.isNull(registrarEstudianteInputPort)) {
            throw new CrosscuttingException("El puerto de entrada RegistrarEstudianteInputPort es obligatorio.");
        }
        this.registrarEstudianteInputPort = registrarEstudianteInputPort;
    }

    @PostMapping("/estudiantes")
    public ResponseEntity<Map<String, Object>> registrarEstudiante(
            @RequestBody final RegistrarEstudianteDTO dto
    ) {
        registrarEstudianteInputPort.execute(dto);

        final Map<String, Object> response = new HashMap<>();
        response.put("exitoso", true);
        response.put("mensaje", "Estudiante registrado correctamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}