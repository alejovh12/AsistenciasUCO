package co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSp;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;

import java.util.List;
import java.util.UUID;

public interface CargaUsuariosValidacionPort {

    List<FilaValidacionDbProjection> validarEnBaseDatos(
            UUID idUsuarioEjecutor,
            PerfilDestinoCarga perfilDestino,
            List<FilaCsvParaSp> filas
    );
}
