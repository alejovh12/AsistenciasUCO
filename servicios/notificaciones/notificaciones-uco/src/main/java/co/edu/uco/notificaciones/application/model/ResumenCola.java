package co.edu.uco.notificaciones.application.model;

public record ResumenCola(long pendientes, long reservadas, long entregadas, long fallidasReintentables, long fallidasDefinitivas) {
}
