package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuditRequestAttributes {

    public static final String REQUEST_BODY = AuditRequestAttributes.class.getName() + ".requestBody";
    public static final String RESPONSE_BODY = AuditRequestAttributes.class.getName() + ".responseBody";
    public static final String RESOURCE_ID = AuditRequestAttributes.class.getName() + ".resourceId";
    public static final String ERROR_CODE = AuditRequestAttributes.class.getName() + ".errorCode";

    private AuditRequestAttributes() {
    }

    public static void storeRequestBody(final HttpServletRequest request, final Object body) {
        setAttribute(request, REQUEST_BODY, body);
    }

    public static void storeResponseBody(final HttpServletRequest request, final Object body) {
        setAttribute(request, RESPONSE_BODY, body);
    }

    public static void storeResourceId(final String resourceId) {
        final HttpServletRequest request = currentRequestOrNull();
        if (!ObjectHelper.isNull(request)) {
            setAttribute(request, RESOURCE_ID, resourceId);
        }
    }

    public static void storeErrorCode(final HttpServletRequest request, final String errorCode) {
        setAttribute(request, ERROR_CODE, errorCode);
    }

    public static Object requestBody(final HttpServletRequest request) {
        return request.getAttribute(REQUEST_BODY);
    }

    public static Object responseBody(final HttpServletRequest request) {
        return request.getAttribute(RESPONSE_BODY);
    }

    public static String resourceId(final HttpServletRequest request) {
        final Object value = request.getAttribute(RESOURCE_ID);
        return value == null ? null : String.valueOf(value);
    }

    public static String errorCode(final HttpServletRequest request) {
        final Object value = request.getAttribute(ERROR_CODE);
        return value == null ? null : String.valueOf(value);
    }

    private static void setAttribute(final HttpServletRequest request, final String key, final Object value) {
        if (!ObjectHelper.isNull(request)) {
            request.setAttribute(key, value);
        }
    }

    private static HttpServletRequest currentRequestOrNull() {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
