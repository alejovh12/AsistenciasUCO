package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.exception.business.TooManyRequestsException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Los tres controladores de carga masiva (administrador -> decanos, decano -> coordinadores, coordinador -> docentes) comparten
 * contrato: perfil destino fijo, nombre de archivo por defecto, archivo vacio o ilegible -> CMA_013 y limite por minuto -> CMA_014.
 */
class CargasControllersArchivoTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final byte[] CSV = "a;b\r\n1;2\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private final ValidarCargaUsuariosInputPort validar = mock(ValidarCargaUsuariosInputPort.class);
    private final EjecutarCargaUsuariosInputPort ejecutar = mock(EjecutarCargaUsuariosInputPort.class);
    private final AuthenticatedUserResolver identidad = () -> ACTOR;

    private SlidingWindowRateLimiter limiter(final int maximo) {
        return new SlidingWindowRateLimiter(Duration.ofMinutes(1), maximo, Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), ZoneId.of("UTC")));
    }

    /** Un caso por controlador: perfil esperado, nombre por defecto y las dos operaciones. */
    private record Caso(String nombre, PerfilDestinoCarga perfil, String archivoPorDefecto,
                        BiFunction<SlidingWindowRateLimiter, MultipartFile, Object> validarOp,
                        BiFunction<SlidingWindowRateLimiter, MultipartFile, Object> ejecutarOp) {
    }

    private List<Caso> casos() {
        return List.of(
                new Caso("admin", PerfilDestinoCarga.DECANO, "decanos.csv",
                        (l, f) -> new AdminCargasController(validar, ejecutar, identidad, l).validar(f),
                        (l, f) -> new AdminCargasController(validar, ejecutar, identidad, l).ejecutar(f, "huella")),
                new Caso("decano", PerfilDestinoCarga.COORDINADOR, "coordinadores.csv",
                        (l, f) -> new DecanoCargasController(validar, ejecutar, identidad, l).validar(f),
                        (l, f) -> new DecanoCargasController(validar, ejecutar, identidad, l).ejecutar(f, "huella")),
                new Caso("coordinador", PerfilDestinoCarga.DOCENTE, "docentes.csv",
                        (l, f) -> new CoordinadorCargasController(validar, ejecutar, identidad, l).validar(f),
                        (l, f) -> new CoordinadorCargasController(validar, ejecutar, identidad, l).ejecutar(f, "huella")));
    }

    private static MultipartFile archivoSinNombre() throws IOException {
        final MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getBytes()).thenReturn(CSV);
        when(archivo.getOriginalFilename()).thenReturn(null);
        return archivo;
    }

    @Test
    void cada_controlador_envia_su_perfil_destino_y_el_nombre_del_archivo_subido() {
        for (final Caso caso : casos()) {
            final MockMultipartFile archivo = new MockMultipartFile("archivo", "subido.csv", "text/csv", CSV);
            when(validar.validar(any())).thenReturn(mock(ResultadoValidacionCargaDTO.class));
            when(ejecutar.ejecutar(any())).thenReturn(mock(ResultadoCargaDTO.class));

            assertNotNull(caso.validarOp().apply(limiter(10), archivo), caso.nombre());
            assertNotNull(caso.ejecutarOp().apply(limiter(10), archivo), caso.nombre());

            final ArgumentCaptor<ValidarCargaUsuariosCommand> v = ArgumentCaptor.forClass(ValidarCargaUsuariosCommand.class);
            verify(validar, org.mockito.Mockito.atLeastOnce()).validar(v.capture());
            final ValidarCargaUsuariosCommand ultimaValidacion = v.getValue();
            assertEquals(caso.perfil(), ultimaValidacion.perfilDestino(), caso.nombre());
            assertEquals("subido.csv", ultimaValidacion.nombreArchivo(), caso.nombre());
            assertEquals(ACTOR, ultimaValidacion.usuarioEjecutor());
            assertArrayEquals(CSV, ultimaValidacion.contenidoBytes());

            final ArgumentCaptor<EjecutarCargaUsuariosCommand> e = ArgumentCaptor.forClass(EjecutarCargaUsuariosCommand.class);
            verify(ejecutar, org.mockito.Mockito.atLeastOnce()).ejecutar(e.capture());
            assertEquals(caso.perfil(), e.getValue().perfilDestino(), caso.nombre());
            assertEquals("huella", e.getValue().huella());
            assertEquals("subido.csv", e.getValue().nombreArchivo());
        }
    }

    @Test
    void sin_nombre_original_se_usa_el_nombre_por_defecto_del_perfil() throws Exception {
        for (final Caso caso : casos()) {
            org.mockito.Mockito.reset(validar, ejecutar);
            when(validar.validar(any())).thenReturn(mock(ResultadoValidacionCargaDTO.class));
            when(ejecutar.ejecutar(any())).thenReturn(mock(ResultadoCargaDTO.class));

            caso.validarOp().apply(limiter(10), archivoSinNombre());
            caso.ejecutarOp().apply(limiter(10), archivoSinNombre());

            final ArgumentCaptor<ValidarCargaUsuariosCommand> v = ArgumentCaptor.forClass(ValidarCargaUsuariosCommand.class);
            verify(validar).validar(v.capture());
            assertEquals(caso.archivoPorDefecto(), v.getValue().nombreArchivo(), caso.nombre());
            final ArgumentCaptor<EjecutarCargaUsuariosCommand> e = ArgumentCaptor.forClass(EjecutarCargaUsuariosCommand.class);
            verify(ejecutar).ejecutar(e.capture());
            assertEquals(caso.archivoPorDefecto(), e.getValue().nombreArchivo(), caso.nombre());
        }
    }

    @Test
    void un_archivo_nulo_o_vacio_es_cma_013_y_no_llega_al_caso_de_uso() {
        for (final Caso caso : casos()) {
            final MockMultipartFile vacio = new MockMultipartFile("archivo", "vacio.csv", "text/csv", new byte[0]);
            for (final MultipartFile archivo : new MultipartFile[]{null, vacio}) {
                assertEquals("CMA_013", assertThrows(ValidationException.class, () -> caso.validarOp().apply(limiter(10), archivo), caso.nombre()).getCode());
                assertEquals("CMA_013", assertThrows(ValidationException.class, () -> caso.ejecutarOp().apply(limiter(10), archivo), caso.nombre()).getCode());
            }
        }
        verifyNoInteractions(validar, ejecutar);
    }

    @Test
    void un_archivo_ilegible_es_cma_013() throws Exception {
        for (final Caso caso : casos()) {
            final MultipartFile roto = mock(MultipartFile.class);
            when(roto.isEmpty()).thenReturn(false);
            when(roto.getBytes()).thenThrow(new IOException("disco"));
            assertEquals("CMA_013", assertThrows(ValidationException.class, () -> caso.validarOp().apply(limiter(10), roto), caso.nombre()).getCode());
            assertEquals("CMA_013", assertThrows(ValidationException.class, () -> caso.ejecutarOp().apply(limiter(10), roto), caso.nombre()).getCode());
        }
        verifyNoInteractions(validar, ejecutar);
    }

    @Test
    void superar_tres_cargas_por_minuto_es_cma_014_en_cada_controlador() {
        for (final Caso caso : casos()) {
            final SlidingWindowRateLimiter limite = limiter(3);
            final MockMultipartFile archivo = new MockMultipartFile("archivo", "x.csv", "text/csv", CSV);
            when(validar.validar(any())).thenReturn(mock(ResultadoValidacionCargaDTO.class));
            for (int i = 0; i < 3; i++) {
                caso.validarOp().apply(limite, archivo);
            }
            assertTrue(assertThrows(TooManyRequestsException.class, () -> caso.validarOp().apply(limite, archivo), caso.nombre()).getMessage() != null);
            assertThrows(TooManyRequestsException.class, () -> caso.ejecutarOp().apply(limite, archivo), caso.nombre());
        }
    }

    @Test
    void los_constructores_exigen_todas_sus_dependencias() {
        final SlidingWindowRateLimiter l = limiter(1);
        assertThrows(NullPointerException.class, () -> new AdminCargasController(null, ejecutar, identidad, l));
        assertThrows(NullPointerException.class, () -> new AdminCargasController(validar, null, identidad, l));
        assertThrows(NullPointerException.class, () -> new AdminCargasController(validar, ejecutar, null, l));
        assertThrows(NullPointerException.class, () -> new AdminCargasController(validar, ejecutar, identidad, null));
        assertThrows(NullPointerException.class, () -> new DecanoCargasController(null, ejecutar, identidad, l));
        assertThrows(NullPointerException.class, () -> new DecanoCargasController(validar, null, identidad, l));
        assertThrows(NullPointerException.class, () -> new DecanoCargasController(validar, ejecutar, null, l));
        assertThrows(NullPointerException.class, () -> new DecanoCargasController(validar, ejecutar, identidad, null));
        assertThrows(NullPointerException.class, () -> new CoordinadorCargasController(null, ejecutar, identidad, l));
        assertThrows(NullPointerException.class, () -> new CoordinadorCargasController(validar, null, identidad, l));
        assertThrows(NullPointerException.class, () -> new CoordinadorCargasController(validar, ejecutar, null, l));
        assertThrows(NullPointerException.class, () -> new CoordinadorCargasController(validar, ejecutar, identidad, null));
    }
}
