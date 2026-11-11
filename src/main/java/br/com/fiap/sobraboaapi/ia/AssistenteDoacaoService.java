package br.com.fiap.sobraboaapi.ia;

import br.com.fiap.sobraboaapi.categoria.CategoriaAlimento;
import br.com.fiap.sobraboaapi.categoria.CategoriaAlimentoRepository;
import br.com.fiap.sobraboaapi.doacao.DoacaoRequest;
import br.com.fiap.sobraboaapi.doacao.DoacaoResponse;
import br.com.fiap.sobraboaapi.doacao.DoacaoService;
import br.com.fiap.sobraboaapi.doacaoitem.ItemRequest;
import br.com.fiap.sobraboaapi.doacaoitem.UnidadeMedida;
import br.com.fiap.sobraboaapi.exception.IaIndisponivelException;
import br.com.fiap.sobraboaapi.exception.RegraNegocioException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssistenteDoacaoService {

    private final CategoriaAlimentoRepository categoriaRepo;
    private final DoacaoService doacaoService;
    private final Validator validator;
    private final SugestaoGateway gateway;

    public AssistenteDoacaoService(CategoriaAlimentoRepository categoriaRepo,
                                   DoacaoService doacaoService,
                                   Validator validator,
                                   SugestaoGateway gateway) {
        this.categoriaRepo = categoriaRepo;
        this.doacaoService = doacaoService;
        this.validator = validator;
        this.gateway = gateway;
    }

    public DoacaoResponse criarRascunho(AssistenteDoacaoRequest req, Long doadorId) {
        List<CategoriaAlimento> categorias = categoriaRepo.findAll();
        String nomes = categorias.stream().map(CategoriaAlimento::getNome).collect(Collectors.joining("; "));

        SugestaoDoacao sugestao;
        try {
            sugestao = gateway.sugerir(req.texto(), LocalDate.now().toString(), nomes);
        } catch (Exception e) {
            throw new IaIndisponivelException("O assistente de IA está indisponível no momento. Cadastre a doação manualmente.", e);
        }

        if (sugestao == null || sugestao.itens() == null || sugestao.itens().isEmpty())
            throw new RegraNegocioException("Não foi possível identificar itens no texto. Descreva o que sobrou com mais detalhes.");

        CategoriaAlimento outros = categoriaRepo.findByNomeIgnoreCase("Outros").orElseThrow();
        List<ItemRequest> itens = sugestao.itens().stream().map(i -> new ItemRequest(
                categorias.stream().filter(c -> c.getNome().equalsIgnoreCase(i.categoria()))
                        .findFirst().orElse(outros).getId(),
                i.descricao() == null || i.descricao().isBlank()
                        ? "Item sem descrição" : limitar(i.descricao(), 200),     // passo 2
                i.quantidade() == null || i.quantidade().signum() <= 0 ? BigDecimal.ONE : i.quantidade(),
                unidade(i.unidade()),
                i.pesoEstimadoKg(),
                i.dataValidade(),
                null)).toList();

        String titulo = (sugestao.titulo() == null || sugestao.titulo().isBlank())   // passo 3
                ? limitar(req.texto().trim(), 60)
                : sugestao.titulo();

        DoacaoRequest doacao = new DoacaoRequest(
                limitar(titulo, 150), sugestao.descricao(),
                req.janelaFim(), req.janelaInicio(),
                sugestao.requerRefrigeracao(), limitar(sugestao.instrucoesRetirada(), 500),
                null, null, itens);

        if (!validator.validate(doacao).isEmpty())                                   // passo 4
            throw new RegraNegocioException(
                    "A IA não conseguiu montar uma doação válida. Descreva melhor ou cadastre manualmente.");

        return doacaoService.criar(doacao, doadorId);
    }

    private UnidadeMedida unidade(String s) {
        try { return UnidadeMedida.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return UnidadeMedida.UN; }
    }

    private String limitar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
