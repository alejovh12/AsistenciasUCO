package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * M-10: las configuraciones de cableado sin prueba directa se ejercitan por reflexion. Cada metodo {@code @Bean} se invoca
 * con dobles de sus dependencias y debe devolver un objeto: detecta cableados rotos (argumentos que ya no se aceptan,
 * constructores que lanzan) sin levantar el contexto de Spring.
 */
class WiringBeansSeConstruyenTest {

    private static final List<Class<?>> CONFIGURACIONES = List.of(
            DocenteWiringConfiguration.class,
            EstudianteWiringConfiguration.class,
            AsistenciaQrWiringConfiguration.class,
            TomaAsistenciaWiringConfiguration.class,
            VerificacionCorreoWiringConfiguration.class,
            IdentidadWiringConfiguration.class);

    @Test
    void cada_bean_de_las_configuraciones_se_construye_con_dobles() throws Exception {
        final List<String> fallos = new ArrayList<>();
        int beans = 0;
        for (final Class<?> clase : CONFIGURACIONES) {
            final var constructor = clase.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Object configuracion = constructor.newInstance();
            for (final Method metodo : clase.getDeclaredMethods()) {
                if (!metodo.isAnnotationPresent(Bean.class)) {
                    continue;
                }
                metodo.setAccessible(true);
                try {
                    final Object bean = metodo.invoke(Modifier.isStatic(metodo.getModifiers()) ? null : configuracion,
                            argumentos(metodo.getParameterTypes()));
                    assertNotNull(bean, clase.getSimpleName() + "." + metodo.getName());
                    beans++;
                } catch (final ReflectiveOperationException | RuntimeException exception) {
                    final Throwable causa = exception.getCause() == null ? exception : exception.getCause();
                    fallos.add(clase.getSimpleName() + "." + metodo.getName() + " -> " + causa);
                }
            }
        }
        assertEquals(List.of(), fallos);
        assertTrue(beans > 10, "Se esperaban mas beans cableados: " + beans);
    }

    private static Object[] argumentos(final Class<?>[] tipos) {
        final Object[] valores = new Object[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            valores[i] = valor(tipos[i]);
        }
        return valores;
    }

    private static Object valor(final Class<?> tipo) {
        if (tipo == boolean.class) return Boolean.FALSE;
        if (tipo == int.class) return 1;
        if (tipo == long.class) return 1L;
        if (tipo == double.class) return 1.0d;
        if (tipo == String.class) return "valor";
        if (tipo == Clock.class) return Clock.systemUTC();
        if (tipo == Duration.class) return Duration.ofSeconds(1);
        if (tipo == ObjectProvider.class) return mock(ObjectProvider.class);
        return mock(tipo);
    }
}
