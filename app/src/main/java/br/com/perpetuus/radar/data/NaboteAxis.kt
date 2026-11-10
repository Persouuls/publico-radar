package br.com.perpetuus.radar.data

data class NaboteAxis(
    val id: String,
    val number: Int,
    val title: String,
    val searchTerms: List<String>
)

object NaboteAxes {
    val all = listOf(
        NaboteAxis("NABOTE_01", 1, "Proteção da vida desde a concepção", listOf("nascituro", "aborto", "gestação", "embrião", "vida humana")),
        NaboteAxis("NABOTE_02", 2, "Liberdade de expressão e oposição à censura", listOf("expressão", "imprensa", "censura", "opinião", "manifestação")),
        NaboteAxis("NABOTE_03", 3, "Defesa da propriedade privada e da segurança jurídica", listOf("propriedade", "posse", "desapropriação", "segurança jurídica", "contrato")),
        NaboteAxis("NABOTE_04", 4, "Liberdade econômica, contratual e desburocratização", listOf("livre iniciativa", "regulação", "licenciamento", "burocracia", "atividade econômica")),
        NaboteAxis("NABOTE_05", 5, "Direito às armas e legítima defesa", listOf("armas", "porte", "posse", "legítima defesa", "autodefesa")),
        NaboteAxis("NABOTE_06", 6, "Primazia dos pais, homeschooling e liberdade educacional", listOf("homeschooling", "educação domiciliar", "pais", "escola", "ensino")),
        NaboteAxis("NABOTE_07", 7, "Liberdade religiosa, de proselitismo e de crítica", listOf("religião", "culto", "proselitismo", "crença", "consciência religiosa")),
        NaboteAxis("NABOTE_08", 8, "Liberdade de consciência médica e consentimento informado", listOf("consentimento informado", "autonomia médica", "tratamento", "objeção de consciência", "saúde")),
        NaboteAxis("NABOTE_09", 9, "Segurança pública, proteção da vítima e aplicação efetiva da lei", listOf("segurança pública", "vítima", "polícia", "execução penal", "proteção")),
        NaboteAxis("NABOTE_10", 10, "Igualdade perante a lei, sem privilégios por sexo, raça ou identidade", listOf("isonomia", "igualdade", "privilégio legal", "discriminação", "tratamento jurídico"))
    )

    fun byNumber(number: Int): NaboteAxis? = all.firstOrNull { it.number == number }
}
