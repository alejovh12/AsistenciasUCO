package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain;

import java.util.UUID;

public record SubirArchivoDomain(
        UUID ownerSubject,
        String originalFilename,
        String declaredContentType,
        byte[] content
) {
}
