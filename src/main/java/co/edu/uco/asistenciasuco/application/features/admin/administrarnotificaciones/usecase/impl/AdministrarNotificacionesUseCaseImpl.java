package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.AdministrarNotificacionesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalPublicoProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

public final class AdministrarNotificacionesUseCaseImpl implements AdministrarNotificacionesUseCase {

    private static final String ESTADO_ENTREGADA = "ENTREGADA";
    private static final String ESTADO_FALLIDA_DEFINITIVA = "FALLIDA_DEFINITIVA";
    private static final String ESTADO_FALLIDA_REINTENTABLE = "FALLIDA_REINTENTABLE";

    private final AdministracionNotificacionPort port;

    public AdministrarNotificacionesUseCaseImpl(final AdministracionNotificacionPort port) {
        this.port = Objects.requireNonNull(port, "AdministracionNotificacionPort es obligatorio.");
    }

    @Override public EstadoCanalNotificacionProjection consultarEstadoCanal() { return port.consultarEstadoCanal(); }

    /**
     * CULM-001 D.9-a (F-ADM-029): el canal esta activo si el despachador esta habilitado y hay un sender disponible; la ultima entrega
     * y el ultimo error salen de la cola (la notificacion mas reciente de cada estado). Sin host, puerto ni usuario de la cuenta tecnica.
     */
    @Override
    public EstadoCanalPublicoProjection consultarEstadoCanalPublico(final UUID idUsuarioEjecutor) {
        final EstadoCanalNotificacionProjection estado = port.consultarEstadoCanal();
        final Instant ultimoEnvio = masReciente(ESTADO_ENTREGADA, idUsuarioEjecutor).map(NotificacionSalienteAdminProjection::entregadoEn).orElse(null);
        final Optional<NotificacionSalienteAdminProjection> fallida = masReciente(ESTADO_FALLIDA_DEFINITIVA, idUsuarioEjecutor);
        final Optional<NotificacionSalienteAdminProjection> reintentable = masReciente(ESTADO_FALLIDA_REINTENTABLE, idUsuarioEjecutor);
        final Optional<NotificacionSalienteAdminProjection> ultimaFalla = java.util.stream.Stream.of(fallida, reintentable)
                .flatMap(Optional::stream)
                .max(java.util.Comparator.comparing(NotificacionSalienteAdminProjection::creadoEn, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        final String ultimoError = ultimaFalla
                .filter(falla -> ultimoEnvio == null || (falla.creadoEn() != null && falla.creadoEn().isAfter(ultimoEnvio)))
                .map(NotificacionSalienteAdminProjection::codigoError)
                .orElse(null);
        return new EstadoCanalPublicoProjection(estado.dispatcherEnabled() && estado.disponible(), ultimoEnvio, ultimoError);
    }

    private Optional<NotificacionSalienteAdminProjection> masReciente(final String codigoEstado, final UUID idUsuarioEjecutor) {
        return port.consultarCola(codigoEstado, null, 1, 1, idUsuarioEjecutor).elementos().stream().findFirst();
    }
    @Override public List<TipoPlantillaAdminProjection> consultarPlantillas(final String idioma, final UUID idUsuarioEjecutor) { return port.consultarPlantillas(idioma, idUsuarioEjecutor); }
    @Override public VersionPlantillaProjection crearVersionPlantilla(final String tipo, final String idioma, final String asunto, final String cuerpo, final UUID creadoPor) { return port.crearVersionPlantilla(tipo, idioma, asunto, cuerpo, creadoPor); }
    @Override public void activarVersionPlantilla(final String tipo, final String idioma, final int version, final UUID activadoPor) { port.activarVersionPlantilla(tipo, idioma, version, activadoPor); }
    @Override public VistaPreviaPlantillaProjection generarVistaPrevia(final String tipo, final String idioma, final String asunto, final String cuerpo) { return port.generarVistaPrevia(tipo, idioma, asunto, cuerpo); }
    @Override public PaginaColaNotificacionProjection consultarCola(final String estado, final String tipo, final int pagina, final int tamano, final UUID idUsuarioEjecutor) { return port.consultarCola(estado, tipo, pagina, tamano, idUsuarioEjecutor); }
    @Override public void reintentarNotificacion(final UUID idNotificacion, final UUID usuarioEjecutor) { port.reintentarNotificacion(idNotificacion, usuarioEjecutor); }
    @Override public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) {
        if (tipo != null && !tipo.isBlank()) {
            port.enviarCorreoPrueba(idUsuarioEjecutor, tipo, correoDestino);
        } else {
            port.enviarCorreoPrueba(idUsuarioEjecutor, correoDestino);
        }
    }
}
