package com.octo.tabbednav

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

  private var nav: NavigationManager by Delegates.notNull()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    nav = NavigationManager(this, R.id.fragmentsContainer, supportFragmentManager)
    val entries = listOf(
      NavigationManager.BuilderEntry("main") { MainFragment() },
      NavigationManager.BuilderEntry("favorites") { FavoritesFragment() },
      NavigationManager.BuilderEntry("basket") { BasketFragment() }
    )
    nav.init(entries, savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    nav.onSaveInstanceState(outState)
  }

  override fun onBackPressed() {
    if (nav.popBack() == false) {
      super.onBackPressed()
    }
  }

  fun selectMain(v: View) {
    nav.select("main")
  }

  fun selectFavorites(v: View) {
    nav.select("favorites")
  }

  fun selectBasket(v: View) {
    nav.select("basket")
  }
}
