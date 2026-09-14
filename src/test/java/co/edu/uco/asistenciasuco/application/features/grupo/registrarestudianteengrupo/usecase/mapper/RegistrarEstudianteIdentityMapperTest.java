package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrarEstudianteIdentityMapperTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ID_USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void toCrearCuentaIdentidadDTO_mapea_datos_del_dominio_con_rol_estudiante() {
        final RegistrarEstudianteDomain domain = new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION, 123456789, "Perez", "Gomez", "Ana", "Maria",
                "ana.perez@uco.edu.co", "Clave123!", GRUPO
        );

        final CrearCuentaIdentidadDTO dto = RegistrarEstudianteIdentityMapper.toCrearCuentaIdentidadDTO(
                domain, new UsuarioIdentidadRepositoryProjection(ID_USUARIO, TIPO_IDENTIFICACION, 123456789, "ANA", "PEREZ", "ana.perez@uco.edu.co"), InstitutionalRole.ESTUDIANTE
        );

        assertEquals("123456789", dto.username());
        assertEquals(ID_USUARIO, dto.idUsuario());
        assertEquals("ana.perez@uco.edu.co", dto.correo());
        assertEquals("ANA", dto.primerNombre());
        assertEquals("PEREZ", dto.primerApellido());
        assertEquals("Clave123!", dto.passwordInicial());
        assertEquals(InstitutionalRole.ESTUDIANTE, dto.rolInstitucional());
    }

    @Test
    void toCrearCuentaIdentidadDTO_propaga_password_nulo_sin_inventar_credencial() {
        final RegistrarEstudianteDomain domain = new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION, 123456789, "Perez", "Gomez", "Ana", "Maria",
                "ana.perez@uco.edu.co", null, GRUPO
        );

        final CrearCuentaIdentidadDTO dto = RegistrarEstudianteIdentityMapper.toCrearCuentaIdentidadDTO(
                domain, new UsuarioIdentidadRepositoryProjection(ID_USUARIO, TIPO_IDENTIFICACION, 123456789, "ANA", "PEREZ", "ana.perez@uco.edu.co"), InstitutionalRole.ESTUDIANTE
        );

        assertEquals(null, dto.passwordInicial());
    }
}
