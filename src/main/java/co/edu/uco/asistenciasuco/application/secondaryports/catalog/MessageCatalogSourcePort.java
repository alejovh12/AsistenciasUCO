package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

import java.util.List;
import java.util.Optional;

/**
 * INT-003: fuente externa de los textos del catalogo de mensajes (hoy MessageUcoLab), leida para reflejarlos en la BD.
 *
 * <p>Trabaja con codigos canonicos de Asistencias: la traduccion entre el codigo del componente externo y el canonico es
 * un detalle del adaptador. Un mensaje externo que no corresponde a ningun codigo canonico no se devuelve.</p>
 *
 * <p>Distingue "no existe" (respuesta vacia) de "no se pudo leer" ({@link FuenteNoDisponibleException}): un fallo
 * transitorio (limite de peticiones, caida, red) no debe confundirse con un mensaje inexistente, porque el aviso se
 * daria por procesado sin copiar el texto.</p>
 */
public interface MessageCatalogSourcePort {

    /**
     * Lee el texto vigente de un mensaje a partir del codigo con que lo identifica el componente externo.
     *
     * @param codigoExterno codigo del aviso de cambio (ej. {@code MSG_VAL001}).
     * @return el mensaje con su codigo canonico; vacio si no existe o no es de Asistencias.
     * @throws FuenteNoDisponibleException si el componente no respondio (se debe reintentar).
     */
    Optional<MensajeExterno> obtener(String codigoExterno);

    /**
     * Lee todos los mensajes que el componente externo entrega para el ambiente de la aplicacion.
     *
     * @return solo los que corresponden a un codigo canonico.
     * @throws FuenteNoDisponibleException si el componente no respondio (se debe reintentar).
     */
    List<MensajeExterno> listar();

    /**
     * Texto vigente de un mensaje externo ya traducido al codigo canonico de Asistencias.
     *
     * @param codigoCanonico codigo de Asistencias (ej. {@code VAL_001}).
     * @param contenido      texto para la persona usuaria, con sus marcadores ({@code {}} o {@code {0}}).
     */
    record MensajeExterno(String codigoCanonico, String contenido) {
    }

    /** El componente externo no entrego la respuesta (fallo transitorio): quien lo invoca debe reintentar. */
    class FuenteNoDisponibleException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public FuenteNoDisponibleException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
