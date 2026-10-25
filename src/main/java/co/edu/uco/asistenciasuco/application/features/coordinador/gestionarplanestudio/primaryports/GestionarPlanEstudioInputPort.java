package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;

import java.util.UUID;

public interface GestionarPlanEstudioInputPort {

    void guardar(GuardarPlanEstudioDTO dto);

    /** HU081: alterna el estado (activo/inactivo) del plan del programa del coordinador. */
    void cambiarEstado(UUID idPlanEstudio, UUID usuario);

    /** HU082: agrega el siguiente semestre consecutivo al plan. */
    void agregarSemestre(UUID idPlanEstudio, UUID usuario);

    /** HU083: quita el ultimo semestre del plan si no tiene asignaturas. */
    void quitarSemestre(UUID idPlanEstudio, Integer numeroSemestre, UUID usuario);
}
