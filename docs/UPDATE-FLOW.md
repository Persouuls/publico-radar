# Fluxo de dados e atualização

O Radar não pesquisa a internet por conta própria. O ciclo é manual, reproduzível e auditável.

## Pacotes

### Snapshot
Base completa. Usado para primeira carga, restauração ou atualização de aparelhos muito atrasados. `payload` contém arrays `records`, `people`, `institutions`, `events`, `sources`, `monitoring`.

### Workset
Contexto para o ChatGPT; **não é importável**. Contém memória de monitoramento, até 20 registros prioritários, índice compacto do acervo e índices de pessoas, instituições e fontes.

### Delta
Atualização incremental. `payload.operations` contém somente operações `upsert` completas nas tabelas `records`, `people`, `institutions`, `events`, `sources`, `monitoring`. Cada operação tem `op`, `table`, `id`, `data`, com `data.id == id`. Não use operações de exclusão.

## Manifesto

`app_id = "radar-nacional"`; `schema_version = 1`; `package_type`; `previous_version`; `data_version`; `created_at` ISO 8601; `checksum_algorithm = "SHA-256"`; `checksum_sha256`.

Um delta só pode ser aplicado se `previous_version` corresponder à versão instalada e `data_version = previous_version + 1`. O app rejeita worksets, IDs inconsistentes, registros malformados e checksums divergentes.

## Canonicalização SHA-256

O hash é calculado sobre o objeto `payload`, não sobre o manifesto. Canonicalização: chaves de objetos em ordem lexicográfica recursiva; arrays preservam a ordem original; UTF-8; sem espaços extras; números inteiros sem casas decimais; NaN e Infinity proibidos. O aplicativo gera e verifica esse hash. O hash não prova autoria; assinatura digital seria uma etapa futura.

## Fluxo recomendado

1. No app, exporte um workset (ou snapshot para a primeira carga).
2. Anexe-o ao Prompt 01 (carga inicial) ou Prompt 02 (atualização incremental), junto ao schema.
3. Pesquise fontes primárias e diferencie o que foi consultado, o que foi confirmado e o que ficou pendente.
4. Audite o delta com o Prompt 03.
5. Importe o arquivo pelo app. Leia a prévia com atenção antes de confirmar.
6. Guarde o snapshot exportado como backup externo periódico.

## Primeira carga

A base instalada começa vazia, versão 0. O primeiro arquivo deve ser um `snapshot` com `previous_version: 0` e `data_version: 1`, contendo as seis coleções exigidas. O registro `monitoring` pode conter um objeto `id: "state"`, inclusive sem pesquisa concluída, deixando claro que nenhuma fonte foi verificada ainda. Não preencha o acervo com registros de demonstração.

## Regras de validação adicionais do MVP

- O manifesto precisa conter todos os campos obrigatórios e um checksum SHA-256 de 64 caracteres hexadecimais.
- O snapshot precisa conter as seis coleções; `monitoring` deve ter exatamente um objeto com `id = "state"` e listas de checkpoints, estados, lacunas e pendências.
- Pessoas, instituições e fontes usam `id` e `name`; eventos usam `id`, `record_id`, `date` e `label`.
- Eixos aceitam somente `axis_id` de 1 a 10, incidência `direct`, `indirect` ou `mention`, nota de 0 a 10, justificativa e evidência.
- A importação de delta rejeita referências de evento a registros inexistentes, operações repetidas, exclusões e divergência de versão.
- O hash cobre somente `payload`; as chaves dos objetos são ordenadas recursivamente, arrays mantêm a ordem e a codificação é UTF-8 sem espaços extras.
