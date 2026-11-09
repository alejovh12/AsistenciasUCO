package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.List;

public record PaginaAlertas(List<AlertaFila> filas, long total) {
}
