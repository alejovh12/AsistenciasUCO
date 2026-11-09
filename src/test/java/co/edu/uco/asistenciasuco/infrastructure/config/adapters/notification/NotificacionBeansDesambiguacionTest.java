package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification.smtp.SmtpNotificacionAdapterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QUALITY-SONAR-RELIABILITY (java:S9352): cuando dos Composition Roots declaran beans del mismo tipo
 * (Admin y Smtp), todo punto de inyeccion de la configuracion SMTP debe resolverse sin ambiguedad,
 * es decir, exactamente un candidato marcado {@code @Primary}.
 */
class NotificacionBeansDesambiguacionTest {

    private static final List<Class<?>> COMPOSITION_ROOTS = List.of(
            AdminNotificacionAdapterConfiguration.class,
            SmtpNotificacionAdapterConfiguration.class
    );

    @Test
    @DisplayName("S9352: cada dependencia concreta de la configuracion SMTP con varios candidatos tiene un unico @Primary")
    void dependencias_de_smtp_con_varios_candidatos_tienen_un_unico_primary() {
        final List<String> ambiguas = new ArrayList<>();
        for (final Method factory : beanMethods(SmtpNotificacionAdapterConfiguration.class)) {
            for (final Parameter dependencia : factory.getParameters()) {
                final List<Method> candidatos = candidatosAsignables(dependencia.getType());
                final long primarios = candidatos.stream().filter(m -> m.isAnnotationPresent(Primary.class)).count();
                if (candidatos.size() > 1 && primarios != 1) {
                    ambiguas.add(factory.getName() + "(" + dependencia.getType().getSimpleName() + ") candidatos="
                            + candidatos.stream().map(Method::getName).toList() + " primarios=" + primarios);
                }
            }
        }
        assertThat(ambiguas)
                .as("dependencias ambiguas sin @Primary unico")
                .isEmpty();
    }

    @Test
    @DisplayName("S9352: las dependencias de tipo repositorio y catalogo de plantillas si tienen varios candidatos (el guard es efectivo)")
    void el_guard_detecta_los_candidatos_duplicados_conocidos() {
        final List<String> tipos = List.of("PlantillaNotificacionRepository", "PlantillaPorDefectoCatalogo");
        for (final String tipo : tipos) {
            final long candidatos = beanMethods(AdminNotificacionAdapterConfiguration.class).stream()
                    .filter(m -> m.getReturnType().getSimpleName().startsWith("Plantilla"))
                    .filter(m -> tipoAsignable(tipo, m.getReturnType()))
                    .count()
                    + beanMethods(SmtpNotificacionAdapterConfiguration.class).stream()
                    .filter(m -> tipoAsignable(tipo, m.getReturnType()))
                    .count();
            assertThat(candidatos).as("candidatos para " + tipo).isGreaterThan(1);
        }
    }

    private static boolean tipoAsignable(final String nombreTipo, final Class<?> retorno) {
        Class<?> actual = retorno;
        while (actual != null) {
            if (actual.getSimpleName().equals(nombreTipo)) {
                return true;
            }
            for (final Class<?> interfaz : actual.getInterfaces()) {
                if (interfaz.getSimpleName().equals(nombreTipo)) {
                    return true;
                }
            }
            actual = actual.getSuperclass();
        }
        return false;
    }

    private static List<Method> candidatosAsignables(final Class<?> tipo) {
        final List<Method> candidatos = new ArrayList<>();
        for (final Class<?> raiz : COMPOSITION_ROOTS) {
            for (final Method metodo : beanMethods(raiz)) {
                if (tipo.isAssignableFrom(metodo.getReturnType())) {
                    candidatos.add(metodo);
                }
            }
        }
        return candidatos;
    }

    private static List<Method> beanMethods(final Class<?> raiz) {
        final List<Method> metodos = new ArrayList<>();
        for (final Method metodo : raiz.getDeclaredMethods()) {
            if (metodo.isAnnotationPresent(Bean.class)) {
                metodos.add(metodo);
            }
        }
        return metodos;
    }
}
