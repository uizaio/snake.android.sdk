package com.uiza.sdkbroadcast.enums

import com.pedro.encoder.utils.gl.TranslateTo

enum class Translate(
    val translateTo: TranslateTo
) {
    CENTER(TranslateTo.CENTER),
    LEFT(TranslateTo.LEFT),
    RIGHT(TranslateTo.RIGHT),
    TOP(TranslateTo.TOP),
    BOTTOM(TranslateTo.BOTTOM),
    TOP_LEFT(TranslateTo.TOP_LEFT),
    TOP_RIGHT(TranslateTo.TOP_RIGHT),
    BOTTOM_LEFT(TranslateTo.BOTTOM_LEFT),
    BOTTOM_RIGHT(TranslateTo.BOTTOM_RIGHT);
}
