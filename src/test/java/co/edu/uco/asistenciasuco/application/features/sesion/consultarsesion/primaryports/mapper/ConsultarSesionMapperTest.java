package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_crea_dominio() {
        final UUID sesion = UUID.randomUUID();

        final ConsultarSesionDomain domain = ConsultarSesionMapper.toDomain(new ConsultarSesionDTO(sesion));

        assertEquals(sesion, domain.getSesion());
    }

    @Test
    void toDTO_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarSesionMapper.toDTO(null));
    }

    @Test
    void toDTO_con_entidad_valida_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final SesionConsultadaEntity entity = new SesionConsultadaEntity(
                sesion, grupo, "Tema", 1, "SES-01", 1, "G1", "Grupo 1",
                LocalDateTime.of(2026, 1, 20, 8, 0), LocalDateTime.of(2026, 1, 20, 10, 0));

        final SesionConsultadaDTO dto = ConsultarSesionMapper.toDTO(entity);

        assertEquals(sesion, dto.getSesion());
        assertEquals(grupo, dto.getGrupo());
        assertEquals("Tema", dto.getNombre());
        assertEquals("SES-01", dto.getCodigo());
    }
}
