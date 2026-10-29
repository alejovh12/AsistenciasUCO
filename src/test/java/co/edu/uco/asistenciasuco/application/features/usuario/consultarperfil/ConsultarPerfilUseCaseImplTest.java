package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.PerfilUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.interactor.ConsultarPerfilInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain.PerfilUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.impl.ConsultarPerfilUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.PerfilUsuarioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.PerfilUsuarioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.VinculoUsuarioProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU006: perfil del usuario autenticado (PU-01..PU-05). */
class ConsultarPerfilUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final PerfilUsuarioQueryPort port = mock(PerfilUsuarioQueryPort.class);
    private final ConsultarPerfilUseCaseImpl useCase = new ConsultarPerfilUseCaseImpl(port);

    private static PerfilUsuarioProjection perfil(final List<VinculoUsuarioProjection> vinculos) {
        return new PerfilUsuarioProjection(ACTOR, "CC", "Cedula", 123, "Ana", "Maria", "Perez", "Gomez", "Ana Maria Perez Gomez",
                "ana@uco.edu.co", true, true, vinculos);
    }

    @Test
    void PU01_devuelve_datos_personales_y_vinculos() {
        when(port.consultarPerfil(ACTOR)).thenReturn(Optional.of(perfil(List.of(
                new VinculoUsuarioProjection("ESTUDIANTE", PROGRAMA, "Ingenieria", "Facultad de Ingenieria", true),
                new VinculoUsuarioProjection("DOCENTE", PROGRAMA, "Ingenieria", "Catedra", false)))));

        final PerfilUsuarioDomain d = useCase.execute(ACTOR);

        assertEquals(ACTOR, d.idUsuario());
        assertEquals("Ana Maria Perez Gomez", d.nombreCompleto());
        assertEquals(123, d.numeroIdentificacion());
        assertEquals(2, d.vinculos().size());
        assertEquals("DOCENTE", d.vinculos().get(1).rol());
        assertFalse(d.vinculos().get(1).estaActivo());
    }

    @Test
    void PU02_usuario_sin_vinculos() {
        when(port.consultarPerfil(ACTOR)).thenReturn(Optional.of(perfil(null)));

        assertTrue(useCase.execute(ACTOR).vinculos().isEmpty());
    }

    @Test
    void PU03_usuario_inexistente_responde_404() {
        when(port.consultarPerfil(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(ACTOR));
    }

    @Test
    void PU04_actor_nulo_es_validacion_sin_consultar() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verifyNoInteractions(port);
        assertThrows(NullPointerException.class, () -> new ConsultarPerfilUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarPerfilInteractor(null));
        assertTrue(new PerfilUsuarioDomain(ACTOR, null, null, null, null, null, null, null, null, null, false, false, null)
                .vinculos().isEmpty());
    }

    @Test
    void PU05_interactor_mapea_a_dto() {
        when(port.consultarPerfil(ACTOR)).thenReturn(Optional.of(perfil(List.of(
                new VinculoUsuarioProjection("COORDINADOR", PROGRAMA, "Ingenieria", "Facultad", true)))));

        final PerfilUsuarioDTO dto = new ConsultarPerfilInteractor(useCase).execute(ACTOR);

        assertEquals("ana@uco.edu.co", dto.correo());
        assertEquals("CC", dto.codigoTipoIdentificacion());
        assertTrue(dto.correoConfirmado());
        assertEquals("COORDINADOR", dto.vinculos().getFirst().rol());
        assertEquals(PROGRAMA, dto.vinculos().getFirst().idEntidad());
        assertEquals("Facultad", dto.vinculos().getFirst().detalle());
    }
}
