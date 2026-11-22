package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.servicio.ServicioNotificacionesAdapterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QUALITY-SONAR-RELIABILITY (java:S9352): el modulo de notificaciones tiene un unico Composition Root con beans propios
 * (F4: ya no coexisten la configuracion de administracion y la del canal SMTP). Cada dependencia concreta de sus fabricas
 * debe tener a lo sumo un candidato dentro de ese Composition Root, de modo que ninguna se resuelva por ambiguedad.
 */
class NotificacionBeansDesambiguacionTest {

    @Test
    @DisplayName("S9352: cada dependencia de las fabricas del servicio de notificaciones tiene a lo sumo un candidato en su Composition Root")
    void las_dependencias_no_tienen_candidatos_ambiguos() {
        final List<String> ambiguas = new ArrayList<>();
        for (final Method factory : beanMethods()) {
            for (final Parameter dependencia : factory.getParameters()) {
                final List<String> candidatos = beanMethods().stream()
                        .filter(m -> dependencia.getType().isAssignableFrom(m.getReturnType()))
                        .map(Method::getName)
                        .toList();
                if (candidatos.size() > 1) {
                    ambiguas.add(factory.getName() + "(" + dependencia.getType().getSimpleName() + ") candidatos=" + candidatos);
                }
            }
        }
        assertThat(ambiguas).as("dependencias con mas de un candidato").isEmpty();
    }

    @Test
    @DisplayName("S9352: el guard es efectivo: hay fabricas con dependencias que resolver")
    void el_guard_revisa_fabricas_reales() {
        assertThat(beanMethods()).isNotEmpty();
        assertThat(beanMethods().stream().flatMap(m -> java.util.Arrays.stream(m.getParameters())).count()).isGreaterThan(5);
    }

    private static List<Method> beanMethods() {
        final List<Method> metodos = new ArrayList<>();
        for (final Method metodo : ServicioNotificacionesAdapterConfiguration.class.getDeclaredMethods()) {
            if (metodo.isAnnotationPresent(Bean.class)) {
                metodos.add(metodo);
            }
        }
        return metodos;
    }
}
