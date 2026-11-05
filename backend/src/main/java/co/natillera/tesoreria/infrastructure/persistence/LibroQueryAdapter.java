package co.natillera.tesoreria.infrastructure.persistence;

import co.natillera.shared.domain.Dinero;
import co.natillera.tesoreria.application.port.out.LibroQueryPort;
import co.natillera.tesoreria.domain.SaldoCajita;
import co.natillera.tesoreria.domain.TipoMovimiento;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/** Consultas agregadas del libro con SQL nativo (GROUP BY) y proyección directa a records. */
@Component
public class LibroQueryAdapter implements LibroQueryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public LibroQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SaldoCajita> saldosCajitas(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT c.id AS cajita_id, c.nombre AS cajita, i.id AS integrante_id, i.nombre AS integrante,
                       t.id AS tesorero_id, t.nombre AS tesorero,
                       COALESCE(m.entradas, 0) AS entradas, COALESCE(m.salidas, 0) AS salidas,
                       s.saldo_app, s.fecha AS fecha_saldo_app
                FROM cajita c
                JOIN integrante i ON i.id = c.integrante_id
                LEFT JOIN tesorero t ON t.id = i.tesorero_id
                LEFT JOIN (SELECT cajita_id, SUM(entrada) AS entradas, SUM(salida) AS salidas
                           FROM movimiento GROUP BY cajita_id) m ON m.cajita_id = c.id
                LEFT JOIN (SELECT cajita_id, saldo_app, fecha
                           FROM (SELECT cajita_id, saldo_app, fecha,
                                        ROW_NUMBER() OVER (PARTITION BY cajita_id ORDER BY fecha DESC, id DESC) AS n
                                 FROM saldo_reportado_nu) r
                           WHERE n = 1) s ON s.cajita_id = c.id
                WHERE 1 = 1 %s
                ORDER BY i.id
                """.formatted(filtro(integrantes, "i.id"));
        return jdbc.query(sql, params(integrantes), (rs, n) -> new SaldoCajita(
                rs.getLong("cajita_id"), rs.getString("cajita"), rs.getLong("integrante_id"),
                rs.getString("integrante"), (Long) rs.getObject("tesorero_id", Long.class), rs.getString("tesorero"),
                dinero(rs, "entradas"), dinero(rs, "salidas"), dineroNulo(rs, "saldo_app"),
                fecha(rs.getDate("fecha_saldo_app"))));
    }

    @Override
    public Dinero saldoCajita(Long cajitaId) {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT COALESCE(SUM(entrada - salida), 0) FROM movimiento WHERE cajita_id = :id",
                new MapSqlParameterSource("id", cajitaId), BigDecimal.class);
        return Dinero.de(saldo);
    }

    @Override
    public List<AporteAnioMes> aportesPorIntegranteYMes(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT c.integrante_id, EXTRACT(YEAR FROM m.fecha) AS anio,
                       EXTRACT(MONTH FROM m.fecha) AS mes, SUM(m.entrada - m.salida) AS monto
                FROM movimiento m JOIN cajita c ON c.id = m.cajita_id
                WHERE m.tipo = 'AHORRO' %s
                GROUP BY c.integrante_id, anio, mes
                """.formatted(filtro(integrantes, "c.integrante_id"));
        return jdbc.query(sql, params(integrantes), (rs, n) -> new AporteAnioMes(rs.getLong("integrante_id"),
                rs.getInt("anio"), rs.getInt("mes"), dinero(rs, "monto")));
    }

    @Override
    public List<MontoPorIntegrante> rendimientoNuPorIntegrante(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT c.integrante_id, SUM(m.entrada - m.salida) AS monto
                FROM movimiento m JOIN cajita c ON c.id = m.cajita_id
                WHERE m.tipo = 'RENDIMIENTO_NU' %s
                GROUP BY c.integrante_id
                """.formatted(filtro(integrantes, "c.integrante_id"));
        return jdbc.query(sql, params(integrantes),
                (rs, n) -> new MontoPorIntegrante(rs.getLong("integrante_id"), dinero(rs, "monto")));
    }

    @Override
    public List<PagoBoletas> pagosBoletas(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT m.actividad_id, c.integrante_id, SUM(m.entrada - m.salida) AS monto
                FROM movimiento m JOIN cajita c ON c.id = m.cajita_id
                WHERE m.tipo = 'PAGO_BOLETAS_RIFA' AND m.actividad_id IS NOT NULL %s
                GROUP BY m.actividad_id, c.integrante_id
                """.formatted(filtro(integrantes, "c.integrante_id"));
        return jdbc.query(sql, params(integrantes), (rs, n) -> new PagoBoletas(rs.getLong("actividad_id"),
                rs.getLong("integrante_id"), dinero(rs, "monto")));
    }

    @Override
    public List<TotalTipo> totalesPorTipo(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT m.tipo, SUM(m.entrada) AS entradas, SUM(m.salida) AS salidas
                FROM movimiento m JOIN cajita c ON c.id = m.cajita_id
                WHERE 1 = 1 %s
                GROUP BY m.tipo
                """.formatted(filtro(integrantes, "c.integrante_id"));
        return jdbc.query(sql, params(integrantes), (rs, n) -> new TotalTipo(
                TipoMovimiento.valueOf(rs.getString("tipo")), dinero(rs, "entradas"), dinero(rs, "salidas")));
    }

    @Override
    public List<FlujoAnioMes> flujoPorMesYTesorero(Collection<Long> integrantes) {
        if (vacio(integrantes)) {
            return List.of();
        }
        String sql = """
                SELECT EXTRACT(YEAR FROM m.fecha) AS anio, EXTRACT(MONTH FROM m.fecha) AS mes,
                       i.tesorero_id, SUM(m.entrada) AS entradas, SUM(m.salida) AS salidas
                FROM movimiento m JOIN cajita c ON c.id = m.cajita_id JOIN integrante i ON i.id = c.integrante_id
                WHERE 1 = 1 %s
                GROUP BY anio, mes, i.tesorero_id
                """.formatted(filtro(integrantes, "c.integrante_id"));
        return jdbc.query(sql, params(integrantes), (rs, n) -> new FlujoAnioMes(rs.getInt("anio"), rs.getInt("mes"),
                (Long) rs.getObject("tesorero_id", Long.class), dinero(rs, "entradas"), dinero(rs, "salidas")));
    }

    @Override
    public List<MovimientoVista> movimientos(FiltroMovimientos f) {
        if (vacio(f.integrantes())) {
            return List.of();
        }
        MapSqlParameterSource p = params(f.integrantes());
        StringBuilder where = new StringBuilder(filtro(f.integrantes(), "c.integrante_id"));
        if (f.cajitaId() != null) {
            where.append(" AND m.cajita_id = :cajitaId");
            p.addValue("cajitaId", f.cajitaId());
        }
        if (f.tipo() != null) {
            where.append(" AND m.tipo = :tipo");
            p.addValue("tipo", f.tipo().name());
        }
        if (f.desde() != null) {
            where.append(" AND m.fecha >= :desde");
            p.addValue("desde", Date.valueOf(f.desde()));
        }
        if (f.hasta() != null) {
            where.append(" AND m.fecha <= :hasta");
            p.addValue("hasta", Date.valueOf(f.hasta()));
        }
        p.addValue("limite", f.limite());
        String sql = """
                SELECT m.id, m.fecha, m.cajita_id, c.nombre AS cajita, i.id AS integrante_id, i.nombre AS integrante,
                       t.id AS tesorero_id, t.nombre AS tesorero, m.tipo, m.descripcion, m.entrada, m.salida,
                       m.prestamo_id, m.actividad_id, m.anula_a, a.id AS anulado_por, m.creado_por, m.creado_en
                FROM movimiento m
                JOIN cajita c ON c.id = m.cajita_id
                JOIN integrante i ON i.id = c.integrante_id
                LEFT JOIN tesorero t ON t.id = i.tesorero_id
                LEFT JOIN movimiento a ON a.anula_a = m.id
                WHERE 1 = 1 %s
                ORDER BY m.fecha DESC, m.id DESC
                LIMIT :limite
                """.formatted(where);
        return jdbc.query(sql, p, (rs, n) -> new MovimientoVista(rs.getLong("id"), fecha(rs.getDate("fecha")),
                rs.getLong("cajita_id"), rs.getString("cajita"), rs.getLong("integrante_id"),
                rs.getString("integrante"), (Long) rs.getObject("tesorero_id", Long.class), rs.getString("tesorero"),
                TipoMovimiento.valueOf(rs.getString("tipo")), rs.getString("descripcion"), dinero(rs, "entrada"),
                dinero(rs, "salida"), (Long) rs.getObject("prestamo_id", Long.class),
                (Long) rs.getObject("actividad_id", Long.class), (Long) rs.getObject("anula_a", Long.class),
                (Long) rs.getObject("anulado_por", Long.class), rs.getString("creado_por"),
                rs.getObject("creado_en", OffsetDateTime.class)));
    }

    private static boolean vacio(Collection<Long> integrantes) {
        return integrantes != null && integrantes.isEmpty();
    }

    private static String filtro(Collection<Long> integrantes, String columna) {
        return integrantes == null ? "" : " AND " + columna + " IN (:integrantes)";
    }

    private static MapSqlParameterSource params(Collection<Long> integrantes) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (integrantes != null) {
            p.addValue("integrantes", integrantes);
        }
        return p;
    }

    private static Dinero dinero(ResultSet rs, String columna) throws SQLException {
        BigDecimal v = rs.getBigDecimal(columna);
        return v == null ? Dinero.CERO : Dinero.de(v);
    }

    private static Dinero dineroNulo(ResultSet rs, String columna) throws SQLException {
        BigDecimal v = rs.getBigDecimal(columna);
        return v == null ? null : Dinero.de(v);
    }

    private static java.time.LocalDate fecha(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
