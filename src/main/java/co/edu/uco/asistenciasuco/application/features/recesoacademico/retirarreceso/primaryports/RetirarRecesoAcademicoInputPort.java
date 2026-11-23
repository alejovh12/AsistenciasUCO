package co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * DEC-081: el ADMINISTRADOR retira un dia de receso que todavia no canceló ninguna sesion. Si ya cancelo clases la DB responde
 * {@code REC_010} (409). Corregir un dia es retirarlo y crear otro: no hay «actualizar».
 */
public interface RetirarRecesoAcademicoInputPort
        extends InteractorWithReturn<RetirarRecesoAcademicoInputPort.Comando, RetirarRecesoAcademicoInputPort.Resultado> {

    record Comando(UUID idReceso, UUID usuarioEjecutor) {
    }

    /** {@code mensajeUsuario} es el REC_002 que entrego la DB. */
    record Resultado(String mensajeUsuario) {
    }
}
