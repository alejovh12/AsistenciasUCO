package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

/**
 * Puerto secundario técnico para la invalidación y purga de caché de catálogos y secretos.
 *
 * <p>Mantiene la pureza de {@link ParameterCatalogPort}, {@link MessageCatalogPort} y
 * {@link co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort},
 * los cuales contienen exclusivamente métodos de consulta de negocio.</p>
 */
public interface CatalogInvalidationPort {

    /**
     * Invalida un parámetro específico identificado por su grupo y clave.
     *
     * @param group Grupo lógico del parámetro (ej. 'asistencias').
     * @param key   Clave del parámetro.
     */
    void invalidateParameter(String group, String key);

    /**
     * Invalida un mensaje del catálogo identificado por su código canónico.
     *
     * @param code Código del mensaje (ej. 'VAL-001').
     */
    void invalidateMessage(String code);

    /**
     * Invalida un secreto almacenado en memoria identificado por su nombre.
     *
     * @param secretName Nombre del secreto en el baúl.
     */
    void invalidateSecret(String secretName);

    /**
     * Invalida todas las entradas en memoria de todos los catálogos y baúl de secretos.
     */
    void invalidateAll();
}
