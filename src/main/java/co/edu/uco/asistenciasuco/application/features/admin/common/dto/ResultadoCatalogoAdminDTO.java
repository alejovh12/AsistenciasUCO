package co.edu.uco.asistenciasuco.application.features.admin.common.dto;

import java.util.UUID;

/** Registro de catalogo guardado o con estado cambiado: identificador y mensaje institucional que entrego la DB. */
public record ResultadoCatalogoAdminDTO(UUID id, String mensajeUsuario) {
}
