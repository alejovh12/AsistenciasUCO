package co.edu.uco.asistenciasuco.application.features.docente;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.interactor.AsignarDocenteAGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.impl.AsignarDocenteAGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl.ConsultarDocentePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.interactor.RegistrarDocenteDesdeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.impl.RegistrarDocenteDesdeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteAsignacionAcademicaRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteOperacionRepositoryEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocenteOperacionesUseCaseTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void registrar_docente_desde_usuario_retorna_resultado_sin_id_docente_ni_mensaje_tecnico() {
        final AtomicReference<RegistrarDocenteDesdeUsuarioRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final DocenteRepositoryPort port = new StubDocenteRepositoryPort() {
            @Override
            public DocenteOperacionRepositoryEntity registrarDocenteDesdeUsuario(
                    final RegistrarDocenteDesdeUsuarioRepositoryDTO dto
            ) {
                dtoCapturado.set(dto);
                return new DocenteOperacionRepositoryEntity("Docente registrado.");
            }
        };

        final RegistrarDocenteDesdeUsuarioUseCaseImpl useCase = new RegistrarDocenteDesdeUsuarioUseCaseImpl(port);
        final var resultado = useCase.execute(RegistrarDocenteDesdeUsuarioDomain.crear(USUARIO));

        assertTrue(resultado.isExitoso());
        assertEquals("Docente registrado.", resultado.getMensajeUsuario());
        assertEquals(USUARIO, dtoCapturado.get().getUsuario());
        assertFalse(tieneCampo(RegistrarDocenteDesdeUsuarioResultadoDTO.class, "mensajeTecnicoResultado"));
    }

    @Test
    void registrar_docente_desde_usuario_propaga_error_funcional_del_repositorio() {
        final DocenteRepositoryPort port = new StubDocenteRepositoryPort() {
            @Override
            public DocenteOperacionRepositoryEntity registrarDocenteDesdeUsuario(
                    final RegistrarDocenteDesdeUsuarioRepositoryDTO dto
            ) {
                throw new ConflictException("El usuario ya es docente.");
            }
        };

        final RegistrarDocenteDesdeUsuarioUseCaseImpl useCase = new RegistrarDocenteDesdeUsuarioUseCaseImpl(port);

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> useCase.execute(RegistrarDocenteDesdeUsuarioDomain.crear(USUARIO))
        );

        assertEquals("CONFLICT", exception.getCode());
        assertEquals("El usuario ya es docente.", exception.getMessage());
    }

    @Test
    void asignar_docente_a_grupo_retorna_resultado_y_permite_reasignacion() {
        final AtomicReference<AsignarDocenteAGrupoRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final DocenteRepositoryPort port = new StubDocenteRepositoryPort() {
            @Override
            public DocenteOperacionRepositoryEntity asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
                dtoCapturado.set(dto);
                return new DocenteOperacionRepositoryEntity("Docente asignado.");
            }
        };

        final AsignarDocenteAGrupoUseCaseImpl useCase = new AsignarDocenteAGrupoUseCaseImpl(port);
        final var resultado = useCase.execute(AsignarDocenteAGrupoDomain.crear(DOCENTE, GRUPO));

        assertTrue(resultado.isExitoso());
        assertEquals("Docente asignado.", resultado.getMensajeUsuario());
        assertEquals(DOCENTE, dtoCapturado.get().getDocente());
        assertEquals(GRUPO, dtoCapturado.get().getGrupo());
        assertFalse(tieneCampo(AsignarDocenteAGrupoResultadoDTO.class, "mensajeTecnicoResultado"));
    }

    @Test
    void asignar_docente_a_grupo_propaga_error_funcional_del_repositorio() {
        final DocenteRepositoryPort port = new StubDocenteRepositoryPort() {
            @Override
            public DocenteOperacionRepositoryEntity asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
                throw new ConflictException("Existe cruce de horario.");
            }
        };

        final AsignarDocenteAGrupoUseCaseImpl useCase = new AsignarDocenteAGrupoUseCaseImpl(port);

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> useCase.execute(AsignarDocenteAGrupoDomain.crear(DOCENTE, GRUPO))
        );

        assertEquals("CONFLICT", exception.getCode());
        assertEquals("Existe cruce de horario.", exception.getMessage());
    }

    @Test
    void consultar_docente_por_id_inexistente_lanza_resourceNotFoundException_con_codigo_especifico() {
        final ConsultarDocentePorIdUseCaseImpl useCase = new ConsultarDocentePorIdUseCaseImpl(
                new StubDocenteRepositoryPort() {
                }
        );

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> useCase.execute(ConsultarDocentePorIdDomain.crear(DOCENTE))
        );

        assertEquals("ERR_DOCENTE_NO_EXISTE", exception.getCode());
        assertEquals("El docente consultado no existe.", exception.getMessage());
    }

    @Test
    void interactors_mapean_request_y_response() {
        final RegistrarDocenteDesdeUsuarioInteractor registrarInteractor =
                new RegistrarDocenteDesdeUsuarioInteractor(domain ->
                        new co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario
                                .usecase.entity.RegistrarDocenteDesdeUsuarioResultadoEntity(
                                true,
                                "Docente registrado."
                        )
                );
        final AsignarDocenteAGrupoInteractor asignarInteractor =
                new AsignarDocenteAGrupoInteractor(domain ->
                        new co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo
                                .usecase.entity.AsignarDocenteAGrupoResultadoEntity(
                                true,
                                "Docente asignado."
                        )
                );

        final RegistrarDocenteDesdeUsuarioResultadoDTO registro =
                registrarInteractor.execute(new RegistrarDocenteDesdeUsuarioDTO(USUARIO));
        final AsignarDocenteAGrupoResultadoDTO asignacion =
                asignarInteractor.execute(new AsignarDocenteAGrupoDTO(DOCENTE, GRUPO));

        assertTrue(registro.isExitoso());
        assertTrue(asignacion.isExitoso());
    }

    private boolean tieneCampo(final Class<?> type, final String fieldName) {
        for (final Field field : type.getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }

    private abstract static class StubDocenteRepositoryPort implements DocenteRepositoryPort {

        @Override
        public List<DocenteIdentidadRepositoryEntity> consultarDocentes() {
            return List.of();
        }

        @Override
        public Optional<DocenteIdentidadRepositoryEntity> consultarDocentePorId(
                final ConsultarDocentePorIdRepositoryDTO dto
        ) {
            return Optional.empty();
        }

        @Override
        public List<DocenteAsignacionAcademicaRepositoryEntity> consultarAsignacionesAcademicas(
                final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto
        ) {
            return List.of();
        }

        @Override
        public DocenteOperacionRepositoryEntity registrarDocenteDesdeUsuario(
                final RegistrarDocenteDesdeUsuarioRepositoryDTO dto
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DocenteOperacionRepositoryEntity asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
            throw new UnsupportedOperationException();
        }
    }
}
