package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;
import java.util.UUID;
public record EstudianteGrupoQueryRow(UUID id, UUID idEstudiante, Integer documento, String nombreCompleto,
                                      String correo, String codigoEstado, String nombreEstado) { }
