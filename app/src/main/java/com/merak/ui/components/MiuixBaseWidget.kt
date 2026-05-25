package com.merak.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
fun MiuixSwitchWidget(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleAction = {
        if (enabled) {
            onCheckedChange(!checked)
        }
    }

    BasicComponent(
        title = title,
        summary = description,
        enabled = enabled,
        onClick = toggleAction,
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * A BasicComponent variant that displays a Checkbox as its right action.
 * The entire row is clickable to toggle the checked state.
 *
 * @param title The main text title.
 * @param description The supporting text (summary).
 * @param enabled Controls the enabled state of the component and the Checkbox.
 * @param checked The current checked state of the Checkbox.
 * @param onCheckedChange A lambda called when the checked state changes.
 */
@Composable
fun MiuixCheckboxWidget(
    title: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val toggleAction = {
        if (enabled) {
            onCheckedChange(!checked)
        }
    }

    BasicComponent(
        title = title,
        summary = description,
        enabled = enabled,
        onClick = toggleAction,
        endActions = {
            Checkbox(
                state = ToggleableState(value = checked),
                onClick = { onCheckedChange(!checked) },
                colors = CheckboxDefaults.checkboxColors(),
                enabled = enabled
            )
        }
    )
}

/**
 * A setting pkg that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the pkg.
 * @param title The main title text of the pkg.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this pkg is clicked.
 */
@Composable
fun MiuixNavigationItemWidget(
    icon: ImageVector? = null,
    title: String,
    description: String,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    ArrowPreference(
        title = title,
        summary = description,
        insideMargin = insideMargin,
        onClick = onClick
    )
}
