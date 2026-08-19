package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditOutcome;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.audit.RequestActor;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.tracing.TraceContextSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public final class AuditInterceptor implements HandlerInterceptor {

    private final AuditEventPublisher auditEventPublisher;
    private final ClientIpResolver clientIpResolver;
    private final RequestActorResolver actorResolver;

    public AuditInterceptor(
            final AuditEventPublisher auditEventPublisher,
            final ClientIpResolver clientIpResolver,
            final RequestActorResolver actorResolver
    ) {
        if (ObjectHelper.isNull(auditEventPublisher)) {
            throw new CrosscuttingException("El publicador de auditoria es obligatorio.");
        }
        if (ObjectHelper.isNull(clientIpResolver)) {
            throw new CrosscuttingException("El resolvedor de IP de cliente es obligatorio.");
        }
        if (ObjectHelper.isNull(actorResolver)) {
            throw new CrosscuttingException("El resolvedor de actor del request es obligatorio.");
        }
        this.auditEventPublisher = auditEventPublisher;
        this.clientIpResolver = clientIpResolver;
        this.actorResolver = actorResolver;
    }

    @Override
    public void afterCompletion(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler,
            final Exception exception
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        final AuditableOperation auditableOperation = handlerMethod.getMethodAnnotation(AuditableOperation.class);
        if (auditableOperation == null) {
            return;
        }

        final RequestActor actor = actorResolver.resolve(request);
        final TraceContextSnapshot traceContext = readTraceContext();
        auditEventPublisher.publish(new AuditEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                actor.actorId(),
                actor.actorType(),
                auditableOperation.action(),
                auditableOperation.resourceType(),
                resolveResourceId(request, auditableOperation),
                CorrelationIdContext.getAsString(),
                traceContext.traceId(),
                traceContext.spanId(),
                safe(request.getMethod(), 16),
                safe(request.getRequestURI(), 240),
                response.getStatus(),
                clientIpResolver.resolve(request),
                safe(AuditRequestAttributes.errorCode(request), 120),
                resolveOutcome(response, exception),
                metadata()
        ));
    }

    private AuditOutcome resolveOutcome(final HttpServletResponse response, final Exception exception) {
        if (exception != null || response.getStatus() >= 400) {
            return AuditOutcome.FAILURE;
        }
        return AuditOutcome.SUCCESS;
    }

    private String resolveResourceId(
            final HttpServletRequest request,
            final AuditableOperation auditableOperation
    ) {
        final String explicitResourceId = safe(AuditRequestAttributes.resourceId(request), 120);
        if (!TextHelper.isNullOrBlank(explicitResourceId)) {
            return explicitResourceId;
        }

        if (TextHelper.isNullOrBlank(auditableOperation.resourceIdPathVariable())) {
            return resolveResourceIdFromBody(request, auditableOperation);
        }
        final Object pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(pathVariables instanceof Map<?, ?> variables)) {
            return resolveResourceIdFromBody(request, auditableOperation);
        }
        final String resourceId = safe(variables.get(auditableOperation.resourceIdPathVariable()), 120);
        if (!TextHelper.isNullOrBlank(resourceId)) {
            return resourceId;
        }
        return resolveResourceIdFromBody(request, auditableOperation);
    }

    private String resolveResourceIdFromBody(
            final HttpServletRequest request,
            final AuditableOperation auditableOperation
    ) {
        final String fromResponse = resolveProperty(AuditRequestAttributes.responseBody(request), auditableOperation.resourceIdResponseField());
        if (!TextHelper.isNullOrBlank(fromResponse)) {
            return fromResponse;
        }
        return resolveProperty(AuditRequestAttributes.requestBody(request), auditableOperation.resourceIdRequestField());
    }

    private String resolveProperty(final Object target, final String property) {
        if (ObjectHelper.isNull(target) || TextHelper.isNullOrBlank(property)) {
            return null;
        }
        final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(target);
        if (!beanWrapper.isReadableProperty(property)) {
            return null;
        }
        return safe(beanWrapper.getPropertyValue(property), 120);
    }

    private TraceContextSnapshot readTraceContext() {
        return new TraceContextSnapshot(
                safe(org.slf4j.MDC.get(TraceContextSnapshot.TRACE_ID_MDC_KEY), 80),
                safe(org.slf4j.MDC.get(TraceContextSnapshot.SPAN_ID_MDC_KEY), 80)
        );
    }

    private Map<String, String> metadata() {
        return Map.of("handlerType", "HTTP");
    }

    private String safe(final Object value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }
}
