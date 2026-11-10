package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CsvCargaUsuariosParserPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaResultado;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.EstadoCuentaAprovisionada;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.ResultadoCuentaIdentidad;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas.CsvCargaUsuariosParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EjecutarCargaUsuariosUseCaseImplTest {

    private final CargaUsuariosValidacionPort validacionPort = mock(CargaUsuariosValidacionPort.class);
    private final CsvCargaUsuariosParserPort parserPort = new CsvCargaUsuariosParser();
    private final VincularDocenteProgramaUseCase vincularDocenteUseCase = mock(VincularDocenteProgramaUseCase.class);
    private final CrearCoordinadorUseCase crearCoordinadorUseCase = mock(CrearCoordinadorUseCase.class);
    private final CrearDecanoUseCase crearDecanoUseCase = mock(CrearDecanoUseCase.class);
    private final AsegurarCuentaIdentidadService asegurarCuentaService = mock(AsegurarCuentaIdentidadService.class);

    private final EjecutarCargaUsuariosUseCaseImpl useCase = new EjecutarCargaUsuariosUseCaseImpl(
            validacionPort, parserPort, vincularDocenteUseCase, crearCoordinadorUseCase, crearDecanoUseCase, asegurarCuentaService
    );

    private static String calcularSha256(final byte[] bytes) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("D5-E01: Huella distinta lanza 409 ConflictException CMA_003")
    void huellaDistintaLanzaConflicto() {
        final byte[] bytes = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\nCC;123;A;B;a@uco.edu.co;C1!;TC;ING\r\n".getBytes(StandardCharsets.UTF_8);
        final EjecutarCargaUsuariosDomain domain = new EjecutarCargaUsuariosDomain(
                UUID.randomUUID(), PerfilDestinoCarga.DOCENTE, bytes, "huella-invalida"
        );

        final ConflictException ex = assertThrows(ConflictException.class, () -> useCase.execute(domain));
        assertEquals(CargaMasivaErrorCode.CMA_003.code(), ex.getCode());
    }

    @Test
    @DisplayName("D5-E02: Excepcion en fila 2 no detiene fila 3 (ejecucion secuencial aislada)")
    void errorEnFilaNoDetieneSiguientesFilas() {
        final UUID ejecutor = UUID.randomUUID();
        final UUID tipoDocId = UUID.randomUUID();
        final UUID tipoVincId = UUID.randomUUID();
        final UUID progId = UUID.randomUUID();

        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017000001;Docente;Uno;doc1@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017000002;Docente;Dos;doc2@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017000003;Docente;Tres;doc3@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        final String huella = calcularSha256(bytes);

        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DOCENTE), any()))
                .thenReturn(List.of(
                        new FilaValidacionDbProjection(2, true, false, null, null, tipoDocId, tipoVincId, progId, null, null),
                        new FilaValidacionDbProjection(3, true, false, null, null, tipoDocId, tipoVincId, progId, null, null),
                        new FilaValidacionDbProjection(4, true, false, null, null, tipoDocId, tipoVincId, progId, null, null)
                ));

        // Fila 1 OK
        final DocenteProgramaDomain mockDomain = mock(DocenteProgramaDomain.class);
        when(vincularDocenteUseCase.execute(any())).thenReturn(mockDomain)
                .thenThrow(new ValidationException("Error en doc2"))
                .thenReturn(mockDomain);

        final EjecutarCargaUsuariosDomain domain = new EjecutarCargaUsuariosDomain(
                ejecutor, PerfilDestinoCarga.DOCENTE, bytes, huella
        );

        final ResultadoCargaDomain resultado = useCase.execute(domain);

        assertEquals(3, resultado.totalFilas());
        assertEquals(2, resultado.creadas());
        assertEquals(1, resultado.conError());
        assertEquals(EstadoFilaResultado.CREADA, resultado.filas().get(0).estado());
        assertEquals(EstadoFilaResultado.ERROR, resultado.filas().get(1).estado());
        assertEquals(EstadoFilaResultado.CREADA, resultado.filas().get(2).estado());

        // CERT-CULM001 (HU171): una persona nueva se vincula en modo "docente nuevo" (idDocente e idUsuario nulos).
        // Con ids aleatorios VincularDocenteProgramaUseCaseImpl usaba el modo "existente" y el SP respondia DOC_005.
        final org.mockito.ArgumentCaptor<co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain> captor =
                org.mockito.ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain.class);
        org.mockito.Mockito.verify(vincularDocenteUseCase, org.mockito.Mockito.times(3)).execute(captor.capture());
        for (final var vinculo : captor.getAllValues()) {
            org.junit.jupiter.api.Assertions.assertNull(vinculo.idDocente());
            org.junit.jupiter.api.Assertions.assertNull(vinculo.idUsuario());
            assertEquals(tipoVincId, vinculo.idTipoVinculacion());
        }
    }

    @Test
    @DisplayName("D5-E03: Fila YA_EXISTE repara cuenta si faltaba o mantiene YA_EXISTE")
    void filaYaExisteReparaCuentaOMantieneYaExiste() {
        final UUID ejecutor = UUID.randomUUID();
        final UUID usuarioExistente1 = UUID.randomUUID();
        final UUID usuarioExistente2 = UUID.randomUUID();

        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017000001;Docente;Uno;doc1@uco.edu.co;;TC;ING-SIS\r\n"
                + "CC;1017000002;Docente;Dos;doc2@uco.edu.co;;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        final String huella = calcularSha256(bytes);

        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DOCENTE), any()))
                .thenReturn(List.of(
                        new FilaValidacionDbProjection(2, false, true, null, null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, usuarioExistente1),
                        new FilaValidacionDbProjection(3, false, true, null, null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, usuarioExistente2)
                ));

        // Para fila 1: repararOFallar tiene exito (creo la cuenta que faltaba)
        when(asegurarCuentaService.repararOFallar(any())).thenReturn(
                new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, true)
        );
        // Para fila 2: repararOFallar lanza ConflictException (la cuenta ya existia en IdP)
        when(asegurarCuentaService.repararOFallar(argThat(s -> s.idUsuario().equals(usuarioExistente2))))
                .thenThrow(new ConflictException("ADS_002"));

        final EjecutarCargaUsuariosDomain domain = new EjecutarCargaUsuariosDomain(
                ejecutor, PerfilDestinoCarga.DOCENTE, bytes, huella
        );

        final ResultadoCargaDomain resultado = useCase.execute(domain);

        assertEquals(2, resultado.totalFilas());
        assertEquals(2, resultado.yaExistentes());
        assertEquals(EstadoFilaResultado.YA_EXISTE_CUENTA_REPARADA, resultado.filas().get(0).estado());
        assertEquals(EstadoFilaResultado.YA_EXISTE, resultado.filas().get(1).estado());
    }

    private static co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad argThat(
            java.util.function.Predicate<co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad> predicate
    ) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
