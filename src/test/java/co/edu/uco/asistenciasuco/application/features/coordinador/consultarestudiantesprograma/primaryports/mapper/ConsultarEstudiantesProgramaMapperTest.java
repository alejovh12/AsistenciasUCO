package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarEstudiantesProgramaMapperTest {

    @Test
    void toDTO_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final EstudianteProgramaDomain domain = new EstudianteProgramaDomain(
                id, UUID.randomUUID(), "123456", "Ana Perez", "ana@uco.edu.co", UUID.randomUUID(), "Ingenieria");

        final EstudianteProgramaDTO dto = ConsultarEstudiantesProgramaMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals("Ana Perez", dto.nombreCompleto());
    }
}
