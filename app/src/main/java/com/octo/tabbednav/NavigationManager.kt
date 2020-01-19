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
  private lateinit var firstStackName: String

  private var initialized = false

  fun init(
    entries: List<BuilderEntry>,
    savedInstanceState: Bundle?
  ) {
    if (entries.isEmpty()) return
    if (initialized) {
      throw IllegalStateException("Already initialized.")
    }

    firstStackName = entries.first().name

    if (savedInstanceState == null) {
      entries.forEach { addStack(it) }
      initialized = true
      select(firstStackName)
    } else {
      entries.forEach { restoreStack(it) }
      initialized = true
      val s = savedInstanceState.getString("selected") ?: firstStackName
      select(s)
    }
  }

  fun select(name: String) {
    checkInit()
    if (!stacks.containsKey(name)) {
      throw RuntimeException("Stack '$name' not found.")
    }

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

  fun pushFragment(fragment: Fragment, addToBackStack: Boolean = true) {
    getSelectedStack().pushFragment(fragment, addToBackStack)
  }

  fun popBack(): Boolean {
    var handled = getSelectedStack().popBack()
    if (!handled && selectedStackName != firstStackName) {
      select(firstStackName)
      handled = true
    }
    return handled
  }

  private fun checkInit() {
    if (!initialized) throw IllegalStateException("Not initialized.")
  }

  private fun getSelectedStack(): Stack {
    return stacks[selectedStackName] ?: throw IllegalStateException()
  }

  private fun addStack(entry: BuilderEntry) {
    val containerFragment = ContainerFragment()
    fragmentManager.beginTransaction()
      .add(containerViewId, containerFragment, entry.name)
      .commitNow()

    stacks[entry.name] = Stack(
      containerFragment.childFragmentManager,
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder
    )
  }

  private fun restoreStack(entry: BuilderEntry) {
    val containerFragment = fragmentManager.findFragmentByTag(entry.name)
      ?: throw IllegalStateException("Cannot restore fragment for '${entry.name}")

    stacks[entry.name] = Stack(
      containerFragment.childFragmentManager,
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder
    )
  }

  class Stack(
    val fragmentManager: FragmentManager,
    @IdRes val containerViewId: Int,
    val containerFragmentTag: String,
    val topFragmentBuilder: FragmentBuilder
  ) {

    init {
      pushFragment(topFragmentBuilder(), false)
    }

    fun pushFragment(fragment: Fragment, addToBackStack: Boolean) {
      val visibleFragments = fragmentManager.fragments.filter { it.isVisible }
      if (visibleFragments.size > 1) {
        throw IllegalStateException("More than 1 visible fragments in '$containerFragmentTag'")
      }
      val tr = fragmentManager.beginTransaction()
      if (visibleFragments.isNotEmpty()) {
        tr.hide(visibleFragments.first())
      }

      if (addToBackStack) {
        tr.addToBackStack(null)
      }

      tr.add(containerViewId, fragment)
        .commit()
    }

    fun popBack(): Boolean {
      if (fragmentManager.backStackEntryCount > 0) {
        fragmentManager.popBackStack()
        return true
      }
      return false
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