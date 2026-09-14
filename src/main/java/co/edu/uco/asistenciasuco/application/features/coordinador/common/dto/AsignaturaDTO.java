package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.util.UUID;

public record AsignaturaDTO(UUID id, String codigo, String nombre, Integer credito, UUID idArea, String nombreArea,
                            UUID idComponente, String nombreComponente, UUID idSemestrePlanEstudio,
                            UUID idPlanEstudio, UUID idPrograma, String nombrePrograma, String codigoSemestre,
                            boolean estaActivaAsignatura, String estaActivaTextoAsignatura) {
}
