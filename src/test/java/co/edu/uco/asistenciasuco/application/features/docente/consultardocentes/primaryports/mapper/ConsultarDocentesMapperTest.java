package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarDocentesMapperTest {

    @Test
    void toDTO_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarDocentesMapper.toDTO(null));
    }

    @Test
    void toDTOs_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarDocentesMapper.toDTOs(null).isEmpty());
    }

    @Test
    void toDTOs_mapea_entidad_completa() {
        final UUID id = UUID.randomUUID();
        final DocenteIdentidadEntity entity = new DocenteIdentidadEntity(
                id, UUID.randomUUID(), 123456789, "Ana Perez", true);

        final List<DocenteIdentidadDTO> resultado = ConsultarDocentesMapper.toDTOs(List.of(entity));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().getId());
        assertEquals("Ana Perez", resultado.getFirst().getNombreCompleto());
        assertTrue(resultado.getFirst().isEstaActivoUsuario());
    }
}
