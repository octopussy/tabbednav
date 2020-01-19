package com.octo.tabbednav

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager

class NavigationManager(@IdRes containerViewId: Int, val fragmentManager: FragmentManager) {

  fun popBack(): Boolean {
    return false
  }
}