package br.com.fiap.sobraboaapi.doador;

import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoadorRepository extends JpaRepository<Doador, Long> {

    Page<Doador> findByStatusAprovacao(StatusAprovacao status, Pageable pageable);
}
