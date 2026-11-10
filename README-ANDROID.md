# Radar Nacional — aplicativo Android

Aplicativo nativo, local-first e offline-first para catalogar e acompanhar pautas legislativas, jurídicas e institucionais brasileiras. O banco começa **vazio**. O usuário abastece o acervo por snapshots e atualizações JSON preparados externamente e revisados antes da importação.

## O que está incluído

- Android nativo com Kotlin, Jetpack Compose e Material 3.
- Room/SQLite local; nenhuma conta ou banco em nuvem.
- Início, Radar, Salvos, Agenda, Dados/Atualizações e ficha detalhada.
- Busca local por título, ementa, identificadores, atores e fontes.
- Filtros combináveis por eixo NABOTE, horizonte, tipo, esfera, situação, efeito interpretado, urgência e arquivamento; filtros avançados ficam recolhidos até serem solicitados.
- Importação de snapshot/delta com prévia, conferência de app/schema/versão e SHA-256.
- Aplicação de atualizações numa transação Room e cópia de segurança JSON anterior à importação.
- Exportação de workset, snapshot completo e schema.
- Histórico local das operações e preferências de salvos.
- Tema claro/escuro do sistema, navegação inferior e estados vazios com placeholders de dimensão realista, sem registros políticos fictícios.
- Workflow GitHub Actions para executar testes, lint e gerar APK debug como artefato.

## O que não faz nesta versão

Não pesquisa fontes públicas, não executa IA, não faz scraping, não sincroniza dados entre aparelhos, não cria notificações por consulta remota e não roda agentes em segundo plano. O ChatGPT é utilizado manualmente fora do aplicativo. Não há permissão `INTERNET`, conta, telemetria, anúncio ou API paga.

## Abrir e compilar

Requisitos: Android Studio estável com Android SDK 36 e JDK 17 ou superior. O repositório inclui o Gradle Wrapper oficial, gerado pelo Gradle 9.5.0. A distribuição Gradle 9.5.0 está fixada por HTTPS e seu SHA-256 oficial está configurado em `gradle/wrapper/gradle-wrapper.properties`.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK debug esperado: `app/build/outputs/apk/debug/app-debug.apk`.

Também é possível executar o workflow **Android build** em GitHub Actions. O APK será publicado como artifact baixável. APK debug é apenas para teste e não possui assinatura de produção.

## Instalar no aparelho

Baixe `app-debug.apk` do artifact do GitHub Actions, transfira para o Android e instale permitindo a instalação por essa origem, se o sistema solicitar. Não compartilhe APK debug como versão de produção.

## Primeira carga de dados

O banco local está vazio por design. O primeiro snapshot deve usar versão anterior 0 e versão de dados 1. Cargas parciais podem ser importadas posteriormente por `delta`, sempre com a versão-base instalada. O pacote inicial deve ter:

- `manifest.app_id`: `radar-nacional`;
- `manifest.schema_version`: `1`;
- `manifest.package_type`: `snapshot`;
- `manifest.previous_version`: `0`;
- `manifest.data_version`: `1`;
- `manifest.checksum_algorithm`: `SHA-256`;
- `manifest.checksum_sha256`: hash real calculado sobre o `payload` canônico;
- `payload`: arrays `records`, `people`, `institutions`, `events`, `sources` e `monitoring`.

O registro principal de `monitoring` deve usar `id: "state"`. Para preparar o primeiro arquivo real, use [Prompt de carga inicial](prompts/01-carga-inicial.md). O arquivo em `examples/initial-snapshot.template.json` é somente um modelo estrutural: está deliberadamente incompleto e não deve ser importado como está. Para especificar o arquivo, consulte [schema JSON](docs/schemas/radar-package.schema.json), [modelo de dados](docs/DATA-MODEL.md) e [fluxo de atualização](docs/UPDATE-FLOW.md). Não cadastre exemplos inventados como fatos reais.

## Os dez eixos NABOTE

A taxonomia está documentada em [DATA-MODEL.md](docs/DATA-MODEL.md) e é fixa: vida desde a concepção; expressão; propriedade e segurança jurídica; liberdade econômica; armas e legítima defesa; primazia dos pais e liberdade educacional; liberdade religiosa; consciência médica; segurança pública; igualdade perante a lei.

## Árvore principal

```text
app/src/main/java/br/com/perpetuus/radar/
├── MainActivity.kt
├── RadarApplication.kt
├── data/
│   ├── Database.kt
│   ├── PackageEngine.kt
│   └── RadarRepository.kt
└── ui/
    ├── RadarApp.kt
    ├── RadarViewModel.kt
    └── theme/Theme.kt

docs/
├── ARCHITECTURE.md
├── DATA-MODEL.md
├── UPDATE-FLOW.md
└── schemas/radar-package.schema.json
.github/workflows/android-build.yml
```

## Decisões futuras

A versão atual concentra-se no aplicativo Android. Uma versão desktop ou um backend central poderão ser considerados depois, mantendo o mesmo contrato JSON. O banco local do Android não é automaticamente compartilhado com outros dispositivos. O workset inclui o índice completo, inclusive registros arquivados, e prioriza até 20 registros ativos para revisão.
