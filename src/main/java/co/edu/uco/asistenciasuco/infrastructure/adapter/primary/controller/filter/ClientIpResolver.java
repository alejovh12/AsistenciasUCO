package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public final class ClientIpResolver {

    public String resolve(final HttpServletRequest request) {
        return SensitiveDataSanitizer.sanitizeForLog(request.getRemoteAddr(), 80);
    }
}
