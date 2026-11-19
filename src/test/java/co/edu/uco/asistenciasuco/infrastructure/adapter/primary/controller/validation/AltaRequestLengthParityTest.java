package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminPortalController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.AdmitirEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.AdmitirEstudianteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.CrearDecanoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.VincularDocenteProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.VincularDocenteProgramaRequestValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F2A-DEF-002: Bean Validation no puede rechazar lo que acepta el validator propio tras trim. */
class AltaRequestLengthParityTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final jakarta.validation.Validator BEAN_VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void cerrarValidador() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void admision_estudiante_acepta_nombre_y_correo_maximos_tras_trim_con_espacios() {
        final AdmitirEstudianteRequest enLimite = estudiante();
        enLimite.setPrimerNombre(conEspacios(nombre(50)));
        enLimite.setSegundoNombre(conEspacios(nombre(50)));
        enLimite.setPrimerApellido(conEspacios(nombre(50)));
        enLimite.setSegundoApellido(conEspacios(nombre(50)));
        enLimite.setCorreo(conEspacios(correo(255)));

        assertTrue(new AdmitirEstudianteRequestValidator().validate(enLimite).isValid());
        assertBeanValidationAccepts(enLimite);
    }

    @Test
    void admision_estudiante_rechaza_nombre_y_correo_max_mas_uno_en_ambas_barreras() {
        final AdmitirEstudianteRequest excedido = estudiante();
        excedido.setPrimerNombre(nombre(51));
        excedido.setSegundoNombre(nombre(51));
        excedido.setPrimerApellido(nombre(51));
        excedido.setSegundoApellido(nombre(51));
        excedido.setCorreo(correo(256));

        assertLongitudInvalida(new AdmitirEstudianteRequestValidator().validate(excedido),
                "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
        assertBeanViolatesFields(excedido, "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
    }

    @Test
    void alta_docente_acepta_nombre_y_correo_maximos_tras_trim_con_espacios() {
        final VincularDocenteProgramaRequest enLimite = docente();
        enLimite.setPrimerNombre(conEspacios(nombre(50)));
        enLimite.setSegundoNombre(conEspacios(nombre(50)));
        enLimite.setPrimerApellido(conEspacios(nombre(50)));
        enLimite.setSegundoApellido(conEspacios(nombre(50)));
        enLimite.setCorreo(conEspacios(correo(100)));

        assertTrue(new VincularDocenteProgramaRequestValidator().validate(enLimite).isValid());
        assertBeanValidationAccepts(enLimite);
    }

    @Test
    void alta_docente_rechaza_nombre_y_correo_max_mas_uno_en_ambas_barreras() {
        final VincularDocenteProgramaRequest excedido = docente();
        excedido.setPrimerNombre(nombre(51));
        excedido.setSegundoNombre(nombre(51));
        excedido.setPrimerApellido(nombre(51));
        excedido.setSegundoApellido(nombre(51));
        excedido.setCorreo(correo(101));

        assertLongitudInvalida(new VincularDocenteProgramaRequestValidator().validate(excedido),
                "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
        assertBeanViolatesFields(excedido, "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
    }

    @Test
    void alta_decano_acepta_nombre_y_correo_maximos_tras_trim_con_espacios() {
        final AdminPortalController.CrearDecanoRequest acolchado = decano(conEspacios(nombre(50)), conEspacios(nombre(50)),
                conEspacios(nombre(50)), conEspacios(nombre(50)), conEspacios(correo(255)));

        assertTrue(new CrearDecanoRequestValidator().validate(acolchado).isValid());
        assertBeanValidationAccepts(acolchado);
    }

    @Test
    void alta_decano_rechaza_nombre_y_correo_max_mas_uno_en_ambas_barreras() {
        final AdminPortalController.CrearDecanoRequest excedido = decano(nombre(51), nombre(51), nombre(51), nombre(51), correo(256));

        assertLongitudInvalida(new CrearDecanoRequestValidator().validate(excedido),
                "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
        assertBeanViolatesFields(excedido, "primerNombre", "segundoNombre", "primerApellido", "segundoApellido", "correo");
    }

    private static AdmitirEstudianteRequest estudiante() {
        final AdmitirEstudianteRequest request = new AdmitirEstudianteRequest();
        request.setTipoIdentificacionId(UUID.randomUUID());
        request.setNumeroIdentificacion(123456789);
        request.setPrimerNombre("Ana");
        request.setPrimerApellido("Perez");
        request.setCorreo("ana@uco.edu.co");
        request.setIdPrograma(UUID.randomUUID());
        return request;
    }

    private static VincularDocenteProgramaRequest docente() {
        final VincularDocenteProgramaRequest request = new VincularDocenteProgramaRequest();
        request.setIdTipoIdentificacion(UUID.randomUUID());
        request.setNumeroIdentificacion("1017555444");
        request.setPrimerNombre("Jorge");
        request.setPrimerApellido("Henao");
        request.setCorreo("jorge.henao@uco.edu.co");
        return request;
    }

    private static AdminPortalController.CrearDecanoRequest decano(final String primerNombre, final String segundoNombre,
                                                                   final String primerApellido, final String segundoApellido,
                                                                   final String correo) {
        return new AdminPortalController.CrearDecanoRequest(UUID.randomUUID(), 123456789, primerNombre, segundoNombre,
                primerApellido, segundoApellido, correo, UUID.randomUUID(), null);
    }

    private static String nombre(final int longitud) {
        return "A".repeat(longitud);
    }

    private static String correo(final int longitud) {
        final String dominio = "@uco.edu.co";
        assertTrue(longitud > dominio.length());
        final String correo = "a".repeat(longitud - dominio.length()) + dominio;
        assertEquals(longitud, correo.length());
        return correo;
    }

    private static String conEspacios(final String valor) {
        return " " + valor + " ";
    }

    private static void assertBeanValidationAccepts(final Object request) {
        final Set<ConstraintViolation<Object>> violations = BEAN_VALIDATOR.validate(request);
        assertTrue(violations.isEmpty(), () -> "Bean Validation debe aceptar el request en el máximo recortado: " + violations);
    }

    private static void assertBeanViolatesFields(final Object request, final String... campos) {
        final Set<ConstraintViolation<Object>> violations = BEAN_VALIDATOR.validate(request);
        assertAll("Bean Validation debe rechazar max+1 en cada campo",
                java.util.Arrays.stream(campos).map(campo -> () -> assertTrue(
                        violations.stream().anyMatch(v -> campo.equals(v.getPropertyPath().toString())),
                        () -> "Falta violación Bean Validation para " + campo + ": " + violations)));
    }

    private static void assertLongitudInvalida(final ValidationResult result, final String... campos) {
        assertTrue(campos.length > 0);
        assertAll("El validator propio debe informar INVALID_LENGTH para cada campo",
                java.util.Arrays.stream(campos).map(campo -> () -> assertTrue(
                        result.issues().stream().anyMatch(issue -> campo.equals(issue.field())
                                && issue.type() == ValidationErrorType.INVALID_LENGTH),
                        () -> "Falta INVALID_LENGTH para " + campo + ": " + result.issues())));
    }
}
