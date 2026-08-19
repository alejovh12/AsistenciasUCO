package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditBodyCaptureAdviceTest {

    @Test
    void requestBodyAdvice_solo_soporta_handlers_auditables() throws Exception {
        final AuditRequestBodyCaptureAdvice advice = new AuditRequestBodyCaptureAdvice(new MockHttpServletRequest());

        assertTrue(advice.supports(parameter("auditable", 0), String.class, StringHttpMessageConverter.class));
        assertFalse(advice.supports(parameter("noAuditable", 0), String.class, StringHttpMessageConverter.class));
    }

    @Test
    void responseBodyAdvice_solo_soporta_handlers_auditables() throws Exception {
        final AuditResponseBodyCaptureAdvice advice = new AuditResponseBodyCaptureAdvice(new MockHttpServletRequest());

        assertTrue(advice.supports(parameter("auditable", -1), StringHttpMessageConverter.class));
        assertFalse(advice.supports(parameter("noAuditable", -1), StringHttpMessageConverter.class));
    }

    private MethodParameter parameter(final String methodName, final int parameterIndex) throws NoSuchMethodException {
        final Method method = Controller.class.getDeclaredMethod(methodName, String.class);
        return new MethodParameter(method, parameterIndex);
    }

    private static final class Controller {

        @AuditableOperation(action = "AUDITABLE", resourceType = "TEST")
        String auditable(final String body) {
            return body;
        }

        String noAuditable(final String body) {
            return body;
        }
    }
}
