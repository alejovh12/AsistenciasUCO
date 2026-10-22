package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteCandidatoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;

import java.util.List;
import java.util.UUID;

/** Puerto de salida de lectura del directorio de docentes del programa y de candidatos (HU062, HU063, HU089, HU091). */
public interface DocenteProgramaQueryPort {

    DocentesProgramaPagina consultar(UUID usuarioEjecutor, DocentesProgramaFiltro filtro);

    List<DocenteCandidatoProjection> consultarCandidatos(UUID usuarioEjecutor, UUID idPrograma, String busqueda);
}
