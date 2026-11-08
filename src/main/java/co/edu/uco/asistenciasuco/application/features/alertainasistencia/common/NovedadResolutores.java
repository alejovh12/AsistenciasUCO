package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NovedadResolutores(UUID idRevision, UUID idGrupo, String nombreGrupo, String nombreAsignatura, UUID idEstudianteGrupo,
                          String perfilResolutor, String evento, BigDecimal porcentaje, BigDecimal umbral, String motivo,
                          String nombreEstudiante, String nombreDocente, UUID idUsuarioDocente, UUID idUsuarioEstudiante,
                          List<Resolutor> resolutores) {
}
