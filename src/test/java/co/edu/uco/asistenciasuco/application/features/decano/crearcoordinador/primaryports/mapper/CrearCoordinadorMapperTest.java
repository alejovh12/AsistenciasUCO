package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearCoordinadorMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearCoordinadorMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID programa = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final CrearCoordinadorDTO dto = new CrearCoordinadorDTO(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", programa, "HASH", usuario);

        final CrearCoordinadorDomain domain = CrearCoordinadorMapper.toDomain(dto);

        assertEquals("123456789", domain.getNumeroIdentificacion());
        assertEquals(programa, domain.getIdPrograma());
        assertEquals(usuario, domain.getUsuario());
    }
}
