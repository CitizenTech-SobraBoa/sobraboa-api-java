package br.com.fiap.sobraboaapi.comprovante;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ComprovanteResponse(String codigoVerificacao, BigDecimal pesoTotalKg, OffsetDateTime emitidoEm,
                                  Long doacaoId, String tituloDoacao, String doador, String receptor,
                                  String urlPdf) {
}