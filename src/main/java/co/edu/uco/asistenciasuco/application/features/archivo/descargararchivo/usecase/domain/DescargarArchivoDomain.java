package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain;

import java.util.UUID;

public record DescargarArchivoDomain(UUID fileId, UUID requesterSubject) {
}
