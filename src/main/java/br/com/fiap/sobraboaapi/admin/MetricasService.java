package br.com.fiap.sobraboaapi.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Indicadores de impacto (consultas agregadas do MVP, seção 5.6). */
@Service
public class MetricasService {
    private final JdbcClient jdbc;

    public MetricasService(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    @Transactional(readOnly = true)
    public MetricasResponse calcular() {
        Map<String, Long> porStatus = new LinkedHashMap<>();
        jdbc.sql("SELECT status::text AS status, count(*) AS total FROM doacao GROUP BY status ORDER BY status")
                .query((rs, n) -> Map.entry(rs.getString("status"), rs.getLong("total")))
                .list()
                .forEach(e -> porStatus.put(e.getKey(), e.getValue()));

        BigDecimal kg = jdbc.sql("SELECT coalesce(sum(peso_total_kg), 0) FROM comprovante")
                .query(BigDecimal.class).single();
        long doadores = jdbc.sql("SELECT count(*) FROM doador WHERE status_aprovacao = 'APROVADO'")
                .query(Long.class).single();
        long receptores = jdbc.sql("SELECT count(*) FROM receptor WHERE status_aprovacao = 'APROVADO'")
                .query(Long.class).single();

        // taxa de aproveitamento = concluídas / (concluídas + expiradas + canceladas)
        long concluidas = porStatus.getOrDefault("CONCLUIDA", 0L);
        long encerradas = concluidas + porStatus.getOrDefault("EXPIRADA", 0L) + porStatus.getOrDefault("CANCELADA", 0L);
        double taxa = encerradas == 0 ? 0.0
                : BigDecimal.valueOf(concluidas * 100.0 / encerradas).setScale(1, RoundingMode.HALF_UP).doubleValue();

        return new MetricasResponse(porStatus, kg.setScale(2, RoundingMode.HALF_UP), doadores, receptores, taxa);
    }
}