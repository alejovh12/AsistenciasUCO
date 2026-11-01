package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.ActualizarGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.CrearGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.FranjaHorarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpTemporalParser;

import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class GrupoHttpMapper {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private GrupoHttpMapper() {
    }

    public static CrearGrupoDTO toApplicationDTO(final CrearGrupoRequest request, final UUID usuarioEjecutor) {
        Objects.requireNonNull(request, "El request HTTP para crear grupo es obligatorio.");
        return new CrearGrupoDTO(
                firstNonNull(request.getIdAsignatura(), request.getAsignaturaId()),
                firstNonNull(request.getIdPeriodoAcademico(), request.getPeriodoAcademicoId()),
                required(request.getCodigo(), "codigo"),
                requiredText(request.getNombre(), "nombre"),
                firstNonNull(request.getIdDocente(), request.getDocenteId()),
                firstNonNull(request.getGenerarSesionesAutomaticas(), request.getCrearSesionesAutomaticamente()),
                usuarioEjecutor,
                toFranjas(request.getFranjas(), request.getDias(), request.getHoraInicio(), request.getHoraFin())
        );
    }

    public static ActualizarGrupoDTO toApplicationDTO(
            final UUID grupoId,
            final ActualizarGrupoRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(grupoId, "El identificador del grupo es obligatorio.");
        Objects.requireNonNull(request, "El request HTTP para actualizar grupo es obligatorio.");
        return new ActualizarGrupoDTO(
                grupoId,
                request.getCodigo(),
                firstTextOrNull(request.getNombre()),
                firstNonNull(request.getIdDocente(), request.getDocenteId()),
                request.getCupoMaximo(),
                usuarioEjecutor,
                toFranjas(request.getFranjas(), request.getDias(), request.getHoraInicio(), request.getHoraFin())
        );
    }

    /**
     * F1: horario semanal del grupo. {@code franjas} (cada una con su dia y horas) tiene prioridad; el formato anterior
     * {@code dias} + {@code horaInicio}/{@code horaFin} se acepta y aplica las mismas horas a cada dia. Sin ninguno de los
     * dos devuelve {@code null}: el horario no cambia. Las reglas de negocio (traslapes, cruce del docente) son de la DB.
     */
    private static List<FranjaHorarioDTO> toFranjas(
            final List<FranjaHorarioRequest> franjas,
            final List<String> dias,
            final String horaInicio,
            final String horaFin
    ) {
        if (franjas != null) {
            final List<FranjaHorarioDTO> resultado = new ArrayList<>();
            for (final FranjaHorarioRequest franja : franjas) {
                if (franja == null) {
                    throw new ValidationException("ERR_CAMPO_OBLIGATORIO", "Cada franja del horario es obligatoria.");
                }
                resultado.add(toFranja(franja.getDia(), franja.getHoraInicio(), franja.getHoraFin()));
            }
            return resultado;
        }
        if (dias == null && TextHelper.isNullOrBlank(horaInicio) && TextHelper.isNullOrBlank(horaFin)) {
            return null;
        }
        if (dias == null || dias.isEmpty()) {
            throw new ValidationException("ERR_CAMPO_OBLIGATORIO", "El campo dias es obligatorio cuando se envia el horario.");
        }
        return dias.stream().map(dia -> toFranja(dia, horaInicio, horaFin)).toList();
    }

    private static FranjaHorarioDTO toFranja(final String dia, final String horaInicio, final String horaFin) {
        final LocalTime inicio = HttpTemporalParser.parseLocalTime(requiredText(horaInicio, "horaInicio"), "horaInicio");
        final LocalTime fin = HttpTemporalParser.parseLocalTime(requiredText(horaFin, "horaFin"), "horaFin");
        return new FranjaHorarioDTO(codigoDia(requiredText(dia, "dia")), inicio.format(HORA), fin.format(HORA));
    }

    /** Acepta el codigo de dbo.Dia (LU, MA, ...) o el nombre del dia con o sin tildes (Lunes, Miércoles, ...). */
    private static String codigoDia(final String dia) {
        final String sinTildes = Normalizer.normalize(dia, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        return sinTildes.length() > 2 ? sinTildes.substring(0, 2) : sinTildes;
    }

    private static <T> T required(final T value, final String fieldName) {
        if (value == null) {
            throw new ValidationException("ERR_CAMPO_OBLIGATORIO", "El campo " + fieldName + " es obligatorio.");
        }
        return value;
    }

    private static String requiredText(final String value, final String fieldName) {
        final String normalized = firstTextOrNull(value);
        if (normalized == null) {
            throw new ValidationException("ERR_CAMPO_OBLIGATORIO", "El campo " + fieldName + " es obligatorio.");
        }
        return normalized;
    }

    private static String firstTextOrNull(final String preferred) {
        if (!TextHelper.isNullOrBlank(preferred)) {
            return preferred.trim();
        }
        return null;
    }

    private static <T> T firstNonNull(final T preferred, final T alternative) {
        return preferred != null ? preferred : alternative;
    }
}
