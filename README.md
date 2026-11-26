# PanoCerto

Monitoramento de pragas da soja pelo WhatsApp.

O produtor faz o **pano de batida**: estende um pano branco entre duas fileiras, sacode as plantas, conta os percevejos e as lagartas que caíram e estima a desfolha (% da folha comida). Depois manda uma **foto, um áudio ou um texto** para o WhatsApp do PanoCerto. O sistema entende os números, aplica a **regra do MIP** (Manejo Integrado de Pragas, Embrapa/IDR-Paraná) e responde se precisa aplicar inseticida. O técnico da cooperativa acompanha tudo num painel web.

> **A IA só conta insetos na foto, transcreve o áudio e extrai números do texto.** A decisão de aplicar vem sempre da classe `RegraMip`, em código determinístico com testes.

## Partes

| Pasta | O que é |
|---|---|
| `backend/` | Java 21 + Spring Boot 3 (API, robô de WhatsApp, regra do MIP, lembretes) |
| `frontend/` | Painel da cooperativa: React + TypeScript + Vite + Tailwind + Recharts |
| `docker-compose.yml` | Banco PostgreSQL |
| `.env.example` | Modelo das chaves e configurações |

## Pré-requisitos (Windows)

- JDK 21 ou mais novo (testado com o 25), Node.js 20+.
- Docker Desktop **só** se for usar PostgreSQL. Sem Docker, use o perfil `dev` (H2 em arquivo).

## Como rodar (PowerShell)

### 1. Sem Docker (mais simples)

```powershell
# terminal 1: backend
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# terminal 2: painel
cd frontend
npm install
npm run dev
```

Abra **http://localhost:5173**.

### 2. Com PostgreSQL

```powershell
docker compose up -d          # sobe o banco
cd backend
.\mvnw.cmd spring-boot:run    # sem perfil: usa o PostgreSQL do .env
```

e o painel como acima (`npm run dev`).

### Login de exemplo

- E-mail: `tecnico@panocerto.com`
- Senha: `123456`

Ao subir com o banco vazio, o sistema cria dados **fictícios**: 1 cooperativa, 5 produtores (um deles o Valdir), 10 talhões em estádios variados e 6 semanas de monitoramento. O painel já abre com alertas, gráfico e economia.

### Testes

```powershell
cd backend
.\mvnw.cmd test       # RegraMip, parser de texto, webhook, fluxo completo pelo simulador, API do painel
cd ..\frontend
npm run build         # confere os tipos e gera o painel
```

## Testar pelo simulador, sem nenhuma chave

1. Suba backend e painel e entre com o login de exemplo.
2. Abra **Simulador de WhatsApp** e escolha o **Valdir** (ele tem 3 talhões).
3. Mande `2 percevejos 3 lagartas 10%`. O robô pergunta o talhão (botões): toque em **Sede**.
4. Mande o ponto 2 por **foto** (botão Foto, qualquer imagem; a contagem é fictícia no modo demo) e toque em **10%**.
5. Mande o ponto 3 por **áudio** (botão Gravar, ou Arquivo). Responda a desfolha.
6. Com 3 pontos o robô dá a decisão. Se der **ACIMA**, aparece um alerta em **Alertas**, onde você clica **Confirmar aplicação** ou **Revisitar**; o produtor recebe a resposta na conversa.
7. Digite `apliquei` (num talhão que não precisava o robô avisa) ou `resultado`, `novo`, `trocar`, `ajuda`.

O simulador passa pelo **mesmo fluxo** (`ServicoRobo`) do webhook real.

## Dia da apresentação (banca testando no celular)

1. No `.env`, preencha `OPENAI_API_KEY` (sem ela, foto e áudio são fictícios).
2. Rode `powershell -ExecutionPolicy Bypass -File .\evento.ps1`.
3. Abra o endereço `/convite` que o script mostra e projete o QR code.

Cada pessoa que abre `/testar` vira um produtor fictício com dois talhões; os alertas dela aparecem no painel.
**Limpar testes:** no Início do painel (só no modo banca) apaga os visitantes e recria os dados de exemplo. Use antes de subir ao palco.

**Plano B:** no painel, **segure o logo PanoCerto por 1 segundo** (celular ou computador). Um ponto dourado pequeno
no logo mostra que está ligado; segure de novo para desligar. Foto e áudio passam a usar respostas prontas,
avisadas na conversa. Se a IA demorar mais de 9 s ou falhar, só aquela mensagem cai na demonstração.
No áudio em demonstração, o robô usa o que o navegador entendeu da fala e pergunta "Está certo?".

Limites: 60 visitantes e 80 mensagens por visitante. O link muda a cada vez que o script roda.

## Como o sistema escolhe o modo

| Se no `.env`... | O sistema usa |
|---|---|
| faltam `WHATSAPP_TOKEN` ou `WHATSAPP_PHONE_NUMBER_ID` | simulador de WhatsApp |
| faltam `ANTHROPIC_API_KEY` e `OPENAI_API_KEY` | contagem de foto em **modo demo** e texto lido só pelo parser |
| falta `OPENAI_API_KEY` | transcrição de áudio em **modo demo** |

