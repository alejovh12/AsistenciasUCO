package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resultado interno del caso de uso de descarga; el Interactor lo mapea al DTO del InputPort
 * (nunca al reves: usecase no conoce primaryports.dto, ver CleanArchitectureRulesTest).
 */
public record DescargarArchivoResultadoEntity(byte[] content, String contentType, String filename, long size) {

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DescargarArchivoResultadoEntity that)) {
            return false;
        }
        return size == that.size
                && Arrays.equals(content, that.content)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(contentType, filename, size) + Arrays.hashCode(content);
    }

    /** No expone el payload del archivo; solo su longitud. */
    @Override
    public String toString() {
        return "DescargarArchivoResultadoEntity[content=" + (content == null ? "null" : content.length + " bytes")
                + ", contentType=" + contentType + ", filename=" + filename + ", size=" + size + "]";
    }
}
