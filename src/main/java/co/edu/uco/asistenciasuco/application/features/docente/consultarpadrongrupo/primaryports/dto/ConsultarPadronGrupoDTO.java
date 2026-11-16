package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto;

import java.util.UUID;

/** Entrada: grupo, ejecutor (usuario autenticado) y paginacion/busqueda opcionales. */
public record ConsultarPadronGrupoDTO(UUID grupoId, UUID usuarioEjecutor, Integer pagina, Integer tamano, String busqueda) {
}
