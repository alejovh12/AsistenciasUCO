package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor.CrearDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminPortalCrearDecanoContractTest {

    private static final UUID TIPO_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID FACULTAD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final JsonMapper json = JsonMapper.builder().build();
    private final CrearDecanoUseCase useCase = mock(CrearDecanoUseCase.class);
    private final AdminPortalController controller = new AdminPortalController(
            mock(ConsultarDecanosInputPort.class), new CrearDecanoInteractor(useCase),
            mock(ConsultarParametrosInputPort.class), mock(EjecutarCierreMasivoInputPort.class),
            mock(ConsultarInstitucionesInputPort.class), mock(ConsultarFacultadesInputPort.class),
            mock(ConsultarAreasInputPort.class), mock(AuthenticatedUserProvider.class)
    );

    @Test
    void tipo_identificacion_del_json_llega_al_domain() {
        controller.crearDecano(json.readValue(requestJson(TIPO_ID.toString()), AdminPortalController.CrearDecanoRequest.class));

        final ArgumentCaptor<CrearDecanoDomain> domain = ArgumentCaptor.forClass(CrearDecanoDomain.class);
        verify(useCase).execute(domain.capture());
        assertEquals(TIPO_ID, domain.getValue().getTipoIdentificacionId());
        assertEquals(123456789, domain.getValue().getNumeroIdentificacion());
    }

    @Test
    void request_sin_tipo_identificacion_no_continua() {
        final var request = json.readValue(requestJson(null), AdminPortalController.CrearDecanoRequest.class);

        assertThrows(ValidationException.class, () -> controller.crearDecano(request));
        verifyNoInteractions(useCase);
    }

    private String requestJson(final String tipoId) {
        final String tipo = tipoId == null ? "" : "\"tipoIdentificacionId\":\"" + tipoId + "\",";
        return "{" + tipo + "\"numeroIdentificacion\":123456789,\"primerNombre\":\"Ana\","
                + "\"segundoNombre\":\"Maria\",\"primerApellido\":\"Perez\","
                + "\"segundoApellido\":\"Gomez\",\"correo\":\"nuevo@uco.edu.co\","
                + "\"password\":\"Clave123!\",\"idFacultad\":\"" + FACULTAD_ID + "\"}";
    }
}
