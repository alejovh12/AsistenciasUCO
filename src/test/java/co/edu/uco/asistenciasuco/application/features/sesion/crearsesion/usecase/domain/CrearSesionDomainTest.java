package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearSesionDomainTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Test
    void construye_domain_valido_y_normaliza_campos() {
        final CrearSesionDomain domain =
                new CrearSesionDomain(GRUPO, "  Tema principal  ", "  Descripcion valida de prueba  ");

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals("Tema principal", domain.getTema());
        assertEquals("Descripcion valida de prueba", domain.getDescripcion());
    }

    @Test
    void permite_descripcion_vacia_como_null() {
        final CrearSesionDomain domain = new CrearSesionDomain(GRUPO, "Tema 1", "   ");
        assertNull(domain.getDescripcion());
    }

    @Test
    void rechaza_grupo_tema_y_descripcion_invalidos() {
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(null, "Tema valido", null));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(GRUPO, null, null));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(GRUPO, "abcd", null));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(GRUPO, "a".repeat(101), null));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(GRUPO, "Tema valido", "corta"));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(GRUPO, "Tema valido", "a".repeat(251)));
    }
}
