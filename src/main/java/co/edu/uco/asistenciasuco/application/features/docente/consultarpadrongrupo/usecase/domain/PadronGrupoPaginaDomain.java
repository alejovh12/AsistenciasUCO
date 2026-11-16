package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain;

import java.util.List;

/** Pagina del padron resuelta por el caso de uso: filas, pagina y tamano efectivos y total filtrado. */
public record PadronGrupoPaginaDomain(List<EstudiantePadronDomain> estudiantes, int pagina, int tamano, long totalRegistros) {
}
