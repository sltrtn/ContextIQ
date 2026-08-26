package com.contextiq.app.ui.theme

/**
 * ContextIQ's cross-platform visual contract.
 *
 * These values deliberately mirror the web tokens in `frontend/src/styles.css`
 * and the rules in `.ai/design-system.md`. Use them in new Compose UI instead
 * of introducing screen-specific spacing, radius, or interaction values.
 */
object ContextIQDesign {
    object Space {
        const val Xs = 4
        const val Sm = 8
        const val Md = 12
        const val Lg = 16
        const val Xl = 20
        const val Xxl = 24
        const val Xxxl = 32
        const val Screen = 20
    }

    object Radius {
        const val Chip = 8
        const val Field = 12
        const val Card = 16
        const val Action = 20
        const val Sheet = 20
    }

    object Control {
        const val StandardHeight = 48
        const val ProminentHeight = 56
    }

    object Motion {
        const val CardPressScale = 0.96f
        const val ButtonPressScale = 0.94f
        const val ChipPressScale = 0.92f
        const val IconPressScale = 0.90f
    }
}
