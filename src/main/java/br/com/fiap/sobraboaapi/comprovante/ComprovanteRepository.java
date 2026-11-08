package br.com.fiap.sobraboaapi.comprovante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprovanteRepository extends JpaRepository<Comprovante, Long> {

    Optional<Comprovante> findByReservaId(Long reservaId);
}