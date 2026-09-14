package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto;

import java.util.UUID;

public record DecanoDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                        UUID idFacultad, String nombreFacultad, boolean estaActivoDecano) {
}
