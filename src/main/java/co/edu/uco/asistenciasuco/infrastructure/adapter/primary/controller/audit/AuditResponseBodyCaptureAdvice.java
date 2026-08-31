package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = Controller.class)
public final class AuditResponseBodyCaptureAdvice implements ResponseBodyAdvice<Object> {

    private final HttpServletRequest request;

    public AuditResponseBodyCaptureAdvice(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean supports(
            final MethodParameter returnType,
            final Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return returnType.hasMethodAnnotation(AuditableOperation.class);
    }

    @Override
    public Object beforeBodyWrite(
            final Object body,
            final MethodParameter returnType,
            final MediaType selectedContentType,
            final Class<? extends HttpMessageConverter<?>> selectedConverterType,
            final ServerHttpRequest serverHttpRequest,
            final ServerHttpResponse serverHttpResponse
    ) {
        AuditRequestAttributes.storeResponseBody(request, body);
        return body;
    }
}
