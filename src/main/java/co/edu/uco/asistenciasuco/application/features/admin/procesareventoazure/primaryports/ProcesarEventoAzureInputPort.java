package co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports;

import java.util.Map;

/**
 * Puerto de entrada primario para procesar eventos provenientes de Azure Event Grid.
 */
public interface ProcesarEventoAzureInputPort {

    /**
     * Procesa un evento recibido desde Azure Event Grid (App Configuration o Key Vault).
     *
     * @param eventType Tipo de evento (ej. 'Microsoft.AppConfiguration.KeyValueModified', 'Microsoft.KeyVault.SecretNewVersionCreated').
     * @param subject   Asunto del evento (ej. clave de configuración o nombre de secreto).
     * @param data      Datos adicionales del evento.
     */
    void procesarEvento(String eventType, String subject, Map<String, Object> data);
}
