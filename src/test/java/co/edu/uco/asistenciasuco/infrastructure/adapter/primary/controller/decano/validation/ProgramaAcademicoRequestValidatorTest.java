package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CambiarEstadoProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.ProgramaAcademicoRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgramaAcademicoRequestValidatorTest {

    private static ProgramaAcademicoRequest valida() {
        final ProgramaAcademicoRequest request = new ProgramaAcademicoRequest();
        request.setNombre("Sistemas");
        request.setIdTipoPrograma(UUID.randomUUID());
        request.setIdCoordinador(UUID.randomUUID());
        return request;
    }

    @Test
    void acepta_una_peticion_valida_con_modalidad_y_jornada_opcionales() {
        assertDoesNotThrow(() -> ProgramaAcademicoRequestValidator.validar(valida()));
    }

    @Test
    void acepta_un_nombre_de_exactamente_50_caracteres() {
        final ProgramaAcademicoRequest request = valida();
        request.setNombre("X".repeat(50));

        assertDoesNotThrow(() -> ProgramaAcademicoRequestValidator.validar(request));
    }

    @Test
    void rechaza_cuerpo_nulo() {
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar((ProgramaAcademicoRequest) null));
    }

    @Test
    void rechaza_nombre_nulo_vacio_o_demasiado_largo() {
        final ProgramaAcademicoRequest nulo = valida();
        nulo.setNombre(null);
        final ProgramaAcademicoRequest vacio = valida();
        vacio.setNombre("   ");
        final ProgramaAcademicoRequest largo = valida();
        largo.setNombre("X".repeat(51));

        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(nulo));
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(vacio));
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(largo));
    }

    @Test
    void rechaza_tipo_o_coordinador_ausentes() {
        final ProgramaAcademicoRequest sinTipo = valida();
        sinTipo.setIdTipoPrograma(null);
        final ProgramaAcademicoRequest sinCoordinador = valida();
        sinCoordinador.setIdCoordinador(null);

        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(sinTipo));
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(sinCoordinador));
    }

    @Test
    void valida_el_estado_destino() {
        final CambiarEstadoProgramaRequest conEstado = new CambiarEstadoProgramaRequest();
        conEstado.setEstaActivo(false);

        assertDoesNotThrow(() -> ProgramaAcademicoRequestValidator.validar(conEstado));
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar((CambiarEstadoProgramaRequest) null));
        assertThrows(ValidationException.class, () -> ProgramaAcademicoRequestValidator.validar(new CambiarEstadoProgramaRequest()));
    }
}
