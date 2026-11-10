# Exemplos e amostras

Não há amostras de pautas políticas neste repositório para evitar que conteúdo sintético seja confundido com fatos. O contrato para a primeira carga está em `docs/schemas/radar-package.schema.json` e `docs/UPDATE-FLOW.md`.

Crie seu primeiro arquivo como `snapshot`, com `previous_version: 0`, `data_version: 1`, as seis coleções do payload e checksum SHA-256 real. Inclua apenas registros sustentados por fontes e evidências verificáveis. Pendências devem permanecer explicitamente pendentes.

O arquivo `initial-snapshot.template.json` é apenas uma estrutura vazia para orientar a primeira carga. Ele contém `checksum_sha256: null` e uma data-placeholder; por isso **não pode ser importado** até que o pacote seja preenchido e o checksum real seja calculado.
