package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaResultado;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas.CsvCargaUsuariosParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.5: ejecucion de la carga para coordinadores y decanos, filas con error y reparacion de cuentas existentes. */
class EjecutarCargaUsuariosPerfilesUseCaseImplTest {

    private static final String CABECERA_COORDINADOR = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_programa\r\n";
    private static final String CABECERA_DECANO = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_facultad\r\n";

    private final CargaUsuariosValidacionPort validacionPort = mock(CargaUsuariosValidacionPort.class);
    private final VincularDocenteProgramaUseCase vincularDocente = mock(VincularDocenteProgramaUseCase.class);
    private final CrearCoordinadorUseCase crearCoordinador = mock(CrearCoordinadorUseCase.class);
    private final CrearDecanoUseCase crearDecano = mock(CrearDecanoUseCase.class);
    private final AsegurarCuentaIdentidadService asegurarCuenta = mock(AsegurarCuentaIdentidadService.class);
    private final EjecutarCargaUsuariosUseCaseImpl useCase = new EjecutarCargaUsuariosUseCaseImpl(
            validacionPort, new CsvCargaUsuariosParser(), vincularDocente, crearCoordinador, crearDecano, asegurarCuenta);

    private static String sha256(final byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static FilaValidacionDbProjection valida(final int fila, final UUID tipo, final UUID programa, final UUID facultad, final UUID existente) {
        return new FilaValidacionDbProjection(fila, true, false, null, null, tipo, UUID.randomUUID(), programa, facultad, existente);
    }

    @Test
    void crea_coordinadores_con_el_programa_resuelto_por_la_db() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final UUID tipo = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final byte[] bytes = (CABECERA_COORDINADOR + "CC;1017000002;Coord;Lopez;coord@uco.edu.co;ClaveTemporal123!;ING-SIS\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.COORDINADOR), any()))
                .thenReturn(List.of(valida(2, tipo, programa, null, usuario)));

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.COORDINADOR, bytes, sha256(bytes)));

