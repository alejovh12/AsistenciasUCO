package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.request;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CULM-001 B.2 (contrato 3.2.5): cuerpos HTTP de las tomas. */
public final class TomaAsistenciaRequests {

    private TomaAsistenciaRequests() {
    }

    /** {@code MANUAL} o {@code QR} (codigo del catalogo {@code ModoTomaAsistencia}). */
    public record AbrirToma(String modo) {
    }

    /** {@code null} aplica el valor del modo (MANUAL no marca, QR marca). */
    public record CerrarToma(Boolean marcarAusentes) {
    }

    public record RegistroToma(
            @NotNull(message = "Cada registro debe indicar el estudiante y el estado.") UUID estudianteId,
            @NotBlank(message = "Cada registro debe indicar el estudiante y el estado.") String estado
    ) {
    }

    public record LoteToma(
            @NotNull(message = "El lote debe tener entre 1 y 200 registros.") @Size(min = 1, max = 200, message = "El lote debe tener entre 1 y 200 registros.") @Valid List<RegistroToma> registros
    ) {
    }
}
