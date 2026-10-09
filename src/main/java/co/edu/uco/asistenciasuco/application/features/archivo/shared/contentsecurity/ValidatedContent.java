package co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity;

/**
 * Resultado de una validacion de contenido exitosa: nombre sanitizado, extension reconocida y
 * content type verificado contra los bytes reales (no el declarado por el cliente).
 */
public record ValidatedContent(String sanitizedFilename, String extension, String verifiedContentType) {
}
