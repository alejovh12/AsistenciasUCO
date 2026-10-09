package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto;

import java.util.UUID;

public record DescargarArchivoDTO(UUID fileId, UUID requesterSubject) {
}
