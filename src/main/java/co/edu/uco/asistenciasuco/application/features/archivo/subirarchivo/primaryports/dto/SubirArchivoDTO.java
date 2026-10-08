package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto;

import java.util.Arrays;
import java.util.Objects;
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

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SubirArchivoDTO that)) {
            return false;
        }
        return Arrays.equals(content, that.content)
                && Objects.equals(ownerSubject, that.ownerSubject)
                && Objects.equals(originalFilename, that.originalFilename)
                && Objects.equals(declaredContentType, that.declaredContentType);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(ownerSubject, originalFilename, declaredContentType) + Arrays.hashCode(content);
    }

    /** No expone el payload del archivo; solo su longitud. */
    @Override
    public String toString() {
        return "SubirArchivoDTO[ownerSubject=" + ownerSubject + ", originalFilename=" + originalFilename
                + ", declaredContentType=" + declaredContentType
                + ", content=" + (content == null ? "null" : content.length + " bytes") + "]";
    }
}
