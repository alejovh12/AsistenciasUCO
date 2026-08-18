package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Adaptador de Spring Security Crypto para codificar passwords.
 */
public final class SpringPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordEncoderAdapter() {
        this(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    SpringPasswordEncoderAdapter(final PasswordEncoder passwordEncoder) {
        if (ObjectHelper.isNull(passwordEncoder)) {
            throw new CrosscuttingException("El PasswordEncoder de Spring es obligatorio.");
        }
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(final String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(final String rawPassword, final String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
