package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerarSesionesGrupoMapperTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> GenerarSesionesGrupoMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_conserva_grupo_y_usuario() {
        final GenerarSesionesGrupoDomain domain =
                GenerarSesionesGrupoMapper.toDomain(new GenerarSesionesGrupoDTO(GRUPO, USUARIO));

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals(USUARIO, domain.getUsuarioEjecutor());
    }

    @Test
    void dominio_rechaza_grupo_nulo() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> new GenerarSesionesGrupoDomain(null, USUARIO));

        assertEquals(GrupoErrorCode.ERR_GRUPO_REQUERIDO.code(), ex.getCode());
    }

    @Test
    void dominio_rechaza_usuario_ejecutor_nulo() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> new GenerarSesionesGrupoDomain(GRUPO, null));

        assertEquals(UsuarioErrorCode.ERR_USUARIO_REQUERIDO.code(), ex.getCode());
    }
}
