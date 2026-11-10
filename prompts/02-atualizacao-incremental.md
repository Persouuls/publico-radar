# Prompt 02 — Atualização incremental do Radar Nacional

Compare o workset exportado pelo Radar com as fontes oficiais disponíveis e produza somente alterações comprovadas desde a versão-base. O workset é contexto de pesquisa, não é importável.

## Regras de pesquisa

- Identifique o `data_version` do workset e use-o como `previous_version`.
- Consulte efetivamente os documentos e os sistemas públicos relevantes. Uma página de índice não prova que o documento foi aberto e verificado.
- Registre as fontes e datas de consulta. Diferencie fonte consultada, indisponível e não consultada.
- Reavalie status, eventos, votos, autores, relatores, textos, decisões e prazos. Preserve o histórico anterior.
- Não transforme investigação ou acusação em culpa, nem decisão recorrível em decisão definitiva.
- Atualize o estado de monitoramento para registrar o que foi consultado, pendente e não coberto, inclusive quando não houver alteração documental.
- Não use operações `delete`; arquive ou atualize o status com justificativa.

## Arquivo de saída

Gere um `delta` com:

- `manifest.app_id = "radar-nacional"`;
- `manifest.schema_version = 1`;
- `manifest.package_type = "delta"`;
- `manifest.previous_version` igual à versão do workset;
- `manifest.data_version = previous_version + 1`;
- `manifest.created_at` em ISO 8601;
- `manifest.checksum_algorithm = "SHA-256"`;
- `manifest.checksum_sha256` real sobre o payload canônico.

`payload.operations` contém operações completas `upsert` nas tabelas `records`, `people`, `institutions`, `events`, `sources` e `monitoring`. Cada operação deve ter `op`, `table`, `id`, `data`; `data.id` deve ser igual a `id`. Envie o objeto completo alterado, não fragmentos. Inclua a atualização de `monitoring` mesmo quando não houver mudança documental. Não inclua registros inalterados.

## Canonicalização

Ordene recursivamente as chaves dos objetos; preserve a ordem dos arrays; codifique em UTF-8; remova espaços extras; normalize números inteiros (5.0 vira 5); proíba NaN/Infinity. Calcule SHA-256 sobre `payload`, excluindo o manifesto. Se não conseguir gerar o hash, `checksum_sha256 = null` e marque **NÃO PRONTO PARA IMPORTAÇÃO**.

## Relatório

Apresente data/hora de corte, fontes realmente verificadas, fontes indisponíveis, período e cobertura efetiva, lacunas, contagens de novos/alterados/verificados sem mudança/pendentes/conflitos, principais mudanças por eixo e evidências/URLs oficiais que justificam os upserts. Não confunda contagem com mérito.

Use a canonicalização abaixo para calcular o checksum (o hash é sobre `payload`, não sobre `manifest`):

```python
import json, hashlib

def canonicalize(value):
    if isinstance(value, dict):
        return "{" + ",".join(
            json.dumps(k, ensure_ascii=False) + ":" + canonicalize(value[k])
            for k in sorted(value)
        ) + "}"
    if isinstance(value, list):
        return "[" + ",".join(canonicalize(x) for x in value) + "]"
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), allow_nan=False)

checksum = hashlib.sha256(canonicalize(payload).encode("utf-8")).hexdigest()
```
