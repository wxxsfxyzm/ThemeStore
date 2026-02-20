package com.merak.core.installer

data class ThemeModule(
    val name: String,
    val flag: Long,
    val isDefaultChecked: Boolean = false
)