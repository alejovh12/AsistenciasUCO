package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EstadoFilaValidacion;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidarCargaUsuariosUseCaseImplTest {

    private final CargaUsuariosValidacionPort validacionPort = mock(CargaUsuariosValidacionPort.class);
    private final ValidarCargaUsuariosUseCaseImpl useCase = new ValidarCargaUsuariosUseCaseImpl(validacionPort);

    @Test
    @DisplayName("D5-V01: Validacion sin comandos de mutacion y mapeo de estados de fila")
    void validaCorrectamenteSinMutaciones() {
        final UUID ejecutor = UUID.randomUUID();
        final UUID tipoDocId = UUID.randomUUID();
        final UUID tipoVincId = UUID.randomUUID();
        final UUID progId = UUID.randomUUID();

        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017000001;Docente;Uno;doc1@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017000002;Docente;Dos;doc2@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017000003;Docente;Tres;doc3@uco.edu.co;ClaveValida123!;TC;OTRO\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        // Simulamos salida de DB:
        // Fila 2: Valida
        // Fila 3: Ya existe
        // Fila 4: Error de ambito (CMA_007)
        when(validacionPort.validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DOCENTE), any()))
                .thenReturn(List.of(
                        new FilaValidacionDbProjection(2, true, false, null, null, tipoDocId, tipoVincId, progId, null, null),
                        new FilaValidacionDbProjection(3, false, true, null, null, tipoDocId, tipoVincId, progId, null, UUID.randomUUID()),
                        new FilaValidacionDbProjection(4, false, false, "CMA_007", "OTRO no esta en tu ambito", null, null, null, null, null)
                ));

        final ValidarCargaUsuariosCommand command = new ValidarCargaUsuariosCommand(
                ejecutor, PerfilDestinoCarga.DOCENTE, "docentes.csv", bytes
        );

        final ResultadoValidacionCargaDTO resultado = useCase.execute(command);

        assertNotNull(resultado.huella());
        assertEquals(PerfilDestinoCarga.DOCENTE, resultado.perfilDestino());
        assertEquals(3, resultado.totalFilas());
        assertEquals(1, resultado.filasValidas());
        assertEquals(1, resultado.filasYaExistentes());
        assertEquals(1, resultado.filasConError());

        assertEquals(EstadoFilaValidacion.VALIDA, resultado.filas().get(0).estado());
        assertEquals(EstadoFilaValidacion.YA_EXISTE, resultado.filas().get(1).estado());
        assertEquals(EstadoFilaValidacion.ERROR, resultado.filas().get(2).estado());
        assertEquals("CMA_007", resultado.filas().get(2).errores().get(0).codigo());

        verify(validacionPort).validarEnBaseDatos(eq(ejecutor), eq(PerfilDestinoCarga.DOCENTE), any());
    }

    @Test
    @DisplayName("D5-V02: Deteccion de duplicados en el archivo marca CMA_010 en la segunda aparicion")
    void detectaDuplicadosEnArchivoConCma010() {
        final UUID ejecutor = UUID.randomUUID();
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017111222;Docente;Uno;docente@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017111222;Docente;Dos;otro@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        when(validacionPort.validarEnBaseDatos(any(), any(), any())).thenReturn(List.of(
                new FilaValidacionDbProjection(2, true, false, null, null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null)
        ));

        final ValidarCargaUsuariosCommand command = new ValidarCargaUsuariosCommand(
                ejecutor, PerfilDestinoCarga.DOCENTE, "duplicados.csv", bytes
        );

        final ResultadoValidacionCargaDTO resultado = useCase.execute(command);

        assertEquals(2, resultado.totalFilas());
        assertEquals(EstadoFilaValidacion.VALIDA, resultado.filas().get(0).estado());
        assertEquals(EstadoFilaValidacion.ERROR, resultado.filas().get(1).estado());
        assertTrue(resultado.filas().get(1).errores().stream().anyMatch(e -> "CMA_010".equals(e.codigo())));
    }
}
