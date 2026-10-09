package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ArchivoControllerErrorHttpTest {

    private final SubirArchivoInputPort subirArchivoInputPort = mock(SubirArchivoInputPort.class);
    private final DescargarArchivoInputPort descargarArchivoInputPort = mock(DescargarArchivoInputPort.class);
    private final AuthenticatedUserResolver authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
    private final ResolverMensajeUsuarioInputPort resolverMensajeUsuario = mock(ResolverMensajeUsuarioInputPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(resolverMensajeUsuario.execute(any())).thenReturn(Optional.empty());
        final ArchivoController controller = new ArchivoController(
                subirArchivoInputPort, descargarArchivoInputPort, authenticatedUserResolver);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(resolverMensajeUsuario))
                .build();
    }

    @Test
    void archivo_infectado_retorna_400_controlado() throws Exception {
        when(subirArchivoInputPort.execute(any())).thenThrow(
                new ValidationException("El archivo fue rechazado por la politica de seguridad."));

        mockMvc.perform(multipart("/api/v1/archivos/subir").file(pdf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void scanner_no_disponible_retorna_5xx_tecnico_sin_detalles_internos() throws Exception {
        when(subirArchivoInputPort.execute(any())).thenThrow(new InternalApplicationException(
                "No fue posible completar el control de seguridad.",
                new IllegalStateException("clamav.internal:3310 protocolo invalido")));

        mockMvc.perform(multipart("/api/v1/archivos/subir").file(pdf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_APPLICATION_ERROR"))
                .andExpect(content().string(not(containsString("clamav.internal"))))
                .andExpect(content().string(not(containsString("3310"))))
                .andExpect(content().string(not(containsString("protocolo invalido"))));
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile(
                "archivo", "soporte.pdf", "application/pdf", "%PDF-contenido".getBytes());
    }
}


