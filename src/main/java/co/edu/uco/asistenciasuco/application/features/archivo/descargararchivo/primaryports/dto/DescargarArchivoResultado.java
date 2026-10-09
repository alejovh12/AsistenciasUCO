package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto;

import java.util.Arrays;
import java.util.Objects;

public record DescargarArchivoResultado(byte[] content, String contentType, String filename, long size) {

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DescargarArchivoResultado that)) {
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
        return "DescargarArchivoResultado[content=" + (content == null ? "null" : content.length + " bytes")
                + ", contentType=" + contentType + ", filename=" + filename + ", size=" + size + "]";
    }
}
