package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrearDecanoIdentityMapperTest {

    @Test
    void usa_perfil_canonico_y_role_decano() {
        final UUID idUsuario = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        final CrearDecanoDomain domain = new CrearDecanoDomain(
                UUID.randomUUID(), 123456789, "Ana", "Maria", "Perez", "Gomez", "nuevo@uco.edu.co", "Clave123!", UUID.randomUUID()
        );
        final var dto = CrearDecanoIdentityMapper.toCrearCuentaIdentidadDTO(
                domain, new UsuarioIdentidadRepositoryProjection(
                        idUsuario, UUID.randomUUID(), 111111111, "JOSE", "ROJAS", "viejo@uco.edu.co"
                )
        );

        assertEquals(idUsuario, dto.idUsuario());
        assertEquals("111111111", dto.username());
        assertEquals("viejo@uco.edu.co", dto.correo());
        assertEquals("JOSE", dto.primerNombre());
        assertEquals("ROJAS", dto.primerApellido());
        assertEquals("Clave123!", dto.passwordInicial());
        assertEquals(InstitutionalRole.DECANO, dto.rolInstitucional());
    }
}
