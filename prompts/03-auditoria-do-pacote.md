# Prompt 03 — Auditoria de pacote do Radar Nacional

Audite o snapshot/delta antes da importação. Compare-o com o workset ou snapshot de origem e com a memória de monitoramento. Não corrija silenciosamente evidência faltante: liste o problema e solicite nova verificação.

## Checklist obrigatório

1. `app_id`, `schema_version`, `package_type`, `previous_version`, `data_version` e `created_at` corretos.
2. Checksum SHA-256 recalculado a partir do payload canônico; nenhum hash inventado.
3. IDs estáveis, sem duplicatas dentro de tabelas; `data.id` idêntico ao ID da operação.
4. Referências a pessoas, instituições, registros, eventos e fontes coerentes.
5. Todos os registros têm tipo, título, resumo e situação factual compreensível.
6. Avaliações por eixo usam somente NABOTE_01 a NABOTE_10 e incluem incidência, nota justificada e evidência específica.
7. Separação de fatos, alegações, investigações, decisões recorríveis e decisões definitivas.
8. Nenhuma exclusão de histórico; arquivamento e substituição têm justificativa.
9. Memória de monitoramento registra consultas reais, lacunas e pendências.
10. As versões estão corretas e o delta parte exatamente da versão-base do workset.

## Resultado

Classifique cada achado como bloqueador, aviso ou aprovado. Informe se o arquivo está **PRONTO PARA IMPORTAÇÃO** ou **NÃO PRONTO PARA IMPORTAÇÃO**. Liste os defeitos com caminho JSON e correção necessária. Não declare validação JSON Schema formal sem executá-la. Não declare pesquisa, fonte, checksum ou evidência que não tenha sido realmente conferida.

O checksum deve ser recalculado com a mesma função de canonicalização descrita em `prompts/02-atualizacao-incremental.md`. Se houver divergência de hash, trate-a como bloqueadora. SHA-256 detecta alteração do conteúdo em relação ao manifesto, mas não autentica a autoria do pacote.
