package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarGruposMapperTest {

    @Test
    void toDTOs_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarGruposMapper.toDTOs(null).isEmpty());
    }

    @Test
    void toDTOs_mapea_entidad_completa() {
        final UUID id = UUID.randomUUID();
        final GrupoEntity entity = new GrupoEntity(
                id, "G1", "Grupo 1", UUID.randomUUID(), "Backend", UUID.randomUUID(),
                30, 12, 18, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 30));

        final List<GrupoDTO> resultado = ConsultarGruposMapper.toDTOs(List.of(entity));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().getId());
        assertEquals("Backend", resultado.getFirst().getNombreAsignatura());
        assertTrue(resultado.getFirst().isGrupoHabilitado());
    }
}
