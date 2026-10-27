package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto;

import java.util.UUID;

/** Resultado de la resolucion: ACEPTADA (matriculado), EN_ESPERA (sin cupo) o RECHAZADA. */
public record ResultadoSolicitudIngresoDTO(UUID id, String estadoResultante) {
}
