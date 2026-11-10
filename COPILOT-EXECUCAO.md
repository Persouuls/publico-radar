# Instruções de execução — Radar Nacional Android

Você é o agente de implementação responsável por concluir o MVP do Radar Nacional no repositório `Persouuls/publico-radar`.

## Objetivo da tarefa

Entregar um aplicativo Android nativo, offline-first e local-first, com banco Room vazio na primeira instalação, interface completa e fluxo manual de importação/exportação de pacotes JSON. Trabalhe numa branch própria e abra um Pull Request para revisão. **Não faça commits diretamente em `main`.**

## 1. Primeiro: examine o repositório e o pacote-base

1. Confira a branch atual, a árvore de arquivos e o estado do repositório.
2. O repositório pode conter `radar-nacional-android-com-instrucoes.zip` (ou `radar-nacional-android.zip`), enviado pelo administrador. Se existir, execute `unzip -l <arquivo>` primeiro; depois extraia para uma pasta temporária, confira a árvore e copie o conteúdo de `radar-nacional-android/` para a raiz do repositório, preservando arquivos ocultos (`cp -a /tmp/extract/radar-nacional-android/. .`). Não deixe o projeto aninhado numa subpasta e remova o ZIP de transferência da branch de entrega.
3. O ZIP contém uma implementação de partida produzida para este projeto: Kotlin, Compose, Material 3, Room, telas, motor de pacotes, schema, prompts, testes e workflow. Use-a como **baseline para revisão**, não como código automaticamente correto.
4. Preserve os requisitos deste documento. Corrija, complete ou substitua o que estiver errado; não descarte a implementação e recomece sem motivo técnico.
5. Se o ZIP não estiver presente, construa o projeto do zero com os requisitos abaixo.
6. Antes de modificar, apresente um plano curto e depois prossiga sem pedir confirmação para decisões técnicas que já estejam definidas aqui.

## 2. Produto e limites

O Radar Nacional organiza pautas legislativas, jurídicas e institucionais brasileiras (proposições, normas, atos administrativos, decisões judiciais e eventos documentados). É ferramenta de organização e análise humana.

Separar visual e estruturalmente:
- fato documentado;
- interpretação/avaliação analítica;
- avaliação jurídica, quando houver;
- alegação ou informação pendente de confirmação.

Não inferir intenção, culpa ou consequência jurídica sem evidência. Não converter investigação, acusação ou notícia em condenação/fato provado. Preservar histórico de tramitação, revogação, anulação, recurso e arquivamento.

Restrições obrigatórias:
- sem backend/servidor, login, banco em nuvem ou sincronização entre aparelhos;
- sem scraping, rastreamento, agente permanente, consulta automática em segundo plano ou chamadas a IA;
- sem API paga obrigatória, anúncios, telemetria ou custos recorrentes;
- não solicitar permissão `INTERNET` na primeira versão;
- funcionar sem conexão;
- guardar o acervo no aparelho;
- banco de produção começa vazio;
- nunca misturar amostras fictícias com dados reais. Fixtures de teste devem ficar isoladas e marcadas inequivocamente como `DEMO`.

## 3. Plataforma e compatibilidade

