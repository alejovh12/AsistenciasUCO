package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrearCoordinadorDomainTest {

    @Test
    void constructor_asigna_todos_los_campos() {
        final UUID programa = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();

        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ",
                "ana@uco.edu.co", programa, "HASH", usuario);

        assertEquals("123456789", domain.getNumeroIdentificacion());
        assertEquals("ANA", domain.getPrimerNombre());
        assertEquals("MARIA", domain.getSegundoNombre());
        assertEquals("PEREZ", domain.getPrimerApellido());
        assertEquals("GOMEZ", domain.getSegundoApellido());
        assertEquals("ana@uco.edu.co", domain.getCorreo());
        assertEquals(programa, domain.getIdPrograma());
        assertEquals("HASH", domain.getPassword());
        assertEquals(usuario, domain.getUsuario());
    }
}
