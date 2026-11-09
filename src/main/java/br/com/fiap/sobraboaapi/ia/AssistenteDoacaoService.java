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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssistenteDoacaoService {

    private final ChatClient chat;
    private final CategoriaAlimentoRepository categoriaRepo;
    private final DoacaoService doacaoService;

    public AssistenteDoacaoService(ChatClient.Builder builder,
                                   CategoriaAlimentoRepository categoriaRepo,
                                   DoacaoService doacaoService) {
        this.categoriaRepo = categoriaRepo;
        this.doacaoService = doacaoService;
        this.chat = builder
                .defaultSystem("""
                        Você é o assistente do SobraBoa, plataforma que conecta comércios com sobra de alimentos
                        a organizações que combatem a fome. Converta a descrição livre do doador em dados
                        estruturados de uma doação. Regras: use somente categorias da lista fornecida;
                        nunca invente validade (use null se o texto não disser); marque refrigeração
                        quando houver prato pronto, laticínio, carne ou peixe; escreva em português do Brasil,
                        com tom objetivo; não faça promessas sobre a qualidade do alimento.
                        """)
                .build();
    }

    public DoacaoResponse criarRascunho(AssistenteDoacaoRequest req, Long doadorId) {
        List<CategoriaAlimento> categorias = categoriaRepo.findAll();
        String nomes = categorias.stream().map(CategoriaAlimento::getNome).collect(Collectors.joining("; "));

        SugestaoDoacao sugestao;
        try {
            sugestao = chat.prompt()
                    .user(u -> u.text("""
                            Data de hoje: {hoje}
                            Categorias permitidas: {categorias}

                            Descrição do doador:
                            {texto}
                            """)
                            .param("hoje", LocalDate.now().toString())
                            .param("categorias", nomes)
                            .param("texto", req.texto()))
                    .call()
                    .entity(SugestaoDoacao.class);
        } catch (Exception e) {
            throw new IaIndisponivelException("O assistente de IA está indisponível no momento. Cadastre a doação manualmente.", e);
        }

        if (sugestao == null || sugestao.itens() == null || sugestao.itens().isEmpty())
            throw new RegraNegocioException("Não foi possível identificar itens no texto. Descreva o que sobrou com mais detalhes.");

        CategoriaAlimento outros = categoriaRepo.findByNomeIgnoreCase("Outros").orElseThrow();
        List<ItemRequest> itens = sugestao.itens().stream().map(i -> new ItemRequest(
                categorias.stream().filter(c -> c.getNome().equalsIgnoreCase(i.categoria()))
                        .findFirst().orElse(outros).getId(),
                i.descricao(),
                i.quantidade() == null || i.quantidade().signum() <= 0 ? BigDecimal.ONE : i.quantidade(),
                unidade(i.unidade()),
                i.pesoEstimadoKg(),
                i.dataValidade(),
                null)).toList();

        DoacaoRequest doacao = new DoacaoRequest(
                limitar(sugestao.titulo(), 150), sugestao.descricao(),
                req.janelaFim(), req.janelaInicio(),
                sugestao.requerRefrigeracao(), limitar(sugestao.instrucoesRetirada(), 500),
                null, null, itens);

        // Cria como RASCUNHO: o doador revisa e publica depois (human-in-the-loop)
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
