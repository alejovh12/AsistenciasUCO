package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de parametros.
 */
public record ParametroDomain(UUID id, String grupo, String clave, String valor, String tipoDato,
                               String valorDefecto, boolean estaActivo) {
}
