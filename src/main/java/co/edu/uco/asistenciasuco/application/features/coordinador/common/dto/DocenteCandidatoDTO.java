package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.util.UUID;

public record DocenteCandidatoDTO(UUID idDocente, UUID idUsuario, String codigoTipoIdentificacion,
                                  Integer numeroIdentificacion, String nombreCompleto) {
}
