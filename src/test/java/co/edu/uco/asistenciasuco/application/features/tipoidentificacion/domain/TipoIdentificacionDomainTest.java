package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoIdentificacionDomainTest {

    private static final UUID ID = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void reconstruir_crea_domain_valido_con_tipos_simples() {
        final TipoIdentificacionDomain domain = TipoIdentificacionDomain.reconstruir(ID, " CC ", " Cedula ");

        assertEquals(ID, domain.getId());
        assertEquals("CC", domain.getTipoIdentificacion());
        assertEquals("Cedula", domain.getNombre());
    }

    @Test
    void reconstruir_rechaza_id_codigo_y_nombre_invalidos() {
        assertThrows(IllegalArgumentException.class, () -> TipoIdentificacionDomain.reconstruir(null, "CC", "Cedula"));
        assertThrows(IllegalArgumentException.class, () -> TipoIdentificacionDomain.reconstruir(ID, "ABCDEF", "Cedula"));
        assertThrows(IllegalArgumentException.class, () -> TipoIdentificacionDomain.reconstruir(ID, "CC", ""));
    }

    @Test
    void constructor_no_es_publico_y_no_tiene_setters() {
        assertTrue(Arrays.stream(TipoIdentificacionDomain.class.getDeclaredConstructors())
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())));
        assertFalse(Arrays.stream(TipoIdentificacionDomain.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().startsWith("set")));
    }

    @Test
    void equals_y_hashcode_usan_la_identidad() {
        final TipoIdentificacionDomain primero = TipoIdentificacionDomain.reconstruir(ID, "CC", "Cedula");
        final TipoIdentificacionDomain segundo = TipoIdentificacionDomain.reconstruir(ID, "TI", "Tarjeta");

        assertEquals(primero, segundo);
        assertEquals(primero.hashCode(), segundo.hashCode());
    }
}
