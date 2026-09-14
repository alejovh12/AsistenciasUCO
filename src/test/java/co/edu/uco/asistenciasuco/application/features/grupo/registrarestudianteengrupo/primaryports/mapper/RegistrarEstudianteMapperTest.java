package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrarEstudianteMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> RegistrarEstudianteMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID tipoId = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO(
                tipoId, 123456789, "PEREZ", "GOMEZ", "ANA", "MARIA", "ana@uco.edu.co", "Clave123!", grupoId);

        final RegistrarEstudianteDomain domain = RegistrarEstudianteMapper.toDomain(dto);

        assertEquals(tipoId, domain.getTipoIdentificacionId());
        assertEquals(grupoId, domain.getGrupoId());
        assertEquals("PEREZ", domain.getPrimerApellido());
    }

    @Test
    void toDTO_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> RegistrarEstudianteMapper.toDTO(null));
    }

    @Test
    void toDTO_con_entidad_valida_mapea_campos() {
        final RegistrarEstudianteResultadoEntity entity = new RegistrarEstudianteResultadoEntity(true, "Registrado.");

        final RegistrarEstudianteResultadoDTO dto = RegistrarEstudianteMapper.toDTO(entity);

        assertTrue(dto.isExitoso());
        assertEquals("Registrado.", dto.getMensajeUsuario());
    }
}
