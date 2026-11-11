package br.com.fiap.sobraboaapi.ia;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
class OpenAiSugestaoGateway implements SugestaoGateway {
    private final ChatClient chat;

    OpenAiSugestaoGateway(ChatClient.Builder builder) {
        this.chat = builder.defaultSystem("""
                Você é o assistente do SobraBoa, plataforma que conecta comércios com sobra de alimentos
                a organizações que combatem a fome. Converta a descrição livre do doador em dados
                estruturados de uma doação. Regras: use somente categorias da lista fornecida;
                nunca invente validade (use null se o texto não disser); marque refrigeração
                quando houver prato pronto, laticínio, carne ou peixe; escreva em português do Brasil,
                com tom objetivo; não faça promessas sobre a qualidade do alimento.
                """).build();
    }

    @Override
    public SugestaoDoacao sugerir(String texto, String hoje, String categorias) {
        return chat.prompt()
                .user(u -> u.text("""
                        Data de hoje: {hoje}
                        Categorias permitidas: {categorias}
                        Descrição do doador:
                        {texto}
                        """).param("hoje", hoje).param("categorias", categorias).param("texto", texto))
                .call().entity(SugestaoDoacao.class);
    }
}
