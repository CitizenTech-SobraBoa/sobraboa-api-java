package br.com.fiap.sobraboaapi.historico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoacaoStatusHistoricoRepository extends JpaRepository<DoacaoStatusHistorico, Long> {

    List<DoacaoStatusHistorico> findByDoacaoIdOrderByAlteradoEmAsc(Long doacaoId);
}

