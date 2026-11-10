# Estado de verificação desta entrega

## Recuperação do projeto

- O ZIP de transferência passou no teste de integridade (`unzip -t`) e foi extraído na raiz do repositório.
- O bootstrap customizado do pacote-base foi substituído pelo Gradle Wrapper oficial 9.5.0, gerado pela tarefa `wrapper` do Gradle 9.5.0.
- A distribuição Gradle 9.5.0 está configurada com o SHA-256 oficial publicado.
- `./gradlew --version` executou com sucesso nesta recuperação: Gradle 9.5.0, usando a JVM 25.0.5 disponível no Codespace.
- A configuração Android do projeto usa Java 17. A compilação do aplicativo não foi executada durante esta recuperação.

## Verificações registradas no pacote-base

As verificações abaixo constavam no pacote recebido e não foram reexecutadas durante a recuperação:

- 10 arquivos XML de recursos/manifests foram analisados pelo parser XML.
- 3 arquivos JSON foram analisados sintaticamente.
- O schema Draft 2020-12 aceitou um pacote snapshot vazio, com manifesto e SHA-256 real calculado sobre o payload canônico.
- O schema usado pelo app (`app/src/main/assets/`) e o schema documentado (`docs/schemas/`) são idênticos.
- O bootstrap Java próprio foi compilado com JDK 17.
- O código Kotlin foi submetido ao parser local para busca de erros de sintaxe. O compilador isolado não possui as dependências Android/Compose/Room, portanto isso não substitui compilação Gradle.

## Verificações ainda não executadas

Conforme solicitado, não foram instalados componentes do Android SDK nem executados testes Gradle, lint ou compilação. Nenhum APK foi gerado durante a recuperação; o projeto ainda não está declarado como build-verificado.

O workflow `.github/workflows/android-build.yml` está configurado para executar testes unitários, lint e `assembleDebug` no GitHub Actions, e publicar o APK debug como artifact. Um resultado verde desse workflow ou uma compilação local será a confirmação efetiva de build.

## GitHub

A leitura de metadados do repositório `Persouuls/publico-radar` foi possível, mas a criação de conteúdo pela integração GitHub retornou HTTP 403 (`Resource not accessible by integration`). Nenhum arquivo foi gravado no repositório por esta sessão. O projeto é entregue em ZIP para envio manual ou após concessão de permissão de gravação à integração.
