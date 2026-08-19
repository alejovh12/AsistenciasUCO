package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    public AuditWebConfig(final AuditInterceptor auditInterceptor) {
        if (ObjectHelper.isNull(auditInterceptor)) {
            throw new CrosscuttingException("El interceptor de auditoria es obligatorio.");
        }
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor);
    }
}
