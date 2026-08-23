package moe.rorita.kanaschule.store

/**
 * Ob die Plattform eine eigene Bildschirmtastatur braucht. Das ist eine Frage
 * der Faehigkeit, nicht der Fenstergroesse: ein grosses Android-Tablet braucht
 * sie, ein schmales Desktop-Fenster nicht.
 */
expect val prefersOnScreenKeyboard: Boolean

expect val platformName: String
