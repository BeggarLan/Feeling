@file:JvmName("ViewControllerExt")
package com.kuaishou.live.ext

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.beggar.viewcontroller.ViewController
import com.beggar.viewcontroller.ViewControllerManagerImpl
import com.beggar.viewcontroller.ViewHost
import java.lang.IllegalStateException

/**
 * 针对Fragment实现的ViewControllerManager的HostFragment
 * 监听其生命周期来驱动ViewControllerManager
 * HostFragment的生命周期相当于ParentFragment的ViewLifecycle
 */
class FragmentViewControllerHostFragment: Fragment() {
  companion object {
    const val TAG = "com.kuaishou.live.ext.FragmentViewControllerHostFragment"
  }

  lateinit var controllerManager : ViewControllerManagerImpl
    private set

  init {
    // ViewControllerHostFragment不能被重建
    // 因为ViewControllerHostFragment需要监听parentFragment的生命周期，需要手动调用
    // 自动重建缺少这些调用，会导致controllerManager的状态错误
    retainInstance = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val parentView = parentFragment?.view
      ?: throw IllegalStateException("addViewController must be called after onCreateView")

    controllerManager = ViewControllerManagerImpl(
      hostLifecycleOwner = this,
      activity= requireActivity(),
      context= requireContext(),
      viewHost = ViewHost.byView(parentView))
  }
}

private fun Fragment.requireViewControllerManager(): ViewControllerManagerImpl {
  var fragment = childFragmentManager.findFragmentByTag(FragmentViewControllerHostFragment.TAG)
  if (fragment == null) {
    val hostFragment = FragmentViewControllerHostFragment()
    childFragmentManager
      .beginTransaction()
      .add(hostFragment, FragmentViewControllerHostFragment.TAG)
      .commitNowAllowingStateLoss()

    val lifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
      override fun onFragmentViewDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
        // 当前Fragment进入onFragmentViewDestroyed，此时需要移除HostFragment
        // 因为ViewController与Fragment的ViewLifecycle保持一致
        if (fragment === this@requireViewControllerManager) {
          childFragmentManager
            .beginTransaction()
            .remove(hostFragment)
            .commitNowAllowingStateLoss()
        }
      }

      override fun onFragmentDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment === this@requireViewControllerManager) {
          // Host fragment被销毁时，需要移除lifecycleCallbacks，避免内存泄漏
          fragmentManager.unregisterFragmentLifecycleCallbacks(this)
        }
      }
    }
    requireFragmentManager().registerFragmentLifecycleCallbacks(lifecycleCallbacks, false)
    fragment = hostFragment
  }
  return (fragment as FragmentViewControllerHostFragment).controllerManager
}

/**
 * 向Fragment中添加ViewController
 * 此方法只能在onCreateView 与 onDestroyView之间调用
 */
fun <T: ViewController> Fragment.addViewController(
  containerId: Int,
  viewController: T
): T {
  requireViewControllerManager().addViewController(containerId, viewController)
  return viewController
}

fun <T: ViewController> Fragment.addViewController(
  container: ViewGroup,
  viewController: T
): T {
  requireViewControllerManager().addViewController(container, viewController)
  return viewController
}

fun <T: ViewController> Fragment.addViewController(
  viewController: T
): T {
  return addViewController(0, viewController)
}

fun Fragment.removeViewController(viewController: ViewController) {
  requireViewControllerManager().removeViewController(viewController)
}

internal class ActivityViewHost(
  private val activity: FragmentActivity
) : ViewHost {
  override fun <T : View> findViewById(resId: Int): T? {
   return activity.findViewById(resId)
  }

  override fun isChildView(view: View): Boolean {
    return ViewHost.isChildView(activity.window.decorView, view)
  }
}

/**
 * 针对FragmentActivity实现的ViewControllerManager的HostFragment
 * 监听其生命周期来驱动ViewControllerManager
 * HostFragment的生命周期与FragmentActivity一致
 */
class ActivityViewControllerHostFragment: Fragment() {
  companion object {
    const val TAG = "com.kuaishou.live.ext.FragmentViewControllerHostFragment"
  }

  lateinit var controllerManager : ViewControllerManagerImpl
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    controllerManager = ViewControllerManagerImpl(
      hostLifecycleOwner = requireActivity(),
      activity = requireActivity(),
      context = requireContext(),
      viewHost = ActivityViewHost(requireActivity())
    )
  }
}

private fun FragmentActivity.requireViewControllerManager(): ViewControllerManagerImpl {
  var fragment = supportFragmentManager.findFragmentByTag(ActivityViewControllerHostFragment.TAG)
  if (fragment == null) {
    fragment = ActivityViewControllerHostFragment()
    supportFragmentManager
      .beginTransaction()
      .add(fragment, ActivityViewControllerHostFragment.TAG)
      .commitNowAllowingStateLoss()
  }
  return (fragment as ActivityViewControllerHostFragment).controllerManager
}

/**
 * 向FragmentActivity中添加ViewController
 * 此方法只能在onCreate 与 onDestroy之间调用
 */
fun <T: ViewController> FragmentActivity.addViewController(
  containerId: Int,
  viewController: T
): T {
  requireViewControllerManager().addViewController(containerId, viewController)
  return viewController
}

fun <T: ViewController> FragmentActivity.addViewController(
  container: ViewGroup,
  viewController: T
): T {
  requireViewControllerManager().addViewController(container, viewController)
  return viewController
}

fun <T: ViewController> FragmentActivity.addViewController(
  viewController: T
): T {
  return addViewController(0, viewController)
}

fun FragmentActivity.removeViewController(viewController: ViewController) {
  requireViewControllerManager().removeViewController(viewController)
}