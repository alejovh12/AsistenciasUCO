package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record SubirArchivoDomain(
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
        if (!(other instanceof SubirArchivoDomain(var otherOwnerSubject, var otherOriginalFilename, var otherDeclaredContentType, var otherContent))) {
            return false;
        }
        return Arrays.equals(content, otherContent)
                && Objects.equals(ownerSubject, otherOwnerSubject)
                && Objects.equals(originalFilename, otherOriginalFilename)
                && Objects.equals(declaredContentType, otherDeclaredContentType);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(ownerSubject, originalFilename, declaredContentType) + Arrays.hashCode(content);
    }

    /** No expone el payload del archivo; solo su longitud. */
    @Override
    public String toString() {
        return "SubirArchivoDomain[ownerSubject=" + ownerSubject + ", originalFilename=" + originalFilename
                + ", declaredContentType=" + declaredContentType
                + ", content=" + (content == null ? "null" : content.length + " bytes") + "]";
    }
}
