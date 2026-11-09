package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.List;

public record ResultadoEvaluacion(int alertasCreadas, int alertasSuperadas, int alertasReactivadas, int revisionesCreadas,
                           List<NovedadAlerta> novedades) {
}
