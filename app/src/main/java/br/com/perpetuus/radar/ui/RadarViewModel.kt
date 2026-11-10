package br.com.perpetuus.radar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.perpetuus.radar.data.PackagePreview
import br.com.perpetuus.radar.data.RadarRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadarViewModel(private val repository: RadarRepository) : ViewModel() {
    val records = repository.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allRecords = repository.allRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val saved = repository.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val people = repository.people.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val institutions = repository.institutions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources = repository.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val meta = repository.meta.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val logs = repository.logs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _preview = MutableStateFlow<PackagePreview?>(null)
    val preview: StateFlow<PackagePreview?> = _preview.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun setSaved(id: String, saved: Boolean) = viewModelScope.launch {
        runCatching { repository.setSaved(id, saved) }
            .onFailure { _notice.value = "Não foi possível atualizar os salvos." }
    }

    fun inspectPackage(raw: String) = viewModelScope.launch {
        _busy.value = true
        _notice.value = null
        try {
            _preview.value = withContext(Dispatchers.Default) { repository.preview(raw) }
        } catch (error: Exception) {
            _notice.value = error.message ?: "Não foi possível ler o pacote."
        } finally {
            _busy.value = false
        }
    }

    fun applyPackage() = viewModelScope.launch {
        val item = _preview.value ?: return@launch
        if (!item.canImport) return@launch
        _busy.value = true
        try {
            withContext(Dispatchers.IO) { repository.importPackage(item.rawPackage, item) }
            _preview.value = null
            _notice.value = "Atualização aplicada. O acervo local foi atualizado."
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            _notice.value = error.message ?: "A importação falhou. Nenhuma alteração parcial foi mantida."
        } finally {
            _busy.value = false
        }
    }

    fun dismissPreview() { _preview.value = null }
    fun clearNotice() { _notice.value = null }

    fun export(kind: String, onReady: (String, String) -> Unit) = viewModelScope.launch {
        _busy.value = true
        _notice.value = null
        try {
            val (content, filename) = withContext(Dispatchers.IO) {
                val exported = when (kind) {
                    "snapshot" -> repository.exportSnapshot()
                    "workset" -> repository.exportWorkset()
                    "schema" -> repository.exportSchema()
                    else -> error("Formato de exportação desconhecido.")
                }
                val name = when (kind) {
                    "snapshot" -> "radar-snapshot-v${meta.value?.dataVersion ?: 0}.json"
                    "workset" -> "radar-workset-v${meta.value?.dataVersion ?: 0}.json"
                    else -> "radar-package.schema.json"
                }
                exported to name
            }
            onReady(content, filename)
        } catch (error: Exception) {
            _notice.value = error.message ?: "Não foi possível preparar o arquivo."
        } finally {
            _busy.value = false
        }
    }

    fun dismissNotice() { _notice.value = null }
}
