package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.SolicitarRecuperacionClaveUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort.Solicitante;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU177: pide el enlace para definir una contrasena nueva. La respuesta es siempre la misma (RCL_001): no dice si el
 * correo existe, si la cuenta esta inactiva ni si ya se envio un enlace hace poco. Cuando procede, encola el aviso
 * {@link Notificacion#TIPO_RECUPERACION_CLAVE}; el enlace de un solo uso lo emite el canal de correo al entregar, de
 * modo que el token nunca pasa por este caso de uso ni queda en el outbox.
 */
public final class SolicitarRecuperacionClaveUseCaseImpl implements SolicitarRecuperacionClaveUseCase {

    static final String CODIGO_RESPUESTA = "RCL_001";
    static final int LONGITUD_MAXIMA_CORREO = 100;
    static final String ROL_DESTINATARIO = "USUARIO";

    private final RecuperacionClaveRepositoryPort repositorio;
    private final NotificacionPort notificacionPort;
    private final MessageCatalogPort mensajes;

    public SolicitarRecuperacionClaveUseCaseImpl(
            final RecuperacionClaveRepositoryPort repositorio,
            final NotificacionPort notificacionPort,
            final MessageCatalogPort mensajes
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "RecuperacionClaveRepositoryPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public String execute(final String correo) {
        final String correoLimpio = correo == null ? "" : correo.strip();
        if (!correoLimpio.isEmpty() && correoLimpio.length() <= LONGITUD_MAXIMA_CORREO) {
            repositorio.consultarSolicitante(correoLimpio).ifPresent(this::encolarAviso);
        }
        return mensajes.getUserMessage(CODIGO_RESPUESTA);
    }

    private void encolarAviso(final Solicitante solicitante) {
        final String nombre = TextHelper.toNombrePropio(solicitante.primerNombre());
        notificacionPort.notificar(new Notificacion(
                Notificacion.TIPO_RECUPERACION_CLAVE,
                "Defina una contraseña nueva",
                "Recibimos una solicitud para definir una contraseña nueva de su cuenta en la plataforma de asistencia.",
                List.of(new Notificacion.Destinatario(ROL_DESTINATARIO, solicitante.idUsuario(), null, null)),
                TextHelper.isNullOrBlank(nombre) ? Map.of() : Map.of("nombre", nombre)
        ));
    }
}
