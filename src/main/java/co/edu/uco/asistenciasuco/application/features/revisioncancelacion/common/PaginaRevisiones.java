package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import java.util.List;

public record PaginaRevisiones(List<RevisionFila> filas, long total) {
}
