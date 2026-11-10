# Operação manual com ChatGPT

Use os prompts de pesquisa e auditoria incluídos em `prompts/`:

- **Carga inicial:** `01-carga-inicial.md` — pesquisa por eixos NABOTE em fontes oficiais; lotes de até 20 registros; sem alegar cobertura que não foi verificada; saída como snapshot.
- **Atualização incremental:** `02-atualizacao-incremental.md` — compara o workset, verifica apenas fontes efetivamente consultadas e gera delta com upserts completos e memória atualizada.
- **Auditoria:** `03-auditoria-do-pacote.md` — confere IDs, referências, status factual, hash, versões, integridade e evidências antes da importação.

O aplicativo não se conecta ao ChatGPT. Esses arquivos são uma operação externa iniciada pelo usuário. Não confundir workset com pacote importável.
