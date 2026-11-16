package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports;

/**
 * INT-003: puerto de entrada para reflejar en la BD y en la cache los cambios del catalogo externo de mensajes.
 *
 * <p>Lo invoca el adaptador primario de mensajeria (avisos de MessageUcoLab por Pulsar) y el arranque del backend.</p>
 */
public interface SincronizarMensajesCatalogoInputPort {

    /**
     * Procesa un aviso de cambio. El aviso no trae el texto: se vuelve a leer del catalogo externo.
     *
     * @param aviso tipo, codigo externo e identificador unico del aviso.
     * @return resumen de lo hecho (un aviso repetido o desconocido no hace nada).
     */
    ResumenSincronizacion procesarAviso(AvisoCambioMensaje aviso);

    /**
     * Reconciliacion completa: invalida la cache y copia en la BD todos los textos del catalogo externo. Cubre los avisos
     * que se pierden (el componente publica con mejor esfuerzo) y los cambios ocurridos con el backend apagado.
     */
    ResumenSincronizacion reconciliarTodo();

    /**
     * Aviso de cambio de un mensaje.
     *
     * @param idAviso       identificador unico (sirve para ignorar repetidos); puede ser nulo.
     * @param tipo          {@code MessageCreated}, {@code MessageUpdated}, {@code MessageDeleted} o {@code MessagesChanged}.
     * @param codigoExterno codigo del mensaje en el catalogo externo; vacio en {@code MessagesChanged}.
     */
    record AvisoCambioMensaje(String idAviso, String tipo, String codigoExterno) {
    }

    /**
     * Cuantos mensajes se revisaron y que paso en la BD.
     *
     * @param revisados   mensajes leidos del catalogo externo.
     * @param actualizados textos que cambiaron en la BD.
     * @param sinCambios  textos que ya eran iguales.
     * @param noExisten   codigos que la BD no tiene (no se crean).
     * @param fallidos    mensajes que no se pudieron leer o guardar.
     */
    record ResumenSincronizacion(int revisados, int actualizados, int sinCambios, int noExisten, int fallidos) {

        public static ResumenSincronizacion vacio() {
            return new ResumenSincronizacion(0, 0, 0, 0, 0);
        }
    }
}
