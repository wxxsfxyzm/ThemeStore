package com.merak.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent

class MiuixPredictiveBackAnimation {
    fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        @NavigationEvent.SwipeEdge swipeEdge: Int
    ): ContentTransform = defaultPredictivePopTransitionSpec<NavKey>().invoke(this, swipeEdge)

    fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        defaultPopTransitionSpec<NavKey>().invoke(this)

    fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<NavKey>().invoke(this)
}
