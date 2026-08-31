package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarEstudianteDomainTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    @Test
    void construye_domain_valido_y_normaliza_texto_igual_que_crear_usuario() {
        final RegistrarEstudianteDomain domain = domainValido();
        final CrearUsuarioDomain usuario = CrearUsuarioDomain.crear(
                TIPO_IDENTIFICACION,
                123456789,
                "  perez ",
                "",
                "  ana ",
                "",
                " ANA.PEREZ@UCO.EDU.CO ",
                "Clave123!"
        );

        assertEquals(TIPO_IDENTIFICACION, domain.getTipoIdentificacionId());
        assertEquals(123456789, domain.getNumeroIdentificacion());
        assertEquals("PEREZ", domain.getPrimerApellido());
        assertEquals("ANA", domain.getPrimerNombre());
        assertEquals("ana.perez@uco.edu.co", domain.getCorreo());
        assertEquals(GRUPO, domain.getGrupoId());
        assertEquals(usuario.getTipoIdentificacionId(), domain.getTipoIdentificacionId());
        assertEquals(usuario.getNumeroIdentificacion(), domain.getNumeroIdentificacion());
        assertEquals(usuario.getPrimerApellido(), domain.getPrimerApellido());
        assertEquals(usuario.getSegundoApellido(), domain.getSegundoApellido());
        assertEquals(usuario.getPrimerNombre(), domain.getPrimerNombre());
        assertEquals(usuario.getSegundoNombre(), domain.getSegundoNombre());
        assertEquals(usuario.getCorreo(), domain.getCorreo());
        assertEquals(usuario.getPassword(), domain.getPassword());
    }

    @Test
    void rechaza_identidad_obligatoria() {
        assertThrows(ValidationException.class, () -> new RegistrarEstudianteDomain(
                null,
                123456789,
                "Perez",
                "",
                "Ana",
                "",
                "ana.perez@uco.edu.co",
                "Clave123!",
                GRUPO
        ));
        assertThrows(ValidationException.class, () -> new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                null,
                "Perez",
                "",
                "Ana",
                "",
                "ana.perez@uco.edu.co",
                "Clave123!",
                GRUPO
        ));
    }

    @Test
    void rechaza_correo_y_grupo_invalidos() {
        assertThrows(ValidationException.class, () -> new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "",
                "Ana",
                "",
                "correo-invalido",
                "Clave123!",
                GRUPO
        ));
        assertThrows(ValidationException.class, () -> new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "",
                "Ana",
                "",
                "ana.perez@uco.edu.co",
                "Clave123!",
                null
        ));
    }

    @Test
    void rechaza_numero_identificacion_nombres_correo_y_password_igual_que_crear_usuario() {
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 12345, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 0, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "A".repeat(51), "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "Perez", "", "Ana", "", "correo-invalido", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "sinreglas");
        assertMismaValidacionUsuario(EMPTY_UUID, 123456789, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
    }

    private RegistrarEstudianteDomain domainValido() {
        return new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "  perez ",
                "",
                "  ana ",
                "",
                " ANA.PEREZ@UCO.EDU.CO ",
                "Clave123!",
                GRUPO
        );
    }

    private void assertMismaValidacionUsuario(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        final ValidationException crearUsuarioException = assertThrows(
                ValidationException.class,
                () -> CrearUsuarioDomain.crear(
                        tipoIdentificacionId,
                        numeroIdentificacion,
                        primerApellido,
                        segundoApellido,
                        primerNombre,
                        segundoNombre,
                        correo,
                        password
                )
        );
        final ValidationException registrarEstudianteException = assertThrows(
                ValidationException.class,
                () -> new RegistrarEstudianteDomain(
                        tipoIdentificacionId,
                        numeroIdentificacion,
                        primerApellido,
                        segundoApellido,
                        primerNombre,
                        segundoNombre,
                        correo,
                        password,
                        GRUPO
                )
        );

        assertEquals(crearUsuarioException.getCode(), registrarEstudianteException.getCode());
        assertEquals(crearUsuarioException.getMessage(), registrarEstudianteException.getMessage());
    }
}
