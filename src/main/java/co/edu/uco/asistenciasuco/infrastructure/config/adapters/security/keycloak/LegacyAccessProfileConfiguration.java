package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.LegacySecurityProviderCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/** En modo legacy no hay perfiles externos: el rol del proveedor de identidad es el único mecanismo de acceso. */
@Configuration(proxyBeanMethods = false)
@Conditional(LegacySecurityProviderCondition.class)
public class LegacyAccessProfileConfiguration {

    @Bean
    public AccessProfilePort accessProfilePort() {
        return new AccessProfilePort() {
            private Resultado noAplica() {
                return new Resultado(Estado.NO_APLICA, "El proveedor legacy no usa perfiles externos");
            }

            @Override
            public Resultado sincronizar(final String correo, final InstitutionalRole rol) {
                return noAplica();
            }

            @Override
            public Resultado revocar(final String correo, final InstitutionalRole rol) {
                return noAplica();
            }

            @Override
            public Resultado consultar(final String correo, final InstitutionalRole rol) {
                return noAplica();
            }
        };
    }
}
