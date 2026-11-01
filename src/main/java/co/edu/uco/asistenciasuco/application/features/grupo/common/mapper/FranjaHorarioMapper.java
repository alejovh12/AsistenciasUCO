package co.edu.uco.asistenciasuco.application.features.grupo.common.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.common.dto.FranjaHorarioDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.FranjaHorarioRepositoryProjection;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Conversiones de la franja de horario del grupo entre capas (F1). Una lista {@code null} significa "sin cambio de horario"
 * en los comandos y se conserva como {@code null}; en las consultas se devuelve vacia.
 */
public final class FranjaHorarioMapper {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private FranjaHorarioMapper() {
    }

    public static List<FranjaHorarioDomain> toDomains(final List<FranjaHorarioDTO> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(dto -> new FranjaHorarioDomain(dto.codigoDia(), dto.nombreDia(),
                        LocalTime.parse(dto.horaInicio()), LocalTime.parse(dto.horaFin())))
                .toList();
    }

    public static List<FranjaHorarioRepositoryDTO> toRepositoryDTOs(final List<FranjaHorarioDomain> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(domain -> new FranjaHorarioRepositoryDTO(domain.codigoDia(), domain.horaInicio(), domain.horaFin()))
                .toList();
    }

    public static List<FranjaHorarioDomain> fromProjections(final List<FranjaHorarioRepositoryProjection> projections) {
        if (projections == null) {
            return List.of();
        }
        return projections.stream()
                .map(projection -> new FranjaHorarioDomain(projection.codigoDia(), projection.nombreDia(),
                        projection.horaInicio(), projection.horaFin()))
                .toList();
    }

    public static List<FranjaHorarioDTO> toDTOs(final List<FranjaHorarioDomain> domains) {
        if (domains == null) {
            return List.of();
        }
        return domains.stream()
                .map(domain -> new FranjaHorarioDTO(domain.codigoDia(), domain.nombreDia(),
                        domain.horaInicio().format(HORA), domain.horaFin().format(HORA)))
                .toList();
    }
}
