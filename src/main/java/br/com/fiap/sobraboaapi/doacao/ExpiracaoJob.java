package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.reserva.Reserva;
import br.com.fiap.sobraboaapi.reserva.ReservaRepository;
import br.com.fiap.sobraboaapi.reserva.StatusReserva;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiracaoJob {
    private final ReservaRepository reservaRepo;
    private final DoacaoRepository doacaoRepo;
    private final DoacaoService doacaoService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirar() {
        OffsetDateTime agora = OffsetDateTime.now();

        // 1) reservas ativas vencidas; se a janela da doação ainda está aberta, ela volta a DISPONIVEL
        for (Reserva r : reservaRepo.findByStatusAndExpiraEmBefore(StatusReserva.ATIVA, agora)) {
            r.setStatus(StatusReserva.EXPIRADA);
            Doacao d = r.getDoacao();
            if (d.getStatus() == StatusDoacao.RESERVADA && d.getJanelaFim().isAfter(agora)) {
                doacaoService.mudarStatus(d, StatusDoacao.DISPONIVEL, null, "Reserva expirada (job automático)");
            }
        }

        // 2) doações cuja janela terminou
        for (Doacao d : doacaoRepo.findByStatusInAndJanelaFimBefore(
                List.of(StatusDoacao.DISPONIVEL, StatusDoacao.RESERVADA), agora)) {
            doacaoService.mudarStatus(d, StatusDoacao.EXPIRADA, null, "Janela de retirada encerrada (job automático)");
        }
    }
}
