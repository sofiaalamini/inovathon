# PanoCerto — instruções para agentes

Responda em português do Brasil. Neste projeto, nomes de código também são em português (domínio: Talhao, Produtor, Monitoramento...).

## Comandos (PowerShell)

| Ação | Comando |
|---|---|
| Instalar | `cd frontend; npm install` |
| Rodar local | `cd backend; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"` e `cd frontend; npm run dev` |
| Build | `cd backend; .\mvnw.cmd package` e `cd frontend; npm run build` |
| Testes | `cd backend; .\mvnw.cmd test` |
| Lint | `cd frontend; npm run lint` |

Rode build, lint e testes antes de declarar uma tarefa concluída. Não diga "testado" sem ter executado.

## Convenções

- Stack: Java 21 (compila com `--release 21`), Spring Boot 3.5, Maven, JPA + Flyway; React 19, TypeScript, Vite, Tailwind 4.
- A decisão de aplicar sai **só** de `mip/RegraMip`. A IA nunca decide.
- Código simples, comentários curtos em português; a autora precisa explicar tudo a uma banca.
- Identidade visual: terra roxa só para "aplicar"; sem gradientes e sem emojis.
- Dependência nova só com motivo explícito.

## Onde está cada coisa

- Estado atual: `STATUS.md`. Decisões: nota `Produtos/PanoCerto/Decisões.md` no vault Karen.
- Problema e regras de negócio: vault Karen, `Produtos/PanoCerto/Produto.md`.

## Limites de autonomia

Exigem autorização: merge, deploy, exclusão de dados, mudança de acessos, gastos, segredos. Nunca grave segredos no repositório (use `.env`, que está no `.gitignore`).
