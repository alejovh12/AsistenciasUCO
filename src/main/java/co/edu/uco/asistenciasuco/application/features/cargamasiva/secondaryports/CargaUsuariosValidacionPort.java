package co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSpDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;

import java.util.List;
import java.util.UUID;

public interface CargaUsuariosValidacionPort {

    List<FilaValidacionDbProjection> validarEnBaseDatos(
            UUID idUsuarioEjecutor,
            PerfilDestinoCarga perfilDestino,
            List<FilaCsvParaSpDTO> filas
    );
}
