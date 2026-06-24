package io.github.msameer0.rhythmicrush.lwjgl3.input

import io.github.msameer0.rhythmicrush.input.TextInputController
import io.github.msameer0.rhythmicrush.input.TextInputType
import org.lwjgl.util.tinyfd.TinyFileDialogs

class DesktopTextInputController : TextInputController {
    override fun request(
        title: String,
        current: String,
        hint: String,
        type: TextInputType,
        accepted: (String) -> Unit
    ) {
        val message = hint.takeIf { it.isNotBlank() } ?: "Enter a value"
        val defaultValue: CharSequence? =
            if (type == TextInputType.PASSWORD) null else current
        val result = TinyFileDialogs.tinyfd_inputBox(title, message, defaultValue) ?: return
        accepted(result)
    }
}
