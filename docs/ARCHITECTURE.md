# Arquitetura

## Princípios

- **Local-first / offline-first:** o Room/SQLite do aparelho é a fonte de verdade. A aplicação não precisa de conexão para ler, buscar, filtrar ou consultar o acervo.
- **Sem backend obrigatório:** não há conta, sincronização automática, telemetria, anúncios, chamadas a IA, scraping nem agente em segundo plano.
- **Importação deliberada:** dados entram somente por arquivo selecionado pelo usuário. O app confere manifesto, app ID, versão, tipo de pacote e SHA-256, apresenta uma prévia e exige confirmação.
- **Fato separado de interpretação:** a estrutura mantém situação factual, evidência, fonte e avaliação analítica em campos distintos.
- **Histórico conservado:** alterações incrementais são upserts completos. A remoção de fatos antigos não é o mecanismo para representar arquivamento, revogação ou substituição.

## Componentes

`ui/` contém as telas Compose e o ViewModel. `data/Database.kt` define entidades Room, DAO e a base local. `data/PackageEngine.kt` concentra parsing, canonicalização, SHA-256, validação estrutural e conversão de entidades. `data/RadarRepository.kt` coordena consultas, backup, exportação e importação transacional.

Fluxo de estado: Compose → `RadarViewModel` → `RadarRepository` → Room. O ViewModel expõe `Flow` como `StateFlow`; a interface não consulta a rede nem conhece SQL.

## Armazenamento

- `records`: documentos, pautas e avaliações aninhadas;
- `people`, `institutions`, `events`, `sources`: cadastros vinculados;
- `monitoring`: checkpoints, memória de cobertura e pendências;
- `collection_meta`: versão instalada do acervo;
- `operation_log`: auditoria local das importações.

A base começa sem registros, pessoas, instituições, eventos ou fontes. A meta de versão é criada na primeira importação válida. O workset mantém um índice dos registros ativos e arquivados para deduplicação; a seleção de revisão prioriza pendências e registros ativos. Os arquivados continuam no snapshot e podem ser consultados pelo filtro avançado do Radar.

## Importação segura

1. Selecionar JSON pelo Android Storage Access Framework.
2. Ler sem permissões amplas de armazenamento.
3. Conferir app ID, schema, tipo, checksum e versão anterior.
4. Mostrar contagem de registros e operações e eventuais erros.
5. Gravar backup portátil em `filesDir/backups/`.
6. Aplicar snapshot/delta dentro de `RoomDatabase.withTransaction`.
7. Atualizar versão e log na mesma transação.

Falha durante uma operação aborta a transação. Snapshots substituem as coleções do acervo, mas não apagam o log de operações. O app preserva a preferência local `isSaved` para registros cujo ID continue presente no snapshot; um registro que desapareça de um snapshot completo não pode manter seu marcador local nessa versão.

## Limites honestos

- O SHA-256 garante que o payload importado corresponde ao checksum declarado; não autentica o autor do pacote.
- A validação implementada no MVP é estrutural em Kotlin. O JSON Schema é entregue como contrato de geração/verificação externa; não é uma alegação de validação completa por engine JSON Schema dentro do app.
- Não existe coleta automática nas fontes públicas. A pesquisa é manual, com prompts/worksets e revisão humana fora do aplicativo.
- A arquitetura poderá receber uma sincronização central ou módulo desktop posteriormente sem trocar o contrato base de pacotes.
