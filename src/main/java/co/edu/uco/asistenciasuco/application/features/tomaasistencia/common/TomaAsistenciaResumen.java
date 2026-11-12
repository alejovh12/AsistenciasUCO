package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013): una toma de asistencia de una sesion tal como la expone {@code uv_toma_asistencia}. Los codigos
 * de modo y estado son los del catalogo ({@code ModoTomaAsistencia}, {@code EstadoTomaAsistencia}). PC (puntos de control): agrega el
 * punto de control de la toma, los minutos de sus tramos vecinos y si sigue editable (hasta cuando); {@code esEditable} y
 * {@code editableHastaUtc} solo son exactos en el listado por sesion ({@code usp_consultar_tomas_sesion}).
 */
public record TomaAsistenciaResumen(
        UUID idToma,
        UUID idSesion,
        UUID idGrupo,
        int numero,
        String modo,
        String estado,
        boolean estaAbierta,
        boolean esImplicita,
        Instant fechaAperturaUtc,
        Instant fechaCierreUtc,
        // PC: horasToma (1/n) esta OBSOLETO; se conserva por compatibilidad, las horas salen de los minutos por tramo.
        BigDecimal horasToma,
        int totalRegistros,
        int totalPresentes,
        int totalAusentes,
        int totalExcusas,
        boolean tieneQrActivo,
        Instant fechaPuntoControlUtc,
        int ordenPuntoControl,
        int minutosTramoAnterior,
        int minutosTramoSiguiente,
        boolean esEditable,
        Instant editableHastaUtc
) {
}
