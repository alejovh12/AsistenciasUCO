package co.edu.uco.asistenciasuco.application.features.coordinador.common.domain;

import java.util.UUID;

public record DocenteCandidatoDomain(UUID idDocente, UUID idUsuario, String codigoTipoIdentificacion,
                                     Integer numeroIdentificacion, String nombreCompleto) {
}
