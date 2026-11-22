package co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DEC-081: el ADMINISTRADOR registra un dia de receso academico en un periodo. La DB valida y cancela al instante las sesiones de ese dia en
 * los grupos del periodo; despues del commit el backend avisa por correo (UN correo por persona afectada) y publica el evento
 * {@code SESION_CANCELADA} de cada grupo afectado. Los avisos son de mejor esfuerzo: un fallo no revierte el receso ni cambia la respuesta.
 */
public interface CrearRecesoAcademicoInputPort
        extends InteractorWithReturn<CrearRecesoAcademicoInputPort.Comando, CrearRecesoAcademicoInputPort.Resultado> {

    record Comando(UUID idPeriodo, LocalDate fecha, String motivo, UUID usuarioEjecutor) {
    }

    /**
     * {@code mensajeUsuario} es el REC_001 que entrego la DB. {@code avisosEncolados} y {@code gruposNotificados} son informativos
     * (correos encolados y grupos con evento SSE publicado); no forman parte de la respuesta HTTP.
     */
    record Resultado(UUID id, LocalDate fecha, String motivo, int sesionesCanceladas, int sesionesConAsistencia, String mensajeUsuario,
                     int avisosEncolados, int gruposNotificados) {
    }
}
