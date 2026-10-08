package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity;

/**
 * Resultado interno del caso de uso de descarga; el Interactor lo mapea al DTO del InputPort
 * (nunca al reves: usecase no conoce primaryports.dto, ver CleanArchitectureRulesTest).
 */
public record DescargarArchivoResultadoEntity(byte[] content, String contentType, String filename, long size) {
}
