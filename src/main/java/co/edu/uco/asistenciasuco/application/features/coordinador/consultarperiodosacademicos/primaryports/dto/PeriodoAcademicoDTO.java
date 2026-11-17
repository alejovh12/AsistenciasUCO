package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.dto;

import java.time.LocalDate;
import java.util.UUID;

/** {@code estado}: PLANEACION, ACTIVO o CERRADO, resuelto en la BD con la fecha local institucional (CIERRE-W2A). */
public record PeriodoAcademicoDTO(UUID id, UUID idInstitucion, String nombreInstitucion, String nombre,
                                  String codigo, LocalDate fechaInicio, LocalDate fechaFin, Integer anio, String estado) {

    /** Sin estado: solo para pruebas y consumidores que no lo necesitan. */
    public PeriodoAcademicoDTO(UUID id, UUID idInstitucion, String nombreInstitucion, String nombre,
                               String codigo, LocalDate fechaInicio, LocalDate fechaFin, Integer anio) {
        this(id, idInstitucion, nombreInstitucion, nombre, codigo, fechaInicio, fechaFin, anio, null);
    }
}
