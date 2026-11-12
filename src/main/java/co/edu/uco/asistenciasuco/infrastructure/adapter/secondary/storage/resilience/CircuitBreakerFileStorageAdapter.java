package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.resilience;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * REA-02: decorador del {@link FileStoragePort} con circuit breaker (Resilience4j).
 *
 * <p>Solo cuenta como fallo la indisponibilidad del provider ({@link StorageUnavailableException}); un objeto
 * inexistente ({@link ObjectNotFoundException}) es una respuesta valida del provider. Con el breaker abierto no se
 * intenta la llamada (no se esperan los timeouts de transporte) y se responde con la misma
 * {@link StorageUnavailableException} que ya traduce Application: fail-closed, nunca un exito simulado.</p>
 *
 * <p>Los timeouts por llamada siguen siendo los del transporte del provider (connect/read/write de MinIO); el breaker
 * evita repetirlos mientras el provider esta caido. Vive en Infrastructure: Application solo conoce el puerto.</p>
 */
public final class CircuitBreakerFileStorageAdapter implements FileStoragePort {

    static final String MENSAJE_ABIERTO = "El almacenamiento de archivos no esta disponible temporalmente.";

    private final FileStoragePort delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerFileStorageAdapter(final FileStoragePort delegate, final CircuitBreaker circuitBreaker) {
        this.delegate = Objects.requireNonNull(delegate, "El FileStoragePort decorado es obligatorio.");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "El circuit breaker del storage es obligatorio.");
    }

    @Override
    public StoredObject store(final StoreObjectCommand command) {
        return proteger(() -> delegate.store(command));
    }

    @Override
    public StoredObject read(final String fileId) {
        return proteger(() -> delegate.read(fileId));
    }

    @Override
    public boolean exists(final String fileId) {
        return proteger(() -> delegate.exists(fileId));
    }

    @Override
    public void delete(final String fileId) {
        proteger(() -> {
            delegate.delete(fileId);
            return null;
        });
    }

    private <T> T proteger(final Supplier<T> llamada) {
        if (!circuitBreaker.tryAcquirePermission()) {
            throw new StorageUnavailableException(MENSAJE_ABIERTO, null);
        }
        final long inicio = circuitBreaker.getCurrentTimestamp();
        try {
            final T resultado = llamada.get();
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - inicio, circuitBreaker.getTimestampUnit());
            return resultado;
        } catch (final ObjectNotFoundException exception) {
            circuitBreaker.onSuccess(circuitBreaker.getCurrentTimestamp() - inicio, circuitBreaker.getTimestampUnit());
            throw exception;
        } catch (final StorageException exception) {
            circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - inicio, circuitBreaker.getTimestampUnit(), exception);
            throw exception;
        } catch (final RuntimeException | Error exception) {
            // Un error de programacion o de argumentos no dice nada de la salud del provider: no se cuenta.
            circuitBreaker.releasePermission();
            throw exception;
        }
    }
}
