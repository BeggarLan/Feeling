package com.beggar.viewcontroller

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.*

/**
 * author: BeggarLan
 * created on: 2023/3/18 22:39
 *
 * @param hostLifecycleOwner host的LifecycleOwner
 * @param viewHost           vcManager关联一个view，如果为空表示该vc不需要view
 */
class ViewControllerManagerImpl(
  private val hostLifecycleOwner: LifecycleOwner,
  private val activity: Activity,
  private val context: Context,
  private val viewHost: ViewHost?,
) : ViewControllerManager {

  constructor(
    hostLifecycleOwner: LifecycleOwner,
    activity: Activity,
    viewHost: ViewHost?
  ) : this(hostLifecycleOwner, activity, activity, viewHost)

  /**
   * 所有的子vc
   * key: vc实例
   * value: 该vc关联的生命周期监听
   */
  private val viewControllersMap = IdentityHashMap<ViewController, LifecycleEventObserver>()

  @MainThread
  override fun addViewController(container: ViewGroup, viewController: ViewController) {
    // container必须是子View
    if (viewHost?.isChildView(container) == false) {
      throw IllegalStateException("containerView $container is not childView of vc viewTree")
    }
    addViewControllerInner(container, viewController)
  }

  @MainThread
  override fun addViewController(containerId: Int, viewController: ViewController) {
    val container = if (containerId == 0) {
      null
    } else {
      viewHost?.findViewById(containerId) ?: throw IllegalStateException(
        "container id " + context.resources.getResourceEntryName(containerId) + " not found"
      )
    }
    addViewControllerInner(container, viewController)
  }

  @MainThread
  private fun addViewControllerInner(container: ViewGroup?, viewController: ViewController) {
    // 必须在[onCreate, onDestroy)之间才可以添加子vc
    check(hostLifecycleOwner.lifecycle.currentState != Lifecycle.State.INITIALIZED) {
      "addViewController must be called after onCreate"
    }
    check(hostLifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
      "addViewController can not be called after onDestroy"
    }
    // 只能添加一次
    check(!viewControllersMap.containsKey(viewController)) {
      "viewController $viewController already added"
    }

    // 子vc的生命周期驱动，这里默认是粘性事件，所以在host的任何生命周期时添加子vc都能流转子vc的状态
    val lifecycleEventObserver = LifecycleEventObserver { source, event ->
      {
        viewController.setCurrentState(event.targetState)
      }
    }

    viewController.attach(context, activity, container)
    viewControllersMap[viewController] = lifecycleEventObserver

    // 子vc状态驱动
    hostLifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)
  }

  @MainThread
  override fun removeViewController(viewController: ViewController) {
    val lifecycleEventObserver = viewControllersMap.remove(viewController)
    check(lifecycleEventObserver != null) {
      "viewController $viewController is not add"
    }
    hostLifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)
    // 确保vc状态流转到Destroy
    viewController.setCurrentState(Lifecycle.State.DESTROYED)
  }

}