package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

import java.util.Optional;

/**
 * Puerto secundario neutral para interactuar con el catalogo dinamico de parametros de la institucion.
 *
 * <p>Permite leer parametros de negocio agrupados por grupo y clave de forma desacoplada
 * del mecanismo fisico (Azure App Configuration, SQL Server, Redis, Consul, Spring Properties, etc.).</p>
 */
public interface ParameterCatalogPort {

    /**
     * Obtiene el valor textual de un parametro por su grupo y clave.
     *
     * @param group Grupo logico o modulo del parametro (ej. 'asistencias', 'seguridad', 'institucional').
     * @param key   Clave o identificador del parametro dentro del grupo.
     * @return Optional con el valor si existe, o vacio si no.
     */
    Optional<String> getParameter(String group, String key);

    /**
     * Obtiene el valor textual de un parametro obligatorio. Si no existe, lanza {@link ParameterNotFoundException}.
     *
     * @param group Grupo logico del parametro.
     * @param key   Clave del parametro.
     * @return Valor del parametro.
     */
    String getRequiredParameter(String group, String key);

    /**
     * Obtiene y castea el valor de un parametro a un tipo tipado basico (String, Integer, Long, Boolean, Double).
     *
     * @param group      Grupo logico.
     * @param key        Clave del parametro.
     * @param targetType Clase destino deseada.
     * @param <T>        Tipo resultado.
     * @return Instancia convertida al tipo indicado.
     */
    <T> T getParameterAs(String group, String key, Class<T> targetType);

    /**
     * Excepcion base para errores del catalogo de parametros.
     */
    class ParameterCatalogException extends RuntimeException {
        public ParameterCatalogException(final String message) {
            super(message);
        }

        public ParameterCatalogException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepcion cuando un parametro requerido no se encuentra configurado.
     */
    class ParameterNotFoundException extends ParameterCatalogException {
        public ParameterNotFoundException(final String group, final String key) {
            super("El parametro requerido [" + group + ":" + key + "] no fue encontrado en el catalogo.");
        }
    }
}
