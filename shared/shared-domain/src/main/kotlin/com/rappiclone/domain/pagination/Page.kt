package com.rappiclone.domain.pagination

import kotlinx.serialization.Serializable

@Serializable
data class PageRequest(
    val page: Int = 1,
    val size: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
) {
    val offset: Long get() = ((page - 1) * size).toLong()

    init {
        require(page >= 1) { "Pagina deve ser >= 1" }
        require(size in 1..100) { "Tamanho da pagina deve estar entre 1 e 100" }
    }
}

@Serializable
enum class SortDirection { ASC, DESC }

@Serializable
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
) {
    fun <R> map(transform: (T) -> R): Page<R> = Page(
        items = items.map(transform),
        page = page,
        size = size,
        totalItems = totalItems,
        totalPages = totalPages
    )

    companion object {
        fun <T> of(items: List<T>, request: PageRequest, totalItems: Long): Page<T> = Page(
            items = items,
            page = request.page,
            size = request.size,
            totalItems = totalItems,
            totalPages = ((totalItems + request.size - 1) / request.size).toInt()
        )

        fun <T> empty(request: PageRequest): Page<T> = Page(
            items = emptyList(),
            page = request.page,
            size = request.size,
            totalItems = 0,
            totalPages = 0
        )
    }
}
