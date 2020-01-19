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
import timber.log.Timber
import java.util.*

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
      entries.forEach { restoreStack(it, savedInstanceState) }
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
    stacks.values.forEach { it.onSaveInstanceState(outState) }
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
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder, null
    )
  }

  private fun restoreStack(
    entry: BuilderEntry,
    savedInstanceState: Bundle
  ) {
    val containerFragment = fragmentManager.findFragmentByTag(entry.name)
      ?: throw IllegalStateException("Cannot restore fragment for '${entry.name}")

    stacks[entry.name] = Stack(
      containerFragment.childFragmentManager,
      R.id.stackFragmentContainer, entry.name, entry.topFragmentBuilder,
      savedInstanceState
    )
  }

  class Stack(
    private val fragmentManager: FragmentManager,
    @IdRes val containerViewId: Int,
    val containerFragmentTag: String,
    val topFragmentBuilder: FragmentBuilder,
    savedInstanceState: Bundle?
  ) {

    private val tagStack = mutableListOf<String>()

    init {
      if (savedInstanceState == null) {
        pushFragment(topFragmentBuilder(), false)
      } else {
        val array = savedInstanceState.getStringArray("${containerFragmentTag}_tags") ?: arrayOf()
        tagStack.addAll(array.toList())
        fragmentManager.beginTransaction()
          .apply {
            // hide fragments except the top one
            if (tagStack.size > 1) {
              tagStack.subList(0, tagStack.size - 2)
                .mapNotNull { fragmentManager.findFragmentByTag(it) }
                .forEach { hide(it) }
            }
          }
          .commitNow()

        Timber.d("Stack '$topFragmentBuilder' restored with $tagStack")
      }
    }

    fun onSaveInstanceState(outState: Bundle) {
      outState.putStringArray("${containerFragmentTag}_tags", tagStack.toTypedArray())
    }

    fun pushFragment(fragment: Fragment, addToBackStack: Boolean) {
      val tag = generateTag(fragment)
      val tr = fragmentManager.beginTransaction()
      if (tagStack.isNotEmpty()) {
        val visibleFragment = fragmentManager.findFragmentByTag(tagStack.last())
        tr.hide(visibleFragment!!)
      }

      if (addToBackStack) {
        tr.addToBackStack(null)
      }

      tr.add(containerViewId, fragment, tag)
        .commit()

      tagStack.add(tag)
    }

    fun popBack(): Boolean {
      if (fragmentManager.backStackEntryCount > 0) {
        fragmentManager.popBackStack()
        tagStack.removeAt(tagStack.size - 1)
        return true
      }

      return false
    }

    private fun generateTag(fragment: Fragment): String =
      "${fragment.javaClass.simpleName}_${UUID.randomUUID()}"
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