package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearGrupoMapperTest {

    @Test
    void toDomain_con_generarSesionesAutomaticas_true_lo_mapea() {
        final UUID asignatura = UUID.randomUUID();
        final CrearGrupoDTO dto = new CrearGrupoDTO(asignatura, UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), "Aula 1", true);

        final CrearGrupoDomain domain = CrearGrupoMapper.toDomain(dto);

        assertEquals(asignatura, domain.idAsignatura());
        assertTrue(domain.generarSesionesAutomaticas());
    }

    @Test
    void toDomain_con_generarSesionesAutomaticas_nulo_lo_normaliza_a_falso() {
        final CrearGrupoDTO dto = new CrearGrupoDTO(UUID.randomUUID(), UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), "Aula 1", null);

        final CrearGrupoDomain domain = CrearGrupoMapper.toDomain(dto);

        assertFalse(domain.generarSesionesAutomaticas());
    }

    @Test
    void toDTO_mapea_entidad_resultado() {
        final UUID id = UUID.randomUUID();
        final CrearGrupoResultadoEntity entity = new CrearGrupoResultadoEntity(id, "Grupo creado.");

        final CrearGrupoResultadoDTO dto = CrearGrupoMapper.toDTO(entity);

        assertEquals(id, dto.id());
        assertEquals("Grupo creado.", dto.mensajeUsuario());
    }
}
