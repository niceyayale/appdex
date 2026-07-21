package com.appdex.hex

import com.appdex.arch.MviIntent

sealed interface HexEditorIntent : MviIntent {
    /** 打开文件。 */
    data class OpenFile(val filePath: String) : HexEditorIntent
    /** 修改指定偏移量的字节。 */
    data class EditByte(val offset: Int, val value: Byte) : HexEditorIntent
    /** 保存文件。 */
    data object Save : HexEditorIntent
    /** 搜索。 */
    data class Search(val query: String, val isHex: Boolean) : HexEditorIntent
    /** 跳转到偏移量。 */
    data class JumpToOffset(val offset: Long) : HexEditorIntent
    /** 切换编辑模式。 */
    data object ToggleEditMode : HexEditorIntent
    /** 清除搜索结果。 */
    data object ClearSearch : HexEditorIntent
    /** 清除错误。 */
    data object ClearError : HexEditorIntent
}
