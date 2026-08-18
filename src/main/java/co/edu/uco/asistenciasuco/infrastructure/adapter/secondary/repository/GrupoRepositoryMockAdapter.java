package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.GrupoRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Adaptador mock temporal para persistencia de estudiantes en grupos.
 */
public final class GrupoRepositoryMockAdapter implements GrupoRepositoryPort {

    @Override
    public RegistrarEstudianteRepositoryEntity registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        return new RegistrarEstudianteRepositoryEntity("Estudiante registrado correctamente.");
    }

    @Override
    public List<GrupoRepositoryEntity> consultarGrupos() {
        return List.of();
    }
}
