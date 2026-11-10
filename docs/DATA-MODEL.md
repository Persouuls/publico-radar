# Modelo de dados

## Taxonomia NABOTE fixa

| ID | Eixo |
|---|---|
| 1 | Proteção da vida desde a concepção |
| 2 | Liberdade de expressão e oposição à censura |
| 3 | Defesa da propriedade privada e da segurança jurídica |
| 4 | Liberdade econômica, contratual e desburocratização |
| 5 | Direito às armas e legítima defesa |
| 6 | Primazia dos pais, homeschooling e liberdade educacional |
| 7 | Liberdade religiosa, de proselitismo e de crítica |
| 8 | Liberdade de consciência médica e consentimento informado |
| 9 | Segurança pública, proteção da vítima e aplicação efetiva da lei |
| 10 | Igualdade perante a lei, sem privilégios por sexo, raça ou identidade |

`axis_id` deve sempre apontar para uma destas dez posições. Sinônimos e descritores podem expandir a pesquisa, mas não criam novos eixos.

## Entidades

- **records:** proposições, leis, decretos, atos, decisões judiciais, eventos ou outros documentos institucionais. Inclui fatos básicos, situação, aderência, impacto, gravidade, oportunidade midiática, urgências, horizonte, eixos, vínculos e evidências.
- **people:** cadastro único de atores públicos com identificador estável. O papel é ligado ao registro, e não inferido do partido.
- **institutions:** órgãos, casas legislativas, tribunais e instituições.
- **events:** marcos cronológicos ligados ao registro por `record_id`.
- **sources:** fonte e metadados de publicação/verificação.
- **monitoring:** exatamente um registro principal de memória (`id = "state"`), com checkpoints, estados de verificação, lacunas e pendências.
- **collection_meta:** versão local do acervo.
- **operation_log:** histórico das importações realizadas no aparelho.

## Regras de classificação

- Incidência de eixo: `direct`, `indirect` ou `mention`. Menção não equivale a efeito nem pontua aderência por si só.
- Efeito pode ser `FAVORECE`, `RESTRINGE`, `MISTO`, `NEUTRO` ou `INDETERMINADO`, sempre sustentado por evidência.
- Aderência é 0–10; impacto, gravidade e oportunidade midiática são ordinais independentes (`low`, `medium`, `high`).
- Urgência guarda quatro dimensões independentes: `temporal`, `moral`, `institutional`, `media`.
- Horizonte `short`, `medium`, `long` não é uma média automática de gravidade.
- Estado factual: `confirmed`, `pending_review` ou `disputed`. Acusação, investigação e decisão não definitiva não podem ser apresentadas como condenação definitiva.

## Campos de registro compatíveis com o pacote

Campos fundamentais: `id`, `type`, `title`, `summary`. Campos recomendados: `official_id`, `jurisdiction`, `institution`, `status`, `phase`, `dates`, `actors`, `axes`, `adherence`, `impact`, `severity`, `media_opportunity`, `urgency`, `horizon`, `sources`, `events`, `factual_status`, `confidence`, `reasons`, `last_checked`, `created_at`, `updated_at`.
