package com.appdex.hex

import com.appdex.arch.MviState

data class HexEditorState(
    val filePath: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val bytes: ByteArray = ByteArray(0),
    val hexRows: List<HexRow> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Long> = emptyList(),
    val searchResultIndex: Int = -1,
    val jumpOffset: Long = -1,
) : MviState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HexEditorState) return false
        return filePath == other.filePath &&
            fileName == other.fileName &&
            fileSize == other.fileSize &&
            bytes.contentEquals(other.bytes) &&
            hexRows == other.hexRows &&
            isLoading == other.isLoading &&
            isSaving == other.isSaving &&
            isDirty == other.isDirty &&
            isEditMode == other.isEditMode &&
            error == other.error &&
            searchQuery == other.searchQuery &&
            searchResults == other.searchResults &&
            searchResultIndex == other.searchResultIndex &&
            jumpOffset == other.jumpOffset
    }

    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + hexRows.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + isDirty.hashCode()
        result = 31 * result + isEditMode.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + searchQuery.hashCode()
        result = 31 * result + searchResults.hashCode()
        result = 31 * result + searchResultIndex
        result = 31 * result + jumpOffset.hashCode()
        return result
    }
}
