-- Visitantes da página da banca (/testar) não entram nos indicadores gerais.
alter table produtor add column visitante boolean default false not null;
-- Desfolha que veio junto com a contagem, guardada enquanto o produtor confirma os números.
alter table conversa_estado add column pendente_desfolha int;
