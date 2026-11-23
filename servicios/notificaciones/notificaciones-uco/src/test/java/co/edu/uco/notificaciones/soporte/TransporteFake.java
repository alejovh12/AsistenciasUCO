package co.edu.uco.notificaciones.soporte;

import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.domain.CorreoSaliente;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TransporteFake implements CorreoTransportPort {

    public final List<CorreoSaliente> enviados = new ArrayList<>();
    public Consumer<CorreoSaliente> comportamiento = c -> { };
    public boolean activo = true;

    @Override
    public void enviar(final CorreoSaliente correo) {
        comportamiento.accept(correo);
        enviados.add(correo);
    }

    @Override
    public boolean activo() {
        return activo;
    }
}
