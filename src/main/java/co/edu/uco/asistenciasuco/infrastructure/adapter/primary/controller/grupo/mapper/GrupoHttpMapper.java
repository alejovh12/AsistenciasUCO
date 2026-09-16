package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.ActualizarGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.CrearGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpTemporalParser;

import java.util.Objects;
import java.util.UUID;

public final class GrupoHttpMapper {

    private GrupoHttpMapper() {
    }

    public static CrearGrupoDTO toApplicationDTO(final CrearGrupoRequest request, final UUID usuarioEjecutor) {
        Objects.requireNonNull(request, "El request HTTP para crear grupo es obligatorio.");
        rejectHorarioIfPresent(request.getDias(), request.getHoraInicio(), request.getHoraFin());
        return new CrearGrupoDTO(
                firstNonNull(request.getIdAsignatura(), request.getAsignaturaId()),
                firstNonNull(request.getIdPeriodoAcademico(), request.getPeriodoAcademicoId()),
                required(request.getCodigo(), "codigo"),
                requiredText(request.getNombre(), "nombre"),
                firstNonNull(request.getIdDocente(), request.getDocenteId()),
                requiredText(request.getAula(), "aula"),
                firstNonNull(request.getGenerarSesionesAutomaticas(), request.getCrearSesionesAutomaticamente()),
                usuarioEjecutor
        );
    }

    public static ActualizarGrupoDTO toApplicationDTO(
            final UUID grupoId,
            final ActualizarGrupoRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(grupoId, "El identificador del grupo es obligatorio.");
        Objects.requireNonNull(request, "El request HTTP para actualizar grupo es obligatorio.");
        rejectHorarioIfPresent(request.getDias(), request.getHoraInicio(), request.getHoraFin());
        return new ActualizarGrupoDTO(
                grupoId,
                request.getCodigo(),
                firstTextOrNull(request.getNombre()),
                firstNonNull(request.getIdDocente(), request.getDocenteId()),
                request.getCupoMaximo(),
                firstTextOrNull(request.getAula()),
                usuarioEjecutor
        );
    }

    private static void rejectHorarioIfPresent(
            final Object dias,
            final String horaInicio,
            final String horaFin
    ) {
        if (!TextHelper.isNullOrBlank(horaInicio)) {
            HttpTemporalParser.parseLocalTime(horaInicio, "horaInicio");
        }
        if (!TextHelper.isNullOrBlank(horaFin)) {
            HttpTemporalParser.parseLocalTime(horaFin, "horaFin");
        }
        if (dias != null || !TextHelper.isNullOrBlank(horaInicio) || !TextHelper.isNullOrBlank(horaFin)) {
            throw new FeatureUnavailableException(
                    "La gestion de horarios de grupo no esta disponible porque la DB no publica un command para esta capacidad."
            );
        }
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
