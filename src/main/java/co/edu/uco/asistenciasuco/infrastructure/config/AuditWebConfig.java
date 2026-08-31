package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Objects;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    public AuditWebConfig(final AuditInterceptor auditInterceptor) {
        this.auditInterceptor = Objects.requireNonNull(auditInterceptor, "El interceptor de auditoria es obligatorio.");
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor);
    }
}