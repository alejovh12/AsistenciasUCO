package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.dto;

import java.util.UUID;

public record ParametroDTO(UUID id, String grupo, String clave, String valor, String tipoDato,
                           String valorDefecto, boolean estaActivo) {
}
