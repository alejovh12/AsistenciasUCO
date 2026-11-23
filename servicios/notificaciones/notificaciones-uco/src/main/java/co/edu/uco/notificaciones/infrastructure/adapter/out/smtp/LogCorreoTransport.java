package co.edu.uco.notificaciones.infrastructure.adapter.out.smtp;

import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.domain.CorreoSaliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Transporte de laboratorio sin SMTP: solo registra cuantos destinatarios tendria el correo (nunca direcciones ni cuerpo). */
public class LogCorreoTransport implements CorreoTransportPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCorreoTransport.class);

    @Override
    public void enviar(final CorreoSaliente correo) {
        LOGGER.info("Canal log: correo no enviado (sin SMTP). messageId={}, destinatarios={}", correo.messageId(), correo.bccAddresses().size());
    }

    @Override
    public boolean activo() {
        return true;
    }
}
