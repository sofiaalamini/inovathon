package br.com.panocerto.controle;

import br.com.panocerto.controle.Dtos.ProdutorDto;
import br.com.panocerto.modelo.Produtor;
import br.com.panocerto.repositorio.RepositorioCooperativa;
import br.com.panocerto.repositorio.RepositorioProdutor;
import br.com.panocerto.repositorio.RepositorioTalhao;
import br.com.panocerto.servico.Acesso;
import br.com.panocerto.servico.ServicoCadastro;
import br.com.panocerto.servico.ServicoPainel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtores")
public class ProdutorControle {

    public record ProdutorPedido(@NotBlank(message = "Informe o nome.") @Size(max = 120, message = "Nome grande demais.") String nome,
                                 @NotBlank(message = "Informe o telefone.") String telefone) {
    }

    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioCooperativa cooperativas;
    private final ServicoCadastro cadastro;
    private final ServicoPainel painel;
    private final Acesso acesso;

    public ProdutorControle(RepositorioProdutor produtores, RepositorioTalhao talhoes, RepositorioCooperativa cooperativas,
                            ServicoCadastro cadastro, ServicoPainel painel, Acesso acesso) {
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.cooperativas = cooperativas;
        this.cadastro = cadastro;
        this.painel = painel;
        this.acesso = acesso;
    }

    @GetMapping
    public List<ProdutorDto> listar() {
        return produtores.findByCooperativaIdOrderByNome(acesso.cooperativaId()).stream().map(this::paraDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutorDto criar(@Valid @RequestBody ProdutorPedido pedido) {
        var cooperativa = cooperativas.getReferenceById(acesso.cooperativaId());
        return paraDto(cadastro.criarProdutor(cooperativa, pedido.nome(), pedido.telefone()));
    }

    @PutMapping("/{id}")
    public ProdutorDto atualizar(@PathVariable Long id, @Valid @RequestBody ProdutorPedido pedido) {
        return paraDto(cadastro.atualizarProdutor(acesso.produtor(id), pedido.nome(), pedido.telefone()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        cadastro.excluirProdutor(acesso.produtor(id));
    }

    private ProdutorDto paraDto(Produtor p) {
        return new ProdutorDto(p.getId(), p.getNome(), p.getTelefone(),
                talhoes.findByProdutorIdOrderByNome(p.getId()).stream().map(painel::resumo).toList(), p.isVisitante());
    }
}
