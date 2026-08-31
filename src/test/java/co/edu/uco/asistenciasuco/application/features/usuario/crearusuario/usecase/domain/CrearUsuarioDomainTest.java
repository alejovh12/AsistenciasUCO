package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearUsuarioDomainTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void crear_normaliza_campos_de_texto_y_correo_igual_que_registrar_estudiante() {
        final CrearUsuarioDomain domain = CrearUsuarioDomain.crear(
                TIPO_IDENTIFICACION,
                123456789,
                "  perez ",
                "  gomez ",
                "  ana ",
                "  maria ",
                "  ANA.PEREZ@UCO.EDU.CO ",
                "Clave123!"
        );
        final RegistrarEstudianteDomain estudiante = new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "  perez ",
                "  gomez ",
                "  ana ",
                "  maria ",
                "  ANA.PEREZ@UCO.EDU.CO ",
                "Clave123!",
                GRUPO
        );

        assertEquals("PEREZ", domain.getPrimerApellido());
        assertEquals("GOMEZ", domain.getSegundoApellido());
        assertEquals("ANA", domain.getPrimerNombre());
        assertEquals("MARIA", domain.getSegundoNombre());
        assertEquals("ana.perez@uco.edu.co", domain.getCorreo());
        assertEquals(domain.getPrimerApellido(), estudiante.getPrimerApellido());
        assertEquals(domain.getSegundoApellido(), estudiante.getSegundoApellido());
        assertEquals(domain.getPrimerNombre(), estudiante.getPrimerNombre());
        assertEquals(domain.getSegundoNombre(), estudiante.getSegundoNombre());
        assertEquals(domain.getCorreo(), estudiante.getCorreo());
        assertEquals(domain.getPassword(), estudiante.getPassword());
    }

    @Test
    void constructor_convierte_segundo_nombre_y_segundo_apellido_null_a_vacio() {
        final CrearUsuarioDomain domain = usuarioValido(null, null, "Clave123!");

        assertEquals("", domain.getSegundoNombre());
        assertEquals("", domain.getSegundoApellido());
    }

    @Test
    void constructor_permite_password_null_para_flujos_sin_credencial_nueva() {
        final CrearUsuarioDomain domain = usuarioValido(null, null, null);

        assertNull(domain.getPassword());
    }

    @Test
    void constructor_rechaza_numero_identificacion_como_password() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> usuarioValido("", "", "123456789")
        );

        assertEquals("ERR_PASSWORD_IGUAL_IDENTIFICACION", exception.getCode());
    }

    @Test
    void constructor_rechaza_tipo_identificacion_null() {
        assertThrows(
                ValidationException.class,
                () -> CrearUsuarioDomain.crear(
                        null,
                        123456789,
                        "Perez",
                        "",
                        "Ana",
                        "",
                        "ana.perez@uco.edu.co",
                        "Clave123!"
                )
        );
    }

    @Test
    void constructor_rechaza_uuid_vacio_en_tipo_identificacion() {
        assertThrows(
                ValidationException.class,
                () -> CrearUsuarioDomain.crear(
                        EMPTY_UUID,
                        123456789,
                        "Perez",
                        "",
                        "Ana",
                        "",
                        "ana.perez@uco.edu.co",
                        "Clave123!"
                )
        );
    }

    @Test
    void constructor_rechaza_numero_identificacion_null_o_menor_a_seis_digitos() {
        assertThrows(
                ValidationException.class,
                () -> CrearUsuarioDomain.crear(
                        TIPO_IDENTIFICACION,
                        null,
                        "Perez",
                        "",
                        "Ana",
                        "",
                        "ana.perez@uco.edu.co",
                        "Clave123!"
                )
        );

        assertThrows(
                ValidationException.class,
                () -> CrearUsuarioDomain.crear(
                        TIPO_IDENTIFICACION,
                        12345,
                        "Perez",
                        "",
                        "Ana",
                        "",
                        "ana.perez@uco.edu.co",
                        "Clave123!"
                )
        );
    }

    @Test
    void constructor_rechaza_nombre_apellido_y_correo_invalidos() {
        assertThrows(ValidationException.class, () -> usuarioConNombre("", "Perez", "ana.perez@uco.edu.co"));
        assertThrows(ValidationException.class, () -> usuarioConNombre("Ana", "", "ana.perez@uco.edu.co"));
        assertThrows(ValidationException.class, () -> usuarioConNombre("Ana", "Perez", ""));
        assertThrows(ValidationException.class, () -> usuarioConNombre("Ana", "Perez", "correo-invalido"));
    }

    @Test
    void constructor_rechaza_longitudes_reales_de_tabla() {
        final String texto51 = "A".repeat(51);
        final String correo101 = "a".repeat(90) + "@uco.edu.co";

        assertThrows(ValidationException.class, () -> usuarioConNombre(texto51, "Perez", "ana.perez@uco.edu.co"));
        assertThrows(ValidationException.class, () -> usuarioConNombre("Ana", texto51, "ana.perez@uco.edu.co"));
        assertThrows(ValidationException.class, () -> usuarioConNombre("Ana", "Perez", correo101));
    }

    @Test
    void constructor_rechaza_password_invalido() {
        assertThrows(ValidationException.class, () -> usuarioValido("", "", ""));
        assertThrows(ValidationException.class, () -> usuarioValido("", "", "sinreglas"));
        assertThrows(ValidationException.class, () -> usuarioValido("", "", "Clave123"));
        assertThrows(ValidationException.class, () -> usuarioValido("", "", "A".repeat(256)));
    }

    @Test
    void crear_usuario_y_registrar_estudiante_rechazan_los_mismos_datos_de_usuario_con_mismo_codigo() {
        assertMismaValidacionUsuario(null, 123456789, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(EMPTY_UUID, 123456789, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 12345, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "", "", "Ana", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "Perez", "", "", "", "ana.perez@uco.edu.co", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "Perez", "", "Ana", "", "correo-invalido", "Clave123!");
        assertMismaValidacionUsuario(TIPO_IDENTIFICACION, 123456789, "Perez", "", "Ana", "", "ana.perez@uco.edu.co", "sinreglas");
    }

    private CrearUsuarioDomain usuarioValido(
            final String segundoNombre,
            final String segundoApellido,
            final String password
    ) {
        return CrearUsuarioDomain.crear(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                segundoApellido,
                "Ana",
                segundoNombre,
                "ana.perez@uco.edu.co",
                password
        );
    }

    private CrearUsuarioDomain usuarioConNombre(
            final String primerNombre,
            final String primerApellido,
            final String correo
    ) {
        return CrearUsuarioDomain.crear(
                TIPO_IDENTIFICACION,
                123456789,
                primerApellido,
                "",
                primerNombre,
                "",
                correo,
                "Clave123!"
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
