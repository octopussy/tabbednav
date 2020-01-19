package com.octo.tabbednav

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

  private var nav: NavigationManager by Delegates.notNull()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    nav = NavigationManager(R.id.fragmentsContainer, supportFragmentManager)
  }

  override fun onBackPressed() {
    if (!nav.popBack()) {
      super.onBackPressed()
    }
  }
}
