package br.com.panocerto.servico;

import br.com.panocerto.config.Configuracao;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Guarda fotos e áudios numa pasta do disco. O nome do arquivo é aleatório (UUID). */
@Service
public class ServicoArmazenamento {

    private static final Pattern NOME_VALIDO = Pattern.compile("[a-f0-9\\-]{36}\\.[a-z0-9]{2,5}");

    private static final Map<String, String> EXTENSOES = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"), Map.entry("image/png", "png"), Map.entry("image/webp", "webp"),
            Map.entry("audio/ogg", "ogg"), Map.entry("audio/webm", "webm"), Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/mp4", "m4a"), Map.entry("audio/aac", "aac"), Map.entry("audio/wav", "wav"),
            Map.entry("audio/amr", "amr"));

    private final Path pasta;

    public ServicoArmazenamento(Configuracao config) {
        this.pasta = Path.of(config.pastaUploads()).toAbsolutePath().normalize();
    }

    /** Salva os bytes e devolve o caminho público, ex.: /midias/3f2a....jpg */
    public String salvar(byte[] bytes, String tipoMime) {
        try {
            Files.createDirectories(pasta);
            String nome = UUID.randomUUID() + "." + extensao(tipoMime);
            Files.write(pasta.resolve(nome), bytes);
            return "/midias/" + nome;
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui guardar o arquivo.", e);
        }
    }

    /** Lê um arquivo guardado. Só aceita nomes no formato que nós mesmos geramos. */
    public Optional<byte[]> ler(String nome) {
        if (!NOME_VALIDO.matcher(nome).matches()) {
            return Optional.empty();
        }
        try {
            Path arquivo = pasta.resolve(nome);
            return Files.exists(arquivo) ? Optional.of(Files.readAllBytes(arquivo)) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String tipoPelaExtensao(String nome) {
        String ext = nome.substring(nome.lastIndexOf('.') + 1);
        return EXTENSOES.entrySet().stream().filter(e -> e.getValue().equals(ext))
                .map(Map.Entry::getKey).findFirst().orElse("application/octet-stream");
    }

    private static String extensao(String tipoMime) {
        String limpo = tipoMime == null ? "" : tipoMime.split(";")[0].trim().toLowerCase();
        if (limpo.equals("video/webm")) {
            limpo = "audio/webm";   // alguns navegadores chamam a gravação de áudio de video/webm
        }
        return EXTENSOES.getOrDefault(limpo, "bin");
    }
}
