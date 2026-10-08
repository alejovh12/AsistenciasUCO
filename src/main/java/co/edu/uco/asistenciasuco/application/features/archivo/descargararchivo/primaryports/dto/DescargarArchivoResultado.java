package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto;

public record DescargarArchivoResultado(byte[] content, String contentType, String filename, long size) {
}
