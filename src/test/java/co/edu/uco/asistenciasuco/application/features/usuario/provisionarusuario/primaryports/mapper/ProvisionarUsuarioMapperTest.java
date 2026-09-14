package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvisionarUsuarioMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ProvisionarUsuarioMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_crea_dominio() {
        final UUID tipoId = UUID.randomUUID();
        final ProvisionarUsuarioDTO dto = new ProvisionarUsuarioDTO(
                tipoId, 123456789, "PEREZ", "GOMEZ", "ANA", "MARIA", "ana@uco.edu.co", "Clave123!");

        final ProvisionarUsuarioDomain domain = ProvisionarUsuarioMapper.toDomain(dto);

        assertEquals(tipoId, domain.getTipoIdentificacionId());
        assertEquals(123456789, domain.getNumeroIdentificacion());
        assertEquals("PEREZ", domain.getPrimerApellido());
        assertEquals("ana@uco.edu.co", domain.getCorreo());
    }

    @Test
    void toDTO_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ProvisionarUsuarioMapper.toDTO(null));
    }

    @Test
    void toDTO_con_entidad_valida_mapea_campos() {
        final UUID usuarioId = UUID.randomUUID();
        final ProvisionarUsuarioResultadoEntity entity =
                new ProvisionarUsuarioResultadoEntity(usuarioId, true, "Usuario creado.");

        final ProvisionarUsuarioResultadoDTO dto = ProvisionarUsuarioMapper.toDTO(entity);

        assertEquals(usuarioId, dto.usuarioId());
        assertTrue(dto.exitoso());
        assertEquals("Usuario creado.", dto.mensajeUsuario());
    }
}
