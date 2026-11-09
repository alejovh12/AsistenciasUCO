package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaValidacion;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ValidarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas.CsvCargaUsuariosParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Casos borde de la validacion previa de la carga masiva: sin respuesta de la DB, mensajes sin texto, clave faltante y huella estable. */
class ValidarCargaUsuariosCasosBordeUseCaseImplTest {

    private static final String CABECERA = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_facultad\r\n";

    private final CargaUsuariosValidacionPort validacionPort = mock(CargaUsuariosValidacionPort.class);
    private final ValidarCargaUsuariosUseCaseImpl useCase = new ValidarCargaUsuariosUseCaseImpl(validacionPort, new CsvCargaUsuariosParser());

    private ResultadoValidacionCargaDomain validar(final String filas) {
        return useCase.execute(new ValidarCargaUsuariosDomain(UUID.randomUUID(), PerfilDestinoCarga.DECANO, (CABECERA + filas).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void sin_respuesta_de_la_db_la_fila_no_se_considera_valida() {
        when(validacionPort.validarEnBaseDatos(any(), any(), any())).thenReturn(null);

        final ResultadoValidacionCargaDomain resultado = validar("CC;1017123456;Ana;Perez;ana@uco.edu.co;ClaveValida123!;FAC-ING\r\n");

        assertEquals(1, resultado.conError());
        assertEquals(0, resultado.validas());
        assertEquals(EstadoFilaValidacion.ERROR, resultado.filas().getFirst().estado());
        assertTrue(resultado.filas().getFirst().errores().isEmpty());
    }

    @Test
    void un_codigo_de_mensaje_sin_texto_usa_el_codigo_como_mensaje() {
        when(validacionPort.validarEnBaseDatos(any(), any(), any())).thenReturn(List.of(
                new FilaValidacionDbProjection(2, false, false, "CMA_007", null, null, null, null, null, null),
                new FilaValidacionDbProjection(3, false, false, "  ", "texto ignorado", null, null, null, null, null)));

        final ResultadoValidacionCargaDomain resultado = validar(
                "CC;1017123456;Ana;Perez;ana@uco.edu.co;ClaveValida123!;FAC-ING\r\nCC;1017123457;Luis;Ruiz;luis@uco.edu.co;ClaveValida123!;FAC-ING\r\n");

        assertEquals("CMA_007", resultado.filas().get(0).errores().getFirst().mensaje());
        assertTrue(resultado.filas().get(1).errores().isEmpty());
        assertEquals(2, resultado.conError());
    }

    @Test
    void una_persona_nueva_sin_clave_inicial_es_error_cma_012_pero_una_existente_no() {
        when(validacionPort.validarEnBaseDatos(any(), any(), any())).thenReturn(List.of(
                new FilaValidacionDbProjection(2, true, false, null, null, UUID.randomUUID(), null, null, UUID.randomUUID(), null),
                new FilaValidacionDbProjection(3, false, true, null, null, UUID.randomUUID(), null, null, UUID.randomUUID(), UUID.randomUUID())));

        final ResultadoValidacionCargaDomain resultado = validar(
                "CC;1017123456;Ana;Perez;ana@uco.edu.co;;FAC-ING\r\nCC;1017123457;Luis;Ruiz;luis@uco.edu.co;;FAC-ING\r\n");

        assertTrue(resultado.filas().get(0).errores().stream().anyMatch(e -> "CMA_012".equals(e.codigo())));
        assertEquals(EstadoFilaValidacion.ERROR, resultado.filas().get(0).estado());
        assertEquals(EstadoFilaValidacion.YA_EXISTE, resultado.filas().get(1).estado());
        assertEquals(1, resultado.yaExistentes());
    }

    @Test
    void la_huella_es_el_sha256_del_contenido_y_cambia_con_el_contenido() {
        when(validacionPort.validarEnBaseDatos(any(), any(), any())).thenReturn(List.of());
        final String filas = "CC;1017123456;Ana;Perez;ana@uco.edu.co;ClaveValida123!;FAC-ING\r\n";

        final String huella = validar(filas).huella();

        assertEquals(huella, validar(filas).huella());
        assertEquals(64, huella.length());
        assertNotEquals(huella, validar(filas.replace("Ana", "Ane")).huella());
        assertEquals(ValidarCargaUsuariosUseCaseImpl.calcularSha256((CABECERA + filas).getBytes(StandardCharsets.UTF_8)), huella);
    }

    @Test
    void exige_dominio_y_dependencias() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosUseCaseImpl(null, new CsvCargaUsuariosParser()));
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosUseCaseImpl(validacionPort, null));
    }
}
