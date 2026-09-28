package co.edu.uco.asistenciasuco.application.secondaryports.vault;

import java.util.Optional;

/**
 * Puerto secundario neutral para interactuar con un baul o almacen de secretos.
 *
 * <p>Este contrato es agnostico a la tecnologia de persistencia de secretos (Azure Key Vault,
 * HashiCorp Vault, AWS Secrets Manager, variables de entorno locales, base de datos cifrada, etc.).
 * La aplicacion y los casos de uso solo conocen este puerto.</p>
 */
public interface SecretVaultPort {

    /**
     * Obtiene el valor de un secreto por su identificador o nombre.
     *
     * @param secretName Nombre del secreto en el baul.
     * @return Optional con el valor si existe, o Optional.empty() si no fue encontrado.
     * @throws SecretVaultException Si ocurre un error de conectividad o de acceso al baul.
     */
    Optional<String> getSecret(String secretName);

    /**
     * Obtiene el valor de un secreto obligatorio. Si no existe, lanza {@link SecretNotFoundException}.
     *
     * @param secretName Nombre del secreto en el baul.
     * @return Valor del secreto.
     * @throws SecretNotFoundException Si el secreto no existe en el baul.
     * @throws SecretVaultException Si ocurre un fallo tecnico al consultar el baul.
     */
    String getRequiredSecret(String secretName);

    /**
     * Excepcion base para errores del baul de secretos.
     */
    class SecretVaultException extends RuntimeException {
        public SecretVaultException(final String message) {
            super(message);
        }

        public SecretVaultException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepcion cuando un secreto requerido no existe en el baul.
     */
    class SecretNotFoundException extends SecretVaultException {
        public SecretNotFoundException(final String secretName) {
            super("El secreto requerido '" + secretName + "' no fue encontrado en el baul de secretos.");
        }
    }
}
