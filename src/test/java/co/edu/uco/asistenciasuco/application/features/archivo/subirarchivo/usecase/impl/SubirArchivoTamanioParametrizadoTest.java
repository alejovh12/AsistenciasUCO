package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ContentSecurityValidator;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** PAR-05: el tamano maximo del soporte sale del parametro ARCHIVO/TAMANIO_MAXIMO_BYTES; 5 MiB es solo el respaldo. */
class SubirArchivoTamanioParametrizadoTest {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};

    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final MalwareScanPort antivirus = mock(MalwareScanPort.class);
    private final SoporteArchivoMetadataPort metadata = mock(SoporteArchivoMetadataPort.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final SubirArchivoUseCaseImpl useCase = new SubirArchivoUseCaseImpl(storage, antivirus, metadata, parametros);

    @Test
    void un_limite_parametrizado_menor_rechaza_antes_del_antivirus_y_lo_informa() {
        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenReturn(Optional.of(" 262144 "));

        final SubirArchivoDomain excedido = pdf(262_145);

        final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(excedido));

        assertTrue(error.getMessage().contains("256 KiB"), error.getMessage());
        verifyNoInteractions(antivirus);
        verify(storage, never()).store(any());
    }

    @Test
    void dentro_del_limite_parametrizado_continua_el_flujo() {
        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenReturn(Optional.of("262144"));
        when(antivirus.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());

        useCase.execute(pdf(262_144));

        verify(storage).store(any());
    }

    @Test
    void sin_parametro_o_con_valor_invalido_se_aplica_el_respaldo_de_5_mib() {
        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenReturn(Optional.empty());
        assertEquals(ContentSecurityValidator.MAX_FILE_SIZE_BYTES, useCase.tamanioMaximoBytes());

        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenReturn(Optional.of("cinco megas"));
        assertEquals(ContentSecurityValidator.MAX_FILE_SIZE_BYTES, useCase.tamanioMaximoBytes());

        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenThrow(new IllegalStateException("catalogo caido"));
        assertEquals(ContentSecurityValidator.MAX_FILE_SIZE_BYTES, useCase.tamanioMaximoBytes());

        assertEquals(ContentSecurityValidator.MAX_FILE_SIZE_BYTES,
                new SubirArchivoUseCaseImpl(storage, antivirus, metadata).tamanioMaximoBytes());
    }

    @Test
    void un_parametro_por_encima_del_techo_tecnico_no_amplia_el_limite() {
        when(parametros.getParameter("ARCHIVO", "TAMANIO_MAXIMO_BYTES")).thenReturn(Optional.of("10485760"));

        final SubirArchivoDomain sobreTecho = pdf((int) ContentSecurityValidator.MAX_FILE_SIZE_BYTES + 1);

        final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(sobreTecho));

        assertTrue(error.getMessage().contains("5 MiB"), error.getMessage());
    }

    private static SubirArchivoDomain pdf(final int tamanio) {
        final byte[] contenido = new byte[tamanio];
        Arrays.fill(contenido, (byte) 'a');
        System.arraycopy(PDF_MAGIC, 0, contenido, 0, PDF_MAGIC.length);
        return new SubirArchivoDomain(UUID.randomUUID(), "soporte.pdf", "application/pdf", contenido);
    }
}
