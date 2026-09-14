package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarEstudiantesProgramaRepositoryMapperTest {

    @Test
    void toDomain_mapea_todos_los_campos() {
        final UUID id = UUID.randomUUID();
        final EstudianteProgramaProjection projection = new EstudianteProgramaProjection(
                id, UUID.randomUUID(), "123456", "Ana Perez", "ana@uco.edu.co", UUID.randomUUID(), "Ingenieria");

        final EstudianteProgramaDomain domain = ConsultarEstudiantesProgramaRepositoryMapper.toDomain(projection);

        assertEquals(id, domain.id());
        assertEquals("Ana Perez", domain.nombreCompleto());
    }
}
