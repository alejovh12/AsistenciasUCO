package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionarAsignaturaMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> GestionarAsignaturaMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final GuardarAsignaturaDTO dto = new GuardarAsignaturaDTO(
                id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", usuarioEjecutor
        );

        final AsignaturaDomain domain = GestionarAsignaturaMapper.toDomain(dto);

        assertEquals(id, domain.idAsignatura());
        assertEquals("COD1", domain.codigo());
        assertEquals(plan, domain.idPlanEstudio());
        assertEquals(usuarioEjecutor, domain.usuarioEjecutor());
    }
}
