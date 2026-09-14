package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto;

import java.util.UUID;

public record CoordinadorDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                             UUID idPrograma, String nombrePrograma, boolean estaActivoCoordinador) {
}
