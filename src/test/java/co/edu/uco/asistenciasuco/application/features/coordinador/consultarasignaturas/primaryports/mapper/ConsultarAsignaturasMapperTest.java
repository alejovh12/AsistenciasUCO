package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.domain.AsignaturaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsignaturasMapperTest {

    @Test
    void toDTO_mapea_todos_los_campos_del_dominio() {
        final UUID id = UUID.randomUUID();
        final AsignaturaDomain domain = new AsignaturaDomain(
                id, "COD1", "Calculo", 3, UUID.randomUUID(), "Area",
                UUID.randomUUID(), "Componente", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Programa", "S1", true, "Activa");

        final AsignaturaDTO dto = ConsultarAsignaturasMapper.toDTO(domain);

        assertEquals(id, dto.id());
        assertEquals("COD1", dto.codigo());
        assertEquals("Calculo", dto.nombre());
        assertTrue(dto.estaActivaAsignatura());
    }
}
