package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarSesionRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarSesionRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_sesion() {
        final UUID sesion = UUID.randomUUID();

        final ConsultarSesionRepositoryDTO dto =
                ConsultarSesionRepositoryMapper.toRepositoryDTO(new ConsultarSesionDomain(sesion));

        assertEquals(sesion, dto.getSesion());
    }

    @Test
    void toUseCaseEntity_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarSesionRepositoryMapper.toUseCaseEntity(null));
    }

    @Test
    void toUseCaseEntity_con_proyeccion_valida_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final SesionRepositoryProjection projection = new SesionRepositoryProjection(
                sesion, grupo, "Tema", 1, "SES-01", 1, "G1", "Grupo 1",
                LocalDateTime.of(2026, 1, 20, 8, 0), LocalDateTime.of(2026, 1, 20, 10, 0));

        final SesionConsultadaEntity entity = ConsultarSesionRepositoryMapper.toUseCaseEntity(projection);

        assertEquals(sesion, entity.getSesion());
        assertEquals(grupo, entity.getGrupo());
        assertEquals("Tema", entity.getNombre());
    }
}