O modo atual aparece no canto do menu do painel. Importante: com o WhatsApp **real** ligado e sem as chaves de IA, o robô **não inventa números**: ele pede que o produtor mande por texto.

## Regra do MIP

- Mínimo de **3 pontos** por monitoramento; a decisão usa a **média**.
- Percevejo: nível 2 por pano (grão) ou 1 (semente), **só a partir do R3**.
- Lagarta: 20 lagartas grandes por pano.
- Desfolha: 30% (fase V) ou 15% (fase R).
- **ACIMA** (média ≥ nível): "Aplique só neste talhão, com produto seletivo." e alerta ao técnico.
- **PERTO** (≥ 70% do nível): "Não aplique ainda. Volte a bater o pano em 3 dias."
- **ABAIXO**: "Não precisa aplicar. Volte a monitorar em 7 dias."

Os níveis ficam no banco e são editáveis na tela **Regras**. **Valores a confirmar com agrônomo.**

## Conectar o WhatsApp real (Meta)

1. Entre em **developers.facebook.com**, clique em **Meus apps > Criar app** e escolha o tipo **Empresa**.
2. No app, adicione o produto **WhatsApp**. Em **WhatsApp > Configuração da API** a Meta mostra um **número de teste**, o **ID do número de telefone** (`WHATSAPP_PHONE_NUMBER_ID`) e um **token temporário** (`WHATSAPP_TOKEN`, vale 24 h; para uso contínuo crie um token permanente de usuário do sistema).
3. Em **Configurações do app > Básico**, copie a **Chave secreta do app** para `WHATSAPP_APP_SECRET` (é com ela que o backend confere a assinatura `X-Hub-Signature-256`).
4. Em **Configuração da API**, adicione seu número pessoal como destinatário de teste e confirme o código recebido.
5. Escolha uma frase para `WHATSAPP_VERIFY_TOKEN` e coloque no `.env`. Reinicie o backend.
6. **Cadastre seu número como produtor** no painel (formato `+55 45 99999-0001`). Número desconhecido recebe o convite para se cadastrar.

### Expor o webhook com ngrok

```powershell
ngrok http 8080
```

Copie o endereço `https://xxxx.ngrok-free.app`. No app da Meta vá em **WhatsApp > Configuração > Webhook > Editar** e informe:

- **URL de callback:** `https://xxxx.ngrok-free.app/webhook/whatsapp`
- **Token de verificação:** o mesmo `WHATSAPP_VERIFY_TOKEN`

Clique em verificar e depois assine o campo **messages**. Mande uma mensagem do seu celular para o número de teste.

### Criar o template do lembrete

O lembrete semanal (todo dia às 7h, fuso America/Sao_Paulo, para quem está há 7+ dias sem monitorar) usa texto livre dentro da janela de 24 h e **template aprovado** fora dela.

1. No **WhatsApp Manager > Modelos de mensagem > Criar modelo**.
2. Categoria **Utilidade**, idioma **Português (BR)**, nome igual ao `WHATSAPP_TEMPLATE_LEMBRETE` (padrão `lembrete_pano`).
3. Corpo: `Olá, {{1}}! Faz {{2}} dias do último pano no talhão {{3}}.`
4. Envie para aprovação. Os parâmetros são enviados nesta ordem: nome, dias, talhão.

## Estrutura do código

```
backend/src/main/java/br/com/panocerto/
  mip/          RegraMip e Estadio (a decisão; sem Spring nem banco)
  robo/         ServicoRobo (conversa), ParserTexto (regex), MensagemEntrada
  whatsapp/     MensageiroWhatsApp (interface), MensageiroCloudApi, MensageiroSimulador, webhook
  ia/           ServicoIa, ClienteClaude, ClienteWhisper, IaDemo
  servico/      monitoramento, alertas, lembrete, cadastro, painel, níveis
  controle/     API REST do painel e do webhook
  seguranca/    JWT e Spring Security
  modelo/ repositorio/ config/
frontend/src/   paginas/ (uma por tela), componentes/, api.ts
```

## Limites e pendências

- **Níveis do MIP**: confirmar com agrônomo antes de uso real.
- **Template do lembrete**: precisa de aprovação da Meta.
- "Aplicações evitadas" compara o calendário fixo de 3,6 por safra com as feitas até agora (por isso é alto no começo da safra).
- Respostas do técnico a alertas (confirmar/revisitar) são texto livre: se o produtor não escreveu nas últimas 24 h, a Meta pode recusar.
- Integração real com Meta, Claude e Whisper foi escrita conforme a documentação, mas **não foi testada com chaves reais**.
- O `docker-compose.yml` (PostgreSQL) não foi executado: Docker não estava instalado na máquina de desenvolvimento. O perfil `dev` (H2) foi testado.
