package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.observability.audit.RequestActor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public final class RequestActorResolver {

    private static final RequestActor ANONYMOUS_ACTOR = new RequestActor(null, AuditActorType.ANONYMOUS);

    public RequestActor resolve(final HttpServletRequest request) {
        final Principal principal = request.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ANONYMOUS_ACTOR;
        }
        return new RequestActor(
                SensitiveDataSanitizer.sanitizeForLog(principal.getName(), 120),
                AuditActorType.USER
        );
    }
}
