package br.com.fiap.sobraboaapi.doacao;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    @EntityGraph(attributePaths = {"doador", "enderecoRetirada"})
    Page<Doacao> findByDoadorUsuarioId(Long doadorId, Pageable pageable);

    List<Doacao> findByStatusInAndJanelaFimBefore(Collection<StatusDoacao> status, OffsetDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doacao d where d.id = :id")
    Optional<Doacao> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT d.* FROM doacao d
            JOIN endereco e ON e.id = d.endereco_retirada_id
            WHERE d.status = 'DISPONIVEL'
              AND now() BETWEEN d.janela_inicio AND d.janela_fim
              AND e.latitude  BETWEEN :latMin AND :latMax
              AND e.longitude BETWEEN :lngMin AND :lngMax
              AND 2 * 6371 * asin(sqrt(
                    power(sin(radians(e.latitude - :lat) / 2), 2) +
                    cos(radians(:lat)) * cos(radians(e.latitude)) *
                    power(sin(radians(e.longitude - :lng) / 2), 2)
                  )) <= :raioKm
            ORDER BY d.janela_fim ASC
            """,
            countQuery = """
                    SELECT count(*) FROM doacao d
                    JOIN endereco e ON e.id = d.endereco_retirada_id
                    WHERE d.status = 'DISPONIVEL'
                      AND now() BETWEEN d.janela_inicio AND d.janela_fim
                      AND e.latitude  BETWEEN :latMin AND :latMax
                      AND e.longitude BETWEEN :lngMin AND :lngMax
                      AND 2 * 6371 * asin(sqrt(
                            power(sin(radians(e.latitude - :lat) / 2), 2) +
                            cos(radians(:lat)) * cos(radians(e.latitude)) *
                            power(sin(radians(e.longitude - :lng) / 2), 2)
                          )) <= :raioKm
                    """,
            nativeQuery = true)
    Page<Doacao> buscarProximas(@Param("lat") double lat, @Param("lng") double lng,
                                @Param("raioKm") double raioKm,
                                @Param("latMin") double latMin, @Param("latMax") double latMax,
                                @Param("lngMin") double lngMin, @Param("lngMax") double lngMax,
                                Pageable pageable);
}