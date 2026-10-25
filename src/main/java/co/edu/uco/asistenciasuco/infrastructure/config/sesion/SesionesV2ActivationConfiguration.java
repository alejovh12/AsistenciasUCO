package co.edu.uco.asistenciasuco.infrastructure.config.sesion;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionesV2DisabledInterceptor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Puerta OFF de compatibilidad para {@code /api/v2/sesiones/**}: solo se instala cuando se solicita
 * explicitamente {@code app.sesiones.v2.enabled=false}. Ausente o true activa la verificacion del
 * contrato DB ({@code SesionV2SchemaCompatibilityVerifier}); un valor invalido falla al arrancar
 * ({@link SesionesV2Activation}).
 */
@Configuration(proxyBeanMethods = false)
@Conditional(SesionesV2Activation.Disabled.class)
public class SesionesV2ActivationConfiguration implements WebMvcConfigurer {

    static final String[] RUTAS_V2 = {"/api/v2/sesiones", "/api/v2/sesiones/**"};

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new SesionesV2DisabledInterceptor()).addPathPatterns(RUTAS_V2);
    }
}
