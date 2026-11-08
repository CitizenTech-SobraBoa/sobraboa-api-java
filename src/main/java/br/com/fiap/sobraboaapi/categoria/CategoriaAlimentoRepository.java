package br.com.fiap.sobraboaapi.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaAlimentoRepository extends JpaRepository<CategoriaAlimento, Short> {

    Optional<CategoriaAlimento> findByNomeIgnoreCase(String nome);

}
