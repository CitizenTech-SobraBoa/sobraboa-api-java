package br.com.fiap.sobraboaapi.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    boolean existsByEventoIdAndUsuarioId(UUID eventoId, Long usuarioId);

    Page<Notificacao> findByUsuarioIdOrderByCriadaEmDesc(Long usuarioId, Pageable pageable);

    Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);
}