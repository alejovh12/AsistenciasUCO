package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto;

import java.util.UUID;

public record EstudianteProgramaDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                                    String correo, UUID idPrograma, String nombrePrograma) {
}
