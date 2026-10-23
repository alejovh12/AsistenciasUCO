package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.VincularDocenteProgramaRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HU091: el validator rechaza con 400 antes de llegar al caso de uso. */
class VincularDocenteProgramaRequestValidatorTest {

    private final VincularDocenteProgramaRequestValidator validator = new VincularDocenteProgramaRequestValidator();

    @Test
    void acepta_modo_existente_y_modo_nuevo_validos() {
        assertTrue(validator.validate(existente()).isValid());
        assertTrue(validator.validate(nuevo()).isValid());
    }

    @Test
    void rechaza_request_nulo_y_tipo_de_vinculacion_ausente() {
        assertIssue(validator.validate(null), "request", ValidationErrorType.REQUIRED);
        final VincularDocenteProgramaRequest request = existente();
        request.setIdTipoVinculacion(null);
        assertIssue(validator.validate(request), "idTipoVinculacion", ValidationErrorType.REQUIRED);
    }

    @Test
    void T_091_06_exige_exactamente_un_modo() {
        assertIssue(validator.validate(new VincularDocenteProgramaRequest()), "modo", ValidationErrorType.REQUIRED);

        final VincularDocenteProgramaRequest ambos = nuevo();
        ambos.setIdDocente(UUID.randomUUID());
        assertIssue(validator.validate(ambos), "modo", ValidationErrorType.INVALID_VALUE);
    }

    @Test
    void T_091_14_modo_nuevo_exige_password_y_que_cumpla_la_politica() {
        final VincularDocenteProgramaRequest sinPassword = nuevo();
        sinPassword.setPassword(null);
        assertIssue(validator.validate(sinPassword), "password", ValidationErrorType.REQUIRED);

        final VincularDocenteProgramaRequest debil = nuevo();
        debil.setPassword("abc");
        assertIssue(validator.validate(debil), "password", ValidationErrorType.INVALID_FORMAT);
    }

    @Test
    void T_091_15_documento_numerico_dentro_del_rango_int() {
        final VincularDocenteProgramaRequest noNumerico = nuevo();
        noNumerico.setNumeroIdentificacion("12ab");
        assertIssue(validator.validate(noNumerico), "numeroIdentificacion", ValidationErrorType.INVALID_FORMAT);

        final VincularDocenteProgramaRequest fueraDeRango = nuevo();
        fueraDeRango.setNumeroIdentificacion("2147483648");
        assertIssue(validator.validate(fueraDeRango), "numeroIdentificacion", ValidationErrorType.OUT_OF_RANGE);
    }

    @Test
    void correo_no_es_vulnerable_a_backtracking_y_valida_dominios_con_subdominios() {
        final VincularDocenteProgramaRequest subdominio = nuevo();
        subdominio.setCorreo("docente@mail.uco.edu.co");
        assertTrue(validator.validate(subdominio).isValid());

        final VincularDocenteProgramaRequest etiquetaVacia = nuevo();
        etiquetaVacia.setCorreo("docente@uco..edu");
        assertIssue(validator.validate(etiquetaVacia), "correo", ValidationErrorType.INVALID_FORMAT);

        final VincularDocenteProgramaRequest hostil = nuevo();
        hostil.setCorreo("!@!." + "!.".repeat(48));
        final long inicio = System.nanoTime();
        assertIssue(validator.validate(hostil), "correo", ValidationErrorType.INVALID_FORMAT);
        assertTrue(System.nanoTime() - inicio < 500_000_000L, "la validacion de correo debe ser de tiempo lineal");
    }
    @Test
    void modo_nuevo_exige_campos_obligatorios_correo_valido_y_longitudes() {
        final VincularDocenteProgramaRequest vacio = nuevo();
        vacio.setPrimerNombre(" ");
        vacio.setPrimerApellido(null);
        vacio.setCorreo("no-es-correo");
        vacio.setIdTipoIdentificacion(null);
        final ValidationResult result = validator.validate(vacio);
        assertIssue(result, "primerNombre", ValidationErrorType.REQUIRED);
        assertIssue(result, "primerApellido", ValidationErrorType.REQUIRED);
        assertIssue(result, "correo", ValidationErrorType.INVALID_FORMAT);
        assertIssue(result, "idTipoIdentificacion", ValidationErrorType.REQUIRED);

        final VincularDocenteProgramaRequest largo = nuevo();
        largo.setPrimerNombre("x".repeat(51));
        assertIssue(validator.validate(largo), "primerNombre", ValidationErrorType.INVALID_LENGTH);
    }

    @Test
    void uuids_nulos_o_vacios_se_rechazan() {
        final VincularDocenteProgramaRequest request = existente();
        request.setIdDocente(new UUID(0, 0));
        assertIssue(validator.validate(request), "idDocente", ValidationErrorType.INVALID_UUID);
    }

    private static VincularDocenteProgramaRequest existente() {
        final VincularDocenteProgramaRequest request = new VincularDocenteProgramaRequest();
        request.setIdDocente(UUID.randomUUID());
        request.setIdTipoVinculacion(UUID.randomUUID());
        return request;
    }

    private static VincularDocenteProgramaRequest nuevo() {
        final VincularDocenteProgramaRequest request = new VincularDocenteProgramaRequest();
        request.setIdTipoIdentificacion(UUID.randomUUID());
        request.setNumeroIdentificacion("1017555444");
        request.setPrimerNombre("Jorge");
        request.setPrimerApellido("Henao");
        request.setCorreo("jorge.henao@uco.edu.co");
        request.setPassword("Clave1234!");
        request.setIdTipoVinculacion(UUID.randomUUID());
        return request;
    }

    private static void assertIssue(final ValidationResult result, final String field, final ValidationErrorType type) {
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(issue -> field.equals(issue.field()) && type == issue.type()),
                () -> "No se encontró " + field + "/" + type + " en " + result.issues());
    }
}
