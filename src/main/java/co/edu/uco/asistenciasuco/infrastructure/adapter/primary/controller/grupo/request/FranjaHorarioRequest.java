package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request;

/**
 * Franja del horario semanal en el cuerpo de crear/actualizar grupo (F1). {@code dia} acepta el codigo (LU, MA, ...) o el
 * nombre del dia (Lunes, Martes, ...); las horas van en formato "HH:mm".
 */
public final class FranjaHorarioRequest {

    private String dia;
    private String horaInicio;
    private String horaFin;

    public String getDia() {
        return dia;
    }

    public void setDia(final String dia) {
        this.dia = dia;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(final String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(final String horaFin) {
        this.horaFin = horaFin;
    }
}
