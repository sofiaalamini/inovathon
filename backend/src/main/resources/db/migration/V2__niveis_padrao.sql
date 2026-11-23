-- Níveis de ação do MIP (Embrapa / IDR-Paraná). Valores a confirmar com agrônomo.
insert into nivel_acao (chave, descricao, valor, unidade) values
    ('PERCEVEJO_GRAO', 'Percevejos por pano, soja para grão (a partir do R3)', 2, 'por pano'),
    ('PERCEVEJO_SEMENTE', 'Percevejos por pano, soja para semente (a partir do R3)', 1, 'por pano'),
    ('LAGARTA', 'Lagartas grandes por pano', 20, 'por pano'),
    ('DESFOLHA_VEGETATIVA', 'Desfolha na fase vegetativa (V)', 30, '%'),
    ('DESFOLHA_REPRODUTIVA', 'Desfolha na fase reprodutiva (R)', 15, '%'),
    ('PERTO_PERCENTUAL', 'Percentual do nível a partir do qual o talhão está "perto"', 70, '%');
