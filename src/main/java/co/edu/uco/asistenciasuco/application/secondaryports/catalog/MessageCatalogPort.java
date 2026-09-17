package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

import java.util.Optional;

/**
 * Puerto secundario neutral para interactuar con el catalogo institucional de mensajes.
 *
 * <p>Permite resolver mensajes de cara al usuario y de cara a soporte tecnico a partir
 * de codigos de negocio canónicos (ej. 'VAL-001', 'AUTH-002'), desacoplando la capa
 * de aplicacion del almacenamiento real (tablas SQL Server CatalogoMensajeUsuario/Tecnico,
 * archivos i18n, Azure App Configuration, etc.).</p>
 */
public interface MessageCatalogPort {

    /**
     * Obtiene el mensaje parametrizado para el usuario final.
     *
     * @param code Codigo unico del mensaje (ej. 'VAL-001').
     * @param args Argumentos posicionales opcionales para interpolacion (ej. {0}, {1} o %s).
     * @return Texto formateado para el usuario final.
     */
    String getUserMessage(String code, Object... args);

    /**
     * Obtiene el mensaje tecnico para soporte y observabilidad (trazas/logs).
     *
     * @param code Codigo unico del mensaje.
     * @param args Argumentos posicionales opcionales.
     * @return Texto tecnico para auditoria.
     */
    String getTechnicalMessage(String code, Object... args);

    /**
     * Obtiene el mensaje para el usuario como Optional si existe en el catalogo.
     *
     * @param code Codigo del mensaje.
     * @return Optional con el mensaje si esta configurado.
     */
    Optional<String> findUserMessage(String code);

    /**
     * Excepcion base para errores del catalogo de mensajes.
     */
    class MessageCatalogException extends RuntimeException {
        public MessageCatalogException(final String message) {
            super(message);
        }

        public MessageCatalogException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
