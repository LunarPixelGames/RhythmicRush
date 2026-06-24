package io.github.msameer0.rhythmicrush.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

enum class TextInputType {
    DEFAULT,
    EMAIL,
    PASSWORD
}

interface TextInputController {
    fun request(
        title: String,
        current: String,
        hint: String,
        type: TextInputType,
        accepted: (String) -> Unit
    )
}

object GdxTextInputController : TextInputController {
    override fun request(
        title: String,
        current: String,
        hint: String,
        type: TextInputType,
        accepted: (String) -> Unit
    ) {
        val keyboardType = when (type) {
            TextInputType.DEFAULT -> Input.OnscreenKeyboardType.Default
            TextInputType.EMAIL -> Input.OnscreenKeyboardType.Email
            TextInputType.PASSWORD -> Input.OnscreenKeyboardType.Password
        }
        Gdx.input.getTextInput(object : Input.TextInputListener {
            override fun input(text: String) = accepted(text)
            override fun canceled() {}
        }, title, current, hint, keyboardType)
    }
}
