package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.SincronizarMensajesCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort.MensajeExterno;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort.ResultadoSincronizacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * INT-003: refleja en la cache y en {@code dbo.CatalogoMensajeUsuario} los cambios del catalogo externo de mensajes.
 *
 * <ul>
 *   <li>{@code MessageCreated} / {@code MessageUpdated}: se lee el texto vigente, se invalida la cache de ese codigo, se
 *       guarda en la BD y, si cambio, se publica {@code MESSAGE_UPDATED} en tiempo real.</li>
 *   <li>{@code MessageDeleted}: solo se invalida la cache. La BD conserva el texto como contingencia del proveedor
 *       {@code sqlserver} (un borrado accidental en el componente externo no deja a los SP sin mensaje).</li>
 *   <li>{@code MessagesChanged} (o un aviso sin codigo): reconciliacion completa.</li>
 *   <li>Un aviso repetido (mismo identificador) se ignora; se marca como visto solo si se proceso sin error, para que el
 *       broker lo reentregue si la BD fallo.</li>
 * </ul>
 */
public class SincronizarMensajesCatalogoUseCaseImpl implements SincronizarMensajesCatalogoUseCase {

    static final String MESSAGE_CREATED = "MessageCreated";
    static final String MESSAGE_UPDATED = "MessageUpdated";
    static final String MESSAGE_DELETED = "MessageDeleted";
    static final String MESSAGES_CHANGED = "MessagesChanged";
    static final String EVENTO_REALTIME = "MESSAGE_UPDATED";
    static final int MAX_AVISOS_RECORDADOS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizarMensajesCatalogoUseCaseImpl.class);

    private final MessageCatalogSourcePort fuente;
    private final MensajeCatalogoRepositoryPort repositorio;
    private final CatalogInvalidationPort invalidacion;
    private final RealtimePublisherPort realtime;
    private final Set<String> avisosProcesados = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_AVISOS_RECORDADOS;
                }
            }));

    public SincronizarMensajesCatalogoUseCaseImpl(
            final MessageCatalogSourcePort fuente,
            final MensajeCatalogoRepositoryPort repositorio,
            final CatalogInvalidationPort invalidacion,
            final RealtimePublisherPort realtime
    ) {
        this.fuente = Objects.requireNonNull(fuente, "MessageCatalogSourcePort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MensajeCatalogoRepositoryPort es obligatorio.");
        this.invalidacion = Objects.requireNonNull(invalidacion, "CatalogInvalidationPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public ResumenSincronizacion procesarAviso(final AvisoCambioMensaje aviso) {
        if (aviso == null || vacio(aviso.tipo())) {
            return ResumenSincronizacion.vacio();
        }
        final String idAviso = vacio(aviso.idAviso()) ? null : aviso.idAviso().trim();
        if (idAviso != null && avisosProcesados.contains(idAviso)) {
            LOGGER.debug("Aviso de MessageUcoLab repetido; se ignora. idAviso={}", idAviso);
            return ResumenSincronizacion.vacio();
        }
        final ResumenSincronizacion resumen = despachar(aviso.tipo().trim(), aviso.codigoExterno());
        if (idAviso != null) {
            avisosProcesados.add(idAviso);
        }
        return resumen;
    }

    @Override
    public ResumenSincronizacion reconciliarTodo() {
        invalidacion.invalidateAll();
        final List<MensajeExterno> mensajes = fuente.listar();
        int actualizados = 0;
        int sinCambios = 0;
        int noExisten = 0;
        int fallidos = 0;
        for (final MensajeExterno mensaje : mensajes) {
            try {
                switch (guardar(mensaje)) {
                    case ACTUALIZADO -> actualizados++;
                    case SIN_CAMBIOS -> sinCambios++;
                    case NO_EXISTE -> noExisten++;
                }
            } catch (final RuntimeException e) {
                fallidos++;
                LOGGER.warn("No se pudo sincronizar el mensaje en la BD. codigo={}, causa={}",
                        mensaje.codigoCanonico(), e.getClass().getSimpleName());
            }
        }
        final ResumenSincronizacion resumen = new ResumenSincronizacion(mensajes.size(), actualizados, sinCambios, noExisten,
                fallidos);
        LOGGER.info("Reconciliacion de mensajes con MessageUcoLab: {}", resumen);
        return resumen;
    }

    private ResumenSincronizacion despachar(final String tipo, final String codigoExterno) {
        if (MESSAGES_CHANGED.equals(tipo)) {
            return reconciliarTodo();
        }
        if (MESSAGE_DELETED.equals(tipo)) {
            invalidacion.invalidateAll();
            return ResumenSincronizacion.vacio();
        }
        if (!MESSAGE_CREATED.equals(tipo) && !MESSAGE_UPDATED.equals(tipo)) {
            LOGGER.debug("Tipo de aviso de MessageUcoLab desconocido; se ignora. tipo={}", tipo);
            return ResumenSincronizacion.vacio();
        }
        if (vacio(codigoExterno)) {
            return reconciliarTodo();
        }
        final Optional<MensajeExterno> mensaje = fuente.obtener(codigoExterno.trim());
        if (mensaje.isEmpty()) {
            // Codigo ajeno a Asistencias o texto no legible: no se sabe que codigo canonico vencer, se vence toda la cache.
            invalidacion.invalidateAll();
            return ResumenSincronizacion.vacio();
        }
        final ResultadoSincronizacion resultado = guardar(mensaje.get());
        return new ResumenSincronizacion(1,
                resultado == ResultadoSincronizacion.ACTUALIZADO ? 1 : 0,
                resultado == ResultadoSincronizacion.SIN_CAMBIOS ? 1 : 0,
                resultado == ResultadoSincronizacion.NO_EXISTE ? 1 : 0,
                0);
    }

    private ResultadoSincronizacion guardar(final MensajeExterno mensaje) {
        invalidacion.invalidateMessage(mensaje.codigoCanonico());
        final ResultadoSincronizacion resultado = repositorio.sincronizarMensajeUsuario(mensaje.codigoCanonico(),
                mensaje.contenido());
        if (resultado == ResultadoSincronizacion.ACTUALIZADO) {
            realtime.publish(RealtimeEvent.of(EVENTO_REALTIME, Map.of(
                    "code", mensaje.codigoCanonico(),
                    "type", "USER",
                    "source", "MESSAGEUCOLAB")));
        }
        return resultado;
    }

    private static boolean vacio(final String valor) {
        return valor == null || valor.isBlank();
    }
}
