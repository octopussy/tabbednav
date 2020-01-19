package com.octo.tabbednav

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    NAV = NavigationManager(this, R.id.fragmentsContainer, supportFragmentManager)
    val entries = listOf(
      NavigationManager.BuilderEntry("main") { MainFragment() },
      NavigationManager.BuilderEntry("favorites") { FavoritesFragment() },
      NavigationManager.BuilderEntry("basket") { BasketFragment() }
    )
    NAV.init(entries, savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    NAV.onSaveInstanceState(outState)
  }

  override fun onBackPressed() {
    if (!NAV.popBack()) {
      super.onBackPressed()
    }
  }

  fun selectMain(v: View) {
    NAV.select("main")
  }

  fun selectFavorites(v: View) {
    NAV.select("favorites")
  }

  fun selectBasket(v: View) {
    NAV.select("basket")
  }

  companion object {
    lateinit var NAV: NavigationManager
  }
}
