# Status

Atualizado em: 2026-11-26 · por: Claude

## Demanda em andamento
Preparar o protótipo para a banca (2026-11-27) · nível: reforcado · sem commit (aguardando revisão).

## Estado
Verificado nesta sessão:
- `.\mvnw.cmd test` passa; `npm run build` ok; `npm run lint` com 2 avisos antigos.
- `evento.ps1 -SemTunel`: banca em `/testar` (390 px) até a decisão, alerta confirmado no painel e resposta no celular, sem erro de console. Prints em `..\inovathon-prints`.
- Plano B: quadro "WhatsApp / IA" no menu do painel.

Não verificado: chamadas reais à OpenAI; link público do cloudflared; câmera e microfone em celular de verdade; WhatsApp da Meta.

## Próximo passo
Revogar a chave vazada, pôr a nova no `.env`, testar foto e áudio reais, rodar `evento.ps1` e testar pelo celular. Depois, commit.

## Bloqueios e dúvidas
A chave antiga continua no histórico do GitHub (repositório público) até ser revogada.