        assertEquals(1, resultado.creadas());
        assertEquals(0, resultado.conError());
        assertEquals("Carga procesada: 1 creadas, 0 ya existentes, 0 con error.", resultado.mensaje());
        final FilaResultadoCargaDomain fila = resultado.filas().getFirst();
        assertEquals(EstadoFilaResultado.CREADA, fila.estado());
        assertEquals(usuario, fila.idUsuarioCreado());
        verify(crearCoordinador).execute(any());
        verify(vincularDocente, never()).execute(any());
    }

    @Test
    void crea_decanos_con_la_facultad_resuelta_por_la_db() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final byte[] bytes = (CABECERA_DECANO + "CC;1017000003;Decano;Ramirez;decano@uco.edu.co;ClaveTemporal123!;FAC-ING\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DECANO), any()))
                .thenReturn(List.of(valida(2, UUID.randomUUID(), null, facultad, null)));

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.DECANO, bytes, sha256(bytes)));

        assertEquals(1, resultado.creadas());
        assertEquals(EstadoFilaResultado.CREADA, resultado.filas().getFirst().estado());
        assertNull(resultado.filas().getFirst().idUsuarioCreado());
        verify(crearDecano).execute(any());
    }

    @Test
    void un_fallo_al_crear_el_decano_deja_la_fila_con_error_y_su_codigo() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final byte[] bytes = (CABECERA_DECANO + "CC;1017000003;Decano;Ramirez;decano@uco.edu.co;ClaveTemporal123!;FAC-ING\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DECANO), any()))
                .thenReturn(List.of(valida(2, UUID.randomUUID(), null, UUID.randomUUID(), null)));
        doThrow(new ValidationException("Facultad sin cupo")).when(crearDecano).execute(any());
        final byte[] sinMensaje = bytes;

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.DECANO, sinMensaje, sha256(bytes)));

        assertEquals(0, resultado.creadas());
        assertEquals(1, resultado.conError());
        final FilaResultadoCargaDomain fila = resultado.filas().getFirst();
        assertEquals(EstadoFilaResultado.ERROR, fila.estado());
        assertEquals("Facultad sin cupo", fila.errores().getFirst().mensaje());
    }

    @Test
    void un_error_inesperado_sin_mensaje_usa_cma_006_y_el_mensaje_generico() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final byte[] bytes = (CABECERA_COORDINADOR + "CC;1017000002;Coord;Lopez;coord@uco.edu.co;ClaveTemporal123!;ING-SIS\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.COORDINADOR), any()))
                .thenReturn(List.of(valida(2, UUID.randomUUID(), UUID.randomUUID(), null, null)));
        doThrow(new IllegalStateException()).when(crearCoordinador).execute(any());

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.COORDINADOR, bytes, sha256(bytes)));

        assertEquals(1, resultado.conError());
        assertEquals("CMA_006", resultado.filas().getFirst().errores().getFirst().codigo());
        assertEquals("Error procesando la fila.", resultado.filas().getFirst().errores().getFirst().mensaje());
    }

    @Test
    void una_persona_nueva_sin_clave_inicial_es_error_y_no_se_crea() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final byte[] bytes = (CABECERA_COORDINADOR + "CC;1017000002;Coord;Lopez;coord@uco.edu.co;;ING-SIS\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.COORDINADOR), any()))
                .thenReturn(List.of(valida(2, UUID.randomUUID(), UUID.randomUUID(), null, null)));

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.COORDINADOR, bytes, sha256(bytes)));

        assertEquals(1, resultado.conError());
        assertEquals("clave_inicial", resultado.filas().getFirst().errores().getFirst().campo());
        verify(crearCoordinador, never()).execute(any());
    }

    @Test
    void una_fila_rechazada_por_la_db_acumula_el_codigo_y_el_mensaje_del_sp() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final byte[] bytes = (CABECERA_DECANO + "CC;1017000003;Decano;Ramirez;decano@uco.edu.co;ClaveTemporal123!;FAC-XX\r\n"
                + "CC;1017000004;Decano;Otro;otro@uco.edu.co;ClaveTemporal123!;FAC-YY\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DECANO), any())).thenReturn(List.of(
                new FilaValidacionDbProjection(2, false, false, "CMA_020", "La facultad no existe.", null, null, null, null, null),
                new FilaValidacionDbProjection(3, false, false, "CMA_021", null, null, null, null, null, null)));

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.DECANO, bytes, sha256(bytes)));

        assertEquals(2, resultado.conError());
        assertEquals("La facultad no existe.", resultado.filas().get(0).errores().getFirst().mensaje());
        assertEquals("CMA_021", resultado.filas().get(1).errores().getFirst().mensaje());
        verify(crearDecano, never()).execute(any());
    }

    @Test
    void una_fila_sin_proyeccion_de_la_db_no_es_valida() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final byte[] bytes = (CABECERA_COORDINADOR + "CC;1017000002;Coord;Lopez;coord@uco.edu.co;ClaveTemporal123!;ING-SIS\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.COORDINADOR), any())).thenReturn(null);

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.COORDINADOR, bytes, sha256(bytes)));

        assertEquals(1, resultado.conError());
        assertEquals(EstadoFilaResultado.ERROR, resultado.filas().getFirst().estado());
    }

    @Test
    void la_reparacion_de_una_cuenta_existente_pide_el_rol_del_perfil_y_tolera_cualquier_fallo() throws Exception {
        final UUID ejecutor = UUID.randomUUID();
        final UUID existente = UUID.randomUUID();
        final byte[] bytes = (CABECERA_DECANO + "CC;1017000003;Decano;Ramirez;decano@uco.edu.co;ClaveTemporal123!;FAC-ING\r\n").getBytes(StandardCharsets.UTF_8);
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DECANO), any()))
                .thenReturn(List.of(new FilaValidacionDbProjection(2, false, true, null, null, null, null, null, null, existente)));
        when(asegurarCuenta.repararOFallar(any())).thenThrow(new IllegalStateException("idp caido"));

        final ResultadoCargaDomain resultado = useCase.execute(new EjecutarCargaUsuariosDomain(ejecutor, PerfilDestinoCarga.DECANO, bytes, sha256(bytes)));

        assertEquals(1, resultado.yaExistentes());
        assertEquals(EstadoFilaResultado.YA_EXISTE, resultado.filas().getFirst().estado());
        assertEquals(existente, resultado.filas().getFirst().idUsuarioCreado());
        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(asegurarCuenta).repararOFallar(solicitud.capture());
        assertEquals(existente, solicitud.getValue().idUsuario());
        assertNotNull(solicitud.getValue().rol());
        assertTrue(solicitud.getValue().rol().name().contains("DECANO"));
    }

    @Test
    void exige_el_dominio_y_las_dependencias() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        final CsvCargaUsuariosParser parser = new CsvCargaUsuariosParser();
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(null, parser, vincularDocente, crearCoordinador, crearDecano, asegurarCuenta));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(validacionPort, null, vincularDocente, crearCoordinador, crearDecano, asegurarCuenta));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(validacionPort, parser, null, crearCoordinador, crearDecano, asegurarCuenta));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(validacionPort, parser, vincularDocente, null, crearDecano, asegurarCuenta));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(validacionPort, parser, vincularDocente, crearCoordinador, null, asegurarCuenta));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosUseCaseImpl(validacionPort, parser, vincularDocente, crearCoordinador, crearDecano, null));
    }
}
