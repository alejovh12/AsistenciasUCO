package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

@ControllerAdvice(annotations = Controller.class)
public final class AuditRequestBodyCaptureAdvice extends RequestBodyAdviceAdapter {

    private final HttpServletRequest request;

    public AuditRequestBodyCaptureAdvice(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean supports(
            final MethodParameter methodParameter,
            final Type targetType,
            final Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return methodParameter.hasMethodAnnotation(AuditableOperation.class);
    }

    @Override
    public Object afterBodyRead(
            final Object body,
            final HttpInputMessage inputMessage,
            final MethodParameter parameter,
            final Type targetType,
            final Class<? extends HttpMessageConverter<?>> converterType
    ) {
        AuditRequestAttributes.storeRequestBody(request, body);
        return body;
    }
}
