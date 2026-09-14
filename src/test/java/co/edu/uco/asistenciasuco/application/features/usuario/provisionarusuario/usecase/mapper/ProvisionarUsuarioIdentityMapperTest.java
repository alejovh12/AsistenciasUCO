package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProvisionarUsuarioIdentityMapperTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO_ID = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void toCrearCuentaIdentidadDTO_transporta_el_rol_como_enum_institucional() {
        final CrearCuentaIdentidadDTO dto = ProvisionarUsuarioIdentityMapper.toCrearCuentaIdentidadDTO(
                domainValido(), USUARIO_ID, InstitutionalRole.ESTUDIANTE
        );

        assertEquals(InstitutionalRole.ESTUDIANTE, dto.rolInstitucional());
        assertEquals(USUARIO_ID, dto.idUsuario());
        assertEquals("ana.perez@uco.edu.co", dto.correo());
    }

    @Test
    void toResultadoEntity_usa_unicamente_el_mensaje_de_la_creacion_en_db() {
        final CrearUsuarioResultadoEntity usuario = new CrearUsuarioResultadoEntity(
                USUARIO_ID, true, "Usuario creado exitosamente."
        );

        final ProvisionarUsuarioResultadoEntity resultado = ProvisionarUsuarioIdentityMapper.toResultadoEntity(usuario);

        assertEquals("Usuario creado exitosamente.", resultado.getMensajeUsuario());
        assertEquals(USUARIO_ID, resultado.getUsuarioId());
    }

    private ProvisionarUsuarioDomain domainValido() {
        return ProvisionarUsuarioDomain.crear(
                TIPO_IDENTIFICACION, 123456789, "Perez", "Gomez", "Ana", "Maria",
                "ana.perez@uco.edu.co", "Clave123!"
        );
    }
}
