package com.octo.tabbednav

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

typealias FragmentBuilder = () -> Fragment

class NavigationManager(
  val context: Context,
  @IdRes val containerViewId: Int,
  private val fragmentManager: FragmentManager
) {

  class BuilderEntry(val name: String, val topFragmentBuilder: FragmentBuilder)

  private val stacks = mutableMapOf<String, Stack>()
  private lateinit var selectedStackName: String

  private var initialized = false

  fun init(
    entries: List<BuilderEntry>,
    savedInstanceState: Bundle?
  ) {
    if (entries.isEmpty()) return
    if (initialized) {
      throw IllegalStateException("Already initialized.")
    }
    if (savedInstanceState == null) {
      entries.forEach { addStack(it) }
      initialized = true
      select(entries.first().name)
    } else {
      entries.forEach { restoreStack(it) }
      initialized = true
      val s = savedInstanceState.getString("selected") ?: entries.first().name
      select(s)
    }

  }

  fun select(name: String) {
    checkInit()
    if (!stacks.containsKey(name)) {
      throw RuntimeException("Stack '$name' not found.")
    }

    val s = stacks[name]

    val tr = fragmentManager.beginTransaction()

    stacks.values
      .filterNot { it.containerFragmentTag == name }
      .mapNotNull { fragmentManager.findFragmentByTag(it.containerFragmentTag) }
      .forEach { tr.hide(it) }

    stacks.values
      .filter { it.containerFragmentTag == name }
      .mapNotNull { fragmentManager.findFragmentByTag(it.containerFragmentTag) }
      .forEach { tr.show(it) }

    tr.commitNow()

    selectedStackName = name
  }

  fun onSaveInstanceState(outState: Bundle) {
    outState.putString("selected", selectedStackName)
  }

  private fun checkInit() {
    if(!initialized) throw IllegalStateException("Not initialized.")
  }

  fun popBack(): Boolean {
    return false
  }

  private fun addStack(entry: BuilderEntry) {
    val containerFragment = ContainerFragment()
    fragmentManager.beginTransaction()
      .add(containerViewId, containerFragment, entry.name)
      .commitNow()

    stacks[entry.name] = Stack(containerFragment.childFragmentManager,
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder)
  }

  private fun restoreStack(entry: BuilderEntry) {
    val containerFragment = fragmentManager.findFragmentByTag(entry.name)
      ?: throw IllegalStateException("Cannot restore fragment for '${entry.name}")

    stacks[entry.name] = Stack(containerFragment.childFragmentManager,
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder)
  }

  class Stack(
    val fragmentManager: FragmentManager,
    @IdRes val containerViewId: Int,
    val containerFragmentTag: String,
    val topFragmentBuilder: FragmentBuilder
  ) {
    init {
      val topFragment = topFragmentBuilder()
      fragmentManager.beginTransaction()
        .add(containerViewId, topFragment)
        .commitNow()
    }
  }

  class ContainerFragment : Fragment() {
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      return FrameLayout(context!!).apply {
        id = R.id.stackFragmentContainer
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.TRANSPARENT)
      }
    }
  }
}