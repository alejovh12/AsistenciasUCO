package co.edu.uco.asistenciasuco.application.features.grupo.common.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.FranjaHorarioRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FranjaHorarioMapperTest {

    @Test
    void null_en_comandos_significa_sin_cambio_de_horario() {
        assertNull(FranjaHorarioMapper.toDomains(null));
        assertNull(FranjaHorarioMapper.toRepositoryDTOs(null));
    }

    @Test
    void comando_convierte_horas_y_conserva_el_codigo_del_dia() {
        final List<FranjaHorarioDomain> domains = FranjaHorarioMapper.toDomains(List.of(new FranjaHorarioDTO("LU", "08:00", "10:30")));

        assertEquals(List.of(new FranjaHorarioDomain("LU", null, LocalTime.of(8, 0), LocalTime.of(10, 30))), domains);
        assertEquals(List.of(new FranjaHorarioRepositoryDTO("LU", LocalTime.of(8, 0), LocalTime.of(10, 30))),
                FranjaHorarioMapper.toRepositoryDTOs(domains));
    }

    @Test
    void consulta_devuelve_lista_vacia_sin_horario_y_horas_en_formato_hh_mm() {
        assertEquals(List.of(), FranjaHorarioMapper.fromProjections(null));
        assertEquals(List.of(), FranjaHorarioMapper.toDTOs(null));

        final List<FranjaHorarioDomain> domains = FranjaHorarioMapper.fromProjections(List.of(
                new FranjaHorarioRepositoryProjection(UUID.randomUUID(), "MI", "Miercoles", LocalTime.of(14, 0), LocalTime.of(16, 0))));

        assertEquals(List.of(new FranjaHorarioDTO("MI", "Miercoles", "14:00", "16:00")), FranjaHorarioMapper.toDTOs(domains));
    }
}
