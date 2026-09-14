package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.mapper.JdbcValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FacultadSqlServerAdapter implements FacultadQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public FacultadSqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<FacultadProjection> consultarFacultades() {
        return jdbcOperations.query("""
                SELECT id, nombreFacultad, idInstitucion, nombreInstitucion, idDecano, nombreCompletoDecano,
                       estaActivaFacultad, estaActivaTextoFacultad
                FROM dbo.uv_facultad
                ORDER BY nombreInstitucion, nombreFacultad, id
                """, (rs, rowNum) -> mapFacultad(rs.getObject("id"),
                rs.getObject("nombreFacultad"),
                rs.getObject("idInstitucion"),
                rs.getObject("nombreInstitucion"),
                rs.getObject("idDecano"),
                rs.getObject("nombreCompletoDecano"),
                rs.getObject("estaActivaFacultad"),
                rs.getObject("estaActivaTextoFacultad")
        ));
    }

    @Override
    public Optional<FacultadProjection> consultarFacultadPorId(final UUID idFacultad) {
        final List<FacultadProjection> result = jdbcOperations.query("""
                SELECT id, nombreFacultad, idInstitucion, nombreInstitucion, idDecano, nombreCompletoDecano,
                       estaActivaFacultad, estaActivaTextoFacultad
                FROM dbo.uv_facultad
                WHERE id = :idFacultad
                """, new MapSqlParameterSource("idFacultad", idFacultad), (rs, rowNum) -> mapFacultad(
                rs.getObject("id"),
                rs.getObject("nombreFacultad"),
                rs.getObject("idInstitucion"),
                rs.getObject("nombreInstitucion"),
                rs.getObject("idDecano"),
                rs.getObject("nombreCompletoDecano"),
                rs.getObject("estaActivaFacultad"),
                rs.getObject("estaActivaTextoFacultad")
        ));
        return result.stream().findFirst();
    }

    private static FacultadProjection mapFacultad(
            final Object id,
            final Object nombreFacultad,
            final Object idInstitucion,
            final Object nombreInstitucion,
            final Object idDecano,
            final Object nombreCompletoDecano,
            final Object estaActivaFacultad,
            final Object estaActivaTextoFacultad
    ) {
        return new FacultadProjection(
                JdbcValueMapper.toUuid(id),
                JdbcValueMapper.toString(nombreFacultad),
                JdbcValueMapper.toUuid(idInstitucion),
                JdbcValueMapper.toString(nombreInstitucion),
                JdbcValueMapper.toUuid(idDecano),
                JdbcValueMapper.toString(nombreCompletoDecano),
                JdbcValueMapper.toBoolean(estaActivaFacultad),
                JdbcValueMapper.toString(estaActivaTextoFacultad)
        );
    }
}