- Kotlin + Jetpack Compose + Material 3.
- Room/SQLite.
- Kotlinx Serialization para JSON.
- Android Storage Access Framework para importar e exportar.
- MVVM simples, com separação clara entre `ui`, `domain` e `data` apenas onde trouxer benefício real.
- `applicationId = br.com.perpetuus.radar`.
- `minSdk = 26`.
- `compileSdk` e `targetSdk` em versões estáveis compatíveis com o SDK disponível no runner.
- JDK 17 ou a versão exigida oficialmente pela combinação adotada.
- Fixar todas as versões; não usar `+`, preview, nightly ou snapshots.
- Usar Gradle Wrapper **oficial**, completo e verificável (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`). Se o pacote-base trouxer wrapper customizado ou não oficial, substitua-o pelo wrapper oficial gerado com Gradle.
- Antes de escolher versões, consulte as matrizes oficiais de compatibilidade AGP/Gradle/Kotlin e confirme a compatibilidade real no build. Não escolha números apenas por parecerem recentes.

## 4. Taxonomia NABOTE fixa

Usar exatamente dez eixos, IDs estáveis `NABOTE_01` a `NABOTE_10`:

1. Proteção da vida desde a concepção.
2. Liberdade de expressão e oposição à censura.
3. Defesa da propriedade privada e da segurança jurídica.
4. Liberdade econômica, contratual e desburocratização.
5. Direito às armas e legítima defesa.
6. Primazia dos pais, homeschooling e liberdade educacional.
7. Liberdade religiosa, de proselitismo e de crítica.
8. Liberdade de consciência médica e consentimento informado.
9. Segurança pública, proteção da vítima e aplicação efetiva da lei.
10. Igualdade perante a lei, sem privilégios por sexo, raça ou identidade.

Cada classificação deve distinguir incidência direta, indireta, mera menção e ausência de evidência. Mera menção não pontua aderência. Sinônimos são termos de busca, não eixos extras.

Métricas independentes:
- aderência 0–10;
- impacto/relevância pública;
- gravidade;
- oportunidade midiática;
- urgência temporal, moral, institucional e midiática, separadamente;
- horizonte `short`, `medium` ou `long`, com justificativa.

Não reduzir dimensões diferentes a uma nota média opaca. Toda prioridade deve ser explicável.

## 5. Interface e navegação

Telas mínimas:
1. Início: indicadores zerados, atualizações importadas, prioridades e atalhos.
2. Radar: lista pesquisável, filtros combináveis e ordenação.
3. Detalhe: fatos, situação, texto/trechos, fontes, evidências, avaliação por eixo, métricas, atores e histórico.
4. Salvos.
5. Agenda: marcos documentados em ordem cronológica.
6. Pessoas e instituições: catálogo e perfis com vínculos/papéis documentados.
7. Fontes: catálogo, URLs e datas de consulta.
8. Dados/Atualizações: exportar workset, snapshot e schema; importar delta/snapshot; backup; versões e histórico de operações.

Busca local por título, identificador, número/ano, ementa, ator e fonte. Filtros por eixo, tipo, esfera, situação, efeito interpretado, horizonte, urgência e status ativo/arquivado. Filtros avançados recolhidos por padrão. Salvos e preferências devem ficar separados do acervo importado.

### Padrão visual

Interface elegante, sóbria, profissional e contemporânea, de baixa carga cognitiva:
- hierarquia tipográfica clara e leitura rápida;
- espaçamento consistente e áreas de toque adequadas;
- paleta neutra e cores semânticas moderadas;
- evitar excesso de cards, badges, gradientes, cores partidárias e animações;
- tema claro/escuro;
- placeholders vazios com dimensões realistas, indicadores zerados e mensagens que expliquem o estado vazio;
- não inserir documentos ou atores fictícios na base real;
- estados de carregamento, vazio, sucesso, erro e confirmação claros;
- acessibilidade básica e suporte a diferentes telas Android.

## 6. Contrato de dados

Criar um JSON Schema versionado e manter o contrato sincronizado entre app, docs e prompts.

Manifesto comum:
- `app_id = "radar-nacional"`;
- `schema_version = 1`;
- `checksum_algorithm = "SHA-256"`;
- `checksum_sha256` calculado de verdade sobre o `payload` canônico.

Coleções persistentes: `records`, `people`, `institutions`, `events`, `sources`, `monitoring`.

Registro principal de monitoramento: `id = "state"`, com `last_run_at`, `source_checkpoints`, `record_states`, `coverage_gaps`, `pending_review` e dados de continuidade.

### Snapshot
- `package_type = "snapshot"`;
- primeira carga: `previous_version = 0`, `data_version = 1`;
- substitui o acervo somente após validação e confirmação explícita;
- gerar backup automático antes de substituir.

### Delta
- `package_type = "delta"`;
- `previous_version` precisa corresponder exatamente à versão local;
- `data_version = previous_version + 1`;
- `payload.operations` contém operações completas `upsert` sobre `records`, `people`, `institutions`, `events`, `sources` e `monitoring`;
- evitar `delete`; arquivar logicamente e preservar histórico.

### Workset
- `package_type = "workset"`; nunca importável como snapshot/delta;
- exportar memória de monitoramento, índice compacto de todos os registros, índices de pessoas/instituições/fontes e lote de até 20 registros completos para revisão;
- priorizar pendências e verificações vencidas; registros ativos antes dos arquivados no lote;
- índice compacto preserva IDs arquivados para deduplicação/histórico;
- não marcar registro verificado apenas porque aparece no índice.

### Integridade e segurança
- SHA-256 calculado sobre o `payload` canônico;
- canonicalização recursiva: chaves de objetos em ordem lexicográfica, arrays preservam ordem, UTF-8, sem espaços extras, floats integrais normalizados e NaN/Infinity proibidos;
- conferir `app_id`, schema, versão-base, versão nova, checksum, IDs únicos e referências cruzadas;
- exibir prévia antes da confirmação;
- aplicar todas as operações numa única transação Room: ou tudo ou nada;
- backup automático antes da importação;
- pacote inválido nunca altera o banco;
- registrar operação, data/hora, resultado e erro;
- explicar que SHA-256 detecta corrupção, mas não comprova autoria.

Entidades mínimas:
- `records`: id, type, official_id, title, summary, jurisdiction, institution/institution_id, status, phase, dates, actors, axes, adherence, impact, severity, media_opportunity, urgency, horizon, factual_status, sources, events, timestamps e observações.
- `people` e `institutions`: IDs estáveis e papéis específicos por vínculo; uma pessoa pode estar ligada a vários registros. Partido/cargo apenas quando documentados e com período.
- `sources`: nome, categoria, instituição, URL, natureza primária/secundária, data de publicação e consulta.
- `events`: ID, record_id, data, rótulo, descrição, órgão e fonte.

## 7. Testes obrigatórios

Criar testes unitários para:
- serialização/desserialização de snapshot, delta e workset;
- canonicalização e SHA-256;
- validação de app_id/schema/tipo de pacote;
- versão-base divergente;
- IDs duplicados e relações quebradas;
- importação transacional e rollback;
- seleção/priorização do workset;
- filtros e cálculos determinísticos.

Executar no ambiente real do runner:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Corrigir todos os erros relevantes; não ignorar falhas, desabilitar testes ou declarar sucesso sem execução. Se a rede ou SDK impedir o build, registrar exatamente o bloqueio e não alegar que o APK foi gerado.

## 8. GitHub Actions e APK

Criar/ajustar workflow para `push`, `pull_request` e `workflow_dispatch`:
- configurar JDK e Android SDK compatíveis;
- usar actions oficiais estáveis e versões fixadas;
- executar testes, lint e `assembleDebug`;
- publicar `app/build/outputs/apk/debug/app-debug.apk` como artifact apenas após build bem-sucedido;
- nunca expor segredos nem guardar keystore no repositório.

O APK debug é para teste. Documentar assinatura release separadamente, para uma etapa futura.

## 9. Documentação

Criar ou revisar:
- `README.md`;
- `docs/ARCHITECTURE.md`;
- `docs/DATA-MODEL.md`;
- `docs/UPDATE-FLOW.md`;
- `docs/schemas/radar-package.schema.json`;
- `prompts/01-carga-inicial.md`;
- `prompts/02-atualizacao-incremental.md`;
- `prompts/03-auditoria-do-pacote.md`;
- exemplos DEMO isolados, marcados como fictícios;
- instruções para baixar o artifact APK e instalar no Android.

## 10. Critérios de aceite

1. Projeto Android nativo completo e Gradle Wrapper oficial.
2. Build verde no GitHub Actions e APK debug publicado como artifact.
3. Banco de produção vazio na primeira instalação.
4. Navegação, busca, filtros e detalhe funcionais.
5. Persistência Room após fechar/reabrir app.
6. Exportação de workset e snapshot.
7. Prévia, validação e importação de delta/snapshot.
8. SHA-256, versão-base, backup e transação funcionando.
9. Dados inválidos não alteram o banco.
10. Documentação, schema, prompts e implementação coerentes.

## 11. Entrega

Faça a implementação completa, execute as verificações, corrija as falhas e abra um Pull Request para revisão. Na descrição do PR, informe:
- o que foi criado/alterado;
- versões efetivamente usadas;
- comandos executados;
- resultados reais de testes, lint e build;
- link/nome do artifact APK, se gerado;
- limitações e tarefas restantes.

Não finalize com apenas uma proposta, plano ou conjunto de telas estáticas. Não use botões decorativos. Não modifique `main` diretamente.
