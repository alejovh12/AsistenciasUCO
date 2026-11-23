package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports;

import java.time.Instant;

/**
 * INT-004: puerto de entrada para reflejar en la BD y en la cache los cambios del Catalogo de Parametros externo.
 *
 * <p>Lo invoca el adaptador primario de mensajeria (eventos Pulsar del Catalogo) y el arranque del backend.</p>
 */
public interface SincronizarParametrosCatalogoInputPort {

    /**
     * Procesa un evento de cambio. El evento solo notifica: el valor vigente se relee del Catalogo por M2M.
     *
     * @param aviso datos del evento ya extraidos del JSON.
     * @return resumen de lo hecho (un evento repetido, ajeno o desconocido no hace nada).
     * @throws RuntimeException si fallo algo reintentable (Catalogo o BD caidos): el evento no se da por procesado.
     */
    ResumenSincronizacion procesarAviso(AvisoCambioParametro aviso);

    /**
     * Reconciliacion completa: invalida la cache y copia en la BD todos los valores del Catalogo. Una sola lectura M2M, sin
     * sondeo. Cubre eventos perdidos y cambios ocurridos con el backend apagado. No borra nada: un parametro ausente de la
     * lectura no implica que se haya borrado.
     */
    ResumenSincronizacion reconciliarTodo();

    /**
     * Evento de cambio del Catalogo (schemaVersion 3).
     *
     * @param idAviso       {@code eventId}; sirve para ignorar repetidos; puede ser nulo.
     * @param tipo          {@code ParameterCreated/Updated/Deleted}, {@code MetadataCreated/Updated/Deleted} o
     *                      {@code MetadataEnvironmentCreated/Updated/Deleted}.
     * @param nombreRecurso {@code resourceName}: nombre del parametro ({@code GRUPO.CLAVE}).
     * @param applicationId aplicacion del Catalogo a la que pertenece el cambio; puede ser nulo.
     * @param environmentId ambiente del Catalogo al que pertenece el cambio; puede ser nulo.
     * @param ocurridoEn    {@code occurredAt}; si falta o no se entiende, el instante de recepcion.
     */
    record AvisoCambioParametro(String idAviso, String tipo, String nombreRecurso, String applicationId,
                                String environmentId, Instant ocurridoEn) {
    }

    /**
     * Cuantos parametros se revisaron y que paso en la BD.
     *
     * @param revisados    parametros leidos del Catalogo (o confirmados tras el evento).
     * @param aplicados    valores que cambiaron en la BD.
     * @param sinCambios   valores que ya eran iguales (incluye eventos repetidos).
     * @param desconocidos claves que la BD no tiene (no se crean).
     * @param obsoletos    valores ignorados por haber uno mas reciente.
     * @param rechazados   valores que no cumplen el tipo, rango o lista del parametro.
     * @param fallidos     parametros que no se pudieron guardar.
     */
    record ResumenSincronizacion(int revisados, int aplicados, int sinCambios, int desconocidos, int obsoletos,
                                 int rechazados, int fallidos) {

        public static ResumenSincronizacion vacio() {
            return new ResumenSincronizacion(0, 0, 0, 0, 0, 0, 0);
        }
    }
}
