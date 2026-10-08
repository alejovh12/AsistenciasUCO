package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto;

import java.util.UUID;

/**
 * Entrada del InputPort de subida de soporte. {@code ownerSubject} proviene siempre de la
 * identidad autenticada resuelta por Infrastructure (JWT), nunca de un campo de request.
 */
public record SubirArchivoDTO(
        UUID ownerSubject,
        String originalFilename,
        String declaredContentType,
        byte[] content
) {
}
