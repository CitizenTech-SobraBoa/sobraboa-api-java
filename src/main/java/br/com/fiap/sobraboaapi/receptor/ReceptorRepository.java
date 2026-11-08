package br.com.fiap.sobraboaapi.receptor;

import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceptorRepository extends JpaRepository<Receptor, Long> {

    Page<Receptor> findByStatusAprovacao(StatusAprovacao status, Pageable pageable);
}
