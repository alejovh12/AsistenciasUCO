package co.edu.uco.asistenciasuco.crosscutting.validation;

public interface Validator<T> {

    ValidationResult validate(T value);
}
