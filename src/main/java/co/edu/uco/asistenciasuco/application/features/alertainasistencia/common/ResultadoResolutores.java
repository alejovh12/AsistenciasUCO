package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.List;

public record ResultadoResolutores(int reasignadas, int escaladas, int sinResolutor, List<NovedadResolutores> novedades) {
}
