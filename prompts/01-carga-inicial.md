# Prompt 01 — Carga inicial do Radar Nacional

Atue como pesquisador documental e organizador de dados. Prepare o primeiro arquivo de dados do aplicativo Radar Nacional com base em fontes públicas e verificáveis. O objetivo é formar uma base inicial nacional, priorizando o âmbito federal e declarando claramente os limites de cobertura. Não invente completude.

## Regras

1. Pesquise atos, proposições e decisões em fontes primárias oficiais. Reportagens podem orientar a busca, mas não substituem o documento primário.
2. Trabalhe em lotes de até 20 registros. Registre a data de corte e as fontes efetivamente consultadas. Não marque um item como verificado só porque foi encontrado num índice.
3. Não invente número, autoria, relatoria, partido, data, votação, situação processual, link, trecho ou consequência jurídica.
4. Separe fato documental, interpretação, avaliação jurídica, alegação e pendência. Investigação, acusação, decisão recorrível e condenação definitiva são estados diferentes.
5. Atribua um `id` estável a cada registro. Não duplique a mesma pauta por ter sido encontrada em mais de uma fonte.
6. Avalie os dez eixos NABOTE. Mera menção não é incidência material; explique o nexo e apresente evidência específica.
7. Mantenha independentes aderência, impacto, gravidade, oportunidade midiática, urgência temporal/moral/institucional/midiática e horizonte curto/médio/longo. Não crie média geral sem regra aprovada.
8. Pessoas e instituições devem ser desambiguadas com identificadores públicos e contexto de cargo/período. Não inclua dados privados ou contatos pessoais.
9. Se algo não puder ser verificado, marque como pendente e explique a lacuna. Não substitua desconhecido por zero.

## Primeiro snapshot

Gere um arquivo JSON com:

- `manifest.app_id = "radar-nacional"`;
- `manifest.schema_version = 1`;
- `manifest.package_type = "snapshot"`;
- `manifest.previous_version = 0`;
- `manifest.data_version = 1`;
- `manifest.created_at` em ISO 8601;
- `manifest.checksum_algorithm = "SHA-256"`;
- `manifest.checksum_sha256` calculado de verdade sobre o `payload` canônico.

`payload` deve conter exatamente as coleções `records`, `people`, `institutions`, `events`, `sources`, `monitoring`. Inclua um único estado de monitoramento com `id = "state"` e as listas `source_checkpoints`, `record_states`, `coverage_gaps` e `pending_review`, mesmo que comecem vazias. Não use conteúdo fictício no acervo.

## Formato de avaliação por eixo

Cada item de `axes` deve ter `axis_id` (1–10), `incidence` (`direct`, `indirect` ou `mention`), `score` (0–10, quando justificável), `rationale` e `evidence`. Inclua o efeito interpretado (`FAVORECE`, `RESTRINGE`, `MISTO`, `NEUTRO` ou `INDETERMINADO`) e a confiança quando houver informação suficiente. Cada evento deve conter `id`, `record_id`, `date` e `label`. Cada pessoa, instituição e fonte deve conter `id` e `name`.

## Integridade do arquivo

Canonicalize recursivamente: chaves de objetos em ordem lexicográfica; arrays na ordem original; strings UTF-8; nenhum espaço extra; números inteiros sem casas decimais; NaN/Infinity proibidos. Calcule SHA-256 dos bytes UTF-8 do payload canônico. Se não conseguir calcular o hash, use `null` e identifique o arquivo como não pronto para importação. Não invente checksum.

Entregue o JSON anexável, um relatório com data de corte e cobertura realmente verificada, lacunas conhecidas e contagens por coleção. Valide a estrutura contra `docs/schemas/radar-package.schema.json` quando esse arquivo estiver disponível.
