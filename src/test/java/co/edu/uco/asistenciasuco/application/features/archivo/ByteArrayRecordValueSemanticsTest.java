package co.edu.uco.asistenciasuco.application.features.archivo;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * RED QUALITY-PR15, Sonar java:S6218 (issues AaEaHMBaE62pkux_HRL0, AaEaHMBIE62pkux_HRLy,
 * AaEaHMA-E62pkux_HRLx, AaEaHMAoE62pkux_HRLv, AaEaHL7XE62pkux_HRLu, AaEaHMCkE62pkux_HRL1,
 * AaEaHMCkE62pkux_HRL2).
 *
 * <p>Los records que transportan bytes de un soporte deben comparar y describir el contenido, no la
 * identidad del arreglo: dos resultados con los mismos bytes son el mismo valor. Ademas,
 * {@code toString} no debe volcar el payload del archivo (no se registra contenido en logs).</p>
 */
class ByteArrayRecordValueSemanticsTest {

    private static final UUID OWNER = UUID.fromString("7a1f0c1e-2b8a-4a55-9c55-6c1f3c0a9b01");
    private static final String PAYLOAD_MARKER = "PAYLOAD-SECRETO-DEL-SOPORTE";

    static Stream<Arguments> records() {
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                OWNER.toString(), "soporte.pdf", "application/pdf", 32L, 32L, false, null,
                "a".repeat(64), Instant.parse("2026-10-08T12:00:00Z"));
        return Stream.of(
                record("DescargarArchivoResultado",
                        b -> new DescargarArchivoResultado(b, "application/pdf", "soporte.pdf", b.length)),
                record("DescargarArchivoResultadoEntity",
                        b -> new DescargarArchivoResultadoEntity(b, "application/pdf", "soporte.pdf", b.length)),
                record("CompressionDecision",
                        b -> new CompressionDecision(true, "DEFLATE", b)),
                record("SubirArchivoDTO",
                        b -> new SubirArchivoDTO(OWNER, "soporte.pdf", "application/pdf", b)),
                record("SubirArchivoDomain",
                        b -> new SubirArchivoDomain(OWNER, "soporte.pdf", "application/pdf", b)),
                record("FileStoragePort.StoreObjectCommand",
                        b -> new FileStoragePort.StoreObjectCommand("f-1", b, metadata)),
                record("FileStoragePort.StoredObject",
                        b -> new FileStoragePort.StoredObject("f-1", b, metadata))
        );
    }

    @ParameterizedTest
    @MethodSource("records")
    void mismo_contenido_en_arreglos_distintos_es_el_mismo_valor(final Function<byte[], Object> factory) {
        final Object first = factory.apply(payload());
        final Object second = factory.apply(payload());

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.toString(), second.toString());
    }

    @ParameterizedTest
    @MethodSource("records")
    void contenido_distinto_no_es_el_mismo_valor(final Function<byte[], Object> factory) {
        final byte[] altered = payload();
        altered[altered.length - 1] = (byte) '#';

        assertNotEquals(factory.apply(payload()), factory.apply(altered));
    }

    @ParameterizedTest
    @MethodSource("records")
    void representacion_textual_no_vuelca_el_payload_del_archivo(final Function<byte[], Object> factory) {
        final String text = factory.apply(payload()).toString();

        assertFalse(text.contains(PAYLOAD_MARKER), "toString no debe exponer los bytes del soporte: " + text);
        assertFalse(text.contains("[B@"), "toString no debe depender de la identidad del arreglo: " + text);
    }

    private static Arguments record(final String name, final Function<byte[], Object> factory) {
        return Arguments.of(Named.of(name, factory));
    }

    private static byte[] payload() {
        return ("%PDF-1.4 " + PAYLOAD_MARKER).getBytes(StandardCharsets.US_ASCII);
    }
}
