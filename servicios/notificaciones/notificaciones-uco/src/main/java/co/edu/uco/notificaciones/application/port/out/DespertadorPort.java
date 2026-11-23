package co.edu.uco.notificaciones.application.port.out;

/** Puerto de salida: avisa al despachador que hay trabajo nuevo para no esperar al siguiente barrido. */
public interface DespertadorPort {

    void despertar();
}
