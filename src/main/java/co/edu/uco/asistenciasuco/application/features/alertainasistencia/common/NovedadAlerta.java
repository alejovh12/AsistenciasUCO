package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.math.BigDecimal;
import java.util.UUID;

public record NovedadAlerta(UUID idAlerta, String codigoTipo, int nivel, boolean esPrincipal, BigDecimal porcentaje, BigDecimal umbral,
                     BigDecimal umbralPerdida, BigDecimal horasFalta, BigDecimal horasDictadas, UUID idEstudianteGrupo, UUID idGrupo, int codigoGrupo,
                     String nombreGrupo, String nombreAsignatura, String tipoPlantillaEstudiante, Persona estudiante,
                     Persona docente, UUID idRevisionCreada) {
}
