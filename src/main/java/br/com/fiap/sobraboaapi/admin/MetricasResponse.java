package br.com.fiap.sobraboaapi.admin;

import java.math.BigDecimal;
import java.util.Map;

public record MetricasResponse(Map<String, Long> doacoesPorStatus, BigDecimal kgDoados,
                               long doadoresAprovados, long receptoresAprovados,
                               double taxaAproveitamento) {
}