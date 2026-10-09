package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto;

import java.util.UUID;

/**
 * Resultado neutral de la subida: nunca expone bucket, objectKey, endpoint del provider,
 * access key, secret ni presigned URL.
 */
public record SubirArchivoResultado(UUID fileId, String nombre, String url, long tamanio) {
}
