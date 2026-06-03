package com.orbit.app.library

import com.orbit.app.memory.MemorySearchResult

data class LibraryUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<MemorySearchResult> = emptyList(),
    val searched: Boolean = false,
    val unavailableTitle: String? = null,
    val unavailableDetail: String? = null,
    val openEnvelopeId: String? = null,
) {
    val canSearch: Boolean get() = query.trim().isNotEmpty() && !loading
    val unavailableMessage: String? get() = unavailableTitle
}
