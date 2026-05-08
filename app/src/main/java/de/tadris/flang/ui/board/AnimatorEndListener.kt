package de.tadris.flang.ui.board

import android.animation.Animator

class AnimatorEndListener(private val onEnd: () -> Unit) : Animator.AnimatorListener {

    override fun onAnimationStart(animation: Animator) { }

    override fun onAnimationEnd(animation: Animator) {
        onEnd()
    }

    override fun onAnimationCancel(animation: Animator) { }

    override fun onAnimationRepeat(animation: Animator) { }
}