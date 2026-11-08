package br.com.fiap.sobraboaapi.reserva;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Page<Reserva> findByReceptorUsuarioId(Long receptorId, Pageable pageable);
    List<Reserva> findByStatusAndExpiraEmBefore(StatusReserva status, OffsetDateTime limite);
    boolean existsByDoacaoIdAndReceptorUsuarioId(Long doacaoId, Long receptorId);
}