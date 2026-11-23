package co.edu.uco.asistenciasuco.testsupport;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Respuesta de Mockito que devuelve valores de muestra (no nulos) del tipo de retorno del metodo invocado: registros se
 * construyen recursivamente, las listas traen un elemento y los textos, numeros y fechas traen un valor fijo. Sirve para
 * ejercitar los mapeadores de un interactor sin armar a mano cada entidad.
 */
public final class MuestraAnswer implements Answer<Object> {

    @Override
    public Object answer(final InvocationOnMock invocation) {
        final Method metodo = invocation.getMethod();
        return muestra(metodo.getGenericReturnType(), 0);
    }

    public static Object muestra(final Type tipo, final int profundidad) {
        if (tipo instanceof ParameterizedType parametrizado) {
            final Class<?> crudo = (Class<?>) parametrizado.getRawType();
            final Type interno = parametrizado.getActualTypeArguments()[0];
            if (List.class.isAssignableFrom(crudo)) {
                final List<Object> lista = new ArrayList<>();
                lista.add(muestra(interno, profundidad + 1));
                return lista;
            }
            if (Optional.class.equals(crudo)) {
                return Optional.of(muestra(interno, profundidad + 1));
            }
            if (Map.class.isAssignableFrom(crudo)) {
                return Map.of();
            }
            return null;
        }
        if (!(tipo instanceof Class<?> clase)) {
            return null;
        }
        if (clase == void.class || clase == Void.class) {
            return null;
        }
        if (clase == String.class) return "muestra";
        if (clase == UUID.class) return UUID.fromString("00000000-0000-4000-8000-000000000001");
        if (clase == int.class || clase == Integer.class) return 1;
        if (clase == long.class || clase == Long.class) return 1L;
        if (clase == double.class || clase == Double.class) return 1.0d;
        if (clase == boolean.class || clase == Boolean.class) return Boolean.TRUE;
        if (clase == BigDecimal.class) return BigDecimal.ONE;
        if (clase == LocalDate.class) return LocalDate.of(2026, 3, 2);
        if (clase == LocalTime.class) return LocalTime.of(8, 0);
        if (clase == LocalDateTime.class) return LocalDateTime.of(2026, 3, 2, 8, 0);
        if (clase == Instant.class) return Instant.parse("2026-03-02T13:00:00Z");
        if (clase == OffsetDateTime.class) return OffsetDateTime.parse("2026-03-02T08:00:00Z");
        if (clase == byte[].class) return new byte[] {1};
        if (clase.isEnum()) return clase.getEnumConstants()[0];
        if (clase.isRecord() && profundidad < 6) {
            try {
                final RecordComponent[] componentes = clase.getRecordComponents();
                final Class<?>[] tipos = new Class<?>[componentes.length];
                final Object[] valores = new Object[componentes.length];
                for (int i = 0; i < componentes.length; i++) {
                    tipos[i] = componentes[i].getType();
                    valores[i] = muestra(componentes[i].getGenericType(), profundidad + 1);
                }
                final var constructor = clase.getDeclaredConstructor(tipos);
                constructor.setAccessible(true);
                return constructor.newInstance(valores);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return null;
            }
        }
        return null;
    }
}
