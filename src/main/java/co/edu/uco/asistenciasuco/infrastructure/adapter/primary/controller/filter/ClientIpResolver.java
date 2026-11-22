package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IP del cliente saneada para logs y bitacora de auditoria.
 *
 * <p>Detras de un proxy ({@code Kong}, {@code nginx}, Docker) {@code getRemoteAddr()} es siempre la IP del proxy. Cuando hay un
 * {@link NetworkClientIpResolver} disponible se delega en el: solo se confia en {@code X-Forwarded-For} si la peticion llega de
 * un proxy de {@code RED/PROXIES_CONFIABLES} (vacio por defecto, que equivale a usar {@code remoteAddr}). Sin ese resolvedor se
 * conserva el comportamiento anterior.</p>
 */
@Component
public final class ClientIpResolver {

    private final NetworkClientIpResolver red;

    public ClientIpResolver() {
        this.red = null;
    }

    @Autowired
    public ClientIpResolver(final ObjectProvider<NetworkClientIpResolver> red) {
        this.red = red.getIfAvailable();
    }

    public String resolve(final HttpServletRequest request) {
        final String ip = red == null ? request.getRemoteAddr() : red.resolve(request);
        return SensitiveDataSanitizer.sanitizeForLog(ip, 80);
    }
}
