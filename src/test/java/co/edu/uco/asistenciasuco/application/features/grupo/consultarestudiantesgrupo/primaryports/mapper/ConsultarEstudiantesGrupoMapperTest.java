package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.ConsultarEstudiantesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarEstudiantesGrupoMapperTest {

    @Test
    void toDomain_mapea_grupoId() {
        final UUID grupoId = UUID.randomUUID();

        final ConsultarEstudiantesGrupoDomain domain =
                ConsultarEstudiantesGrupoMapper.toDomain(new ConsultarEstudiantesGrupoDTO(grupoId));

        assertEquals(grupoId, domain.grupoId());
    }

    @Test
    void toDTOs_mapea_entidad_completa() {
        final UUID id = UUID.randomUUID();
        final EstudianteGrupoEntity entity = new EstudianteGrupoEntity(
                id, UUID.randomUUID(), "123456789", "Ana Perez", "ana@uco.edu.co", "ACTIVO", "Activo");

        final List<EstudianteGrupoDTO> resultado = ConsultarEstudiantesGrupoMapper.toDTOs(List.of(entity));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals("Ana Perez", resultado.getFirst().nombreCompleto());
    }
}
