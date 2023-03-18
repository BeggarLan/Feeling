package com.beggar.viewcontroller

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * author: BeggarLan
 * created on: 2023/3/18 20:58
 * description: 具有生命周期和ViewModelStore管理能力的VC基类
 * vc树(嵌套)场景中，生命周期的回调顺序：
 */
abstract class ViewController : LifecycleOwner, ViewModelStoreOwner {

  protected lateinit var context: Context

  protected lateinit var activity: Activity

  // vc的容器
  private var container: ViewGroup? = null

  // vc的ContentView，为null表示vc是纯逻辑controller
  private var contentView: View? = null

  // 用于管理自己的子vc
  private lateinit var childViewControllerManager: ViewControllerManager

  // 该vc提供viewModelStore的能力
  private val viewModelStore = ViewModelStore()

  override fun getViewModelStore(): ViewModelStore {
    return viewModelStore
  }

  // 该vc自己的Lifecycle
  private val lifecycleRegistry = LifecycleRegistry(this)

  override fun getLifecycle(): Lifecycle {
    return lifecycleRegistry
  }

  init {
    // 状态改变 --> 回调
    lifecycleRegistry.addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
          Lifecycle.Event.ON_CREATE -> onCreate()
          Lifecycle.Event.ON_START -> onStart()
          Lifecycle.Event.ON_RESUME -> onResume()
          Lifecycle.Event.ON_PAUSE -> onPause()
          Lifecycle.Event.ON_STOP -> onStop()
          Lifecycle.Event.ON_DESTROY -> performDestroy()
          else -> {}
        }
      }
    })
  }

  // 注入依赖，时机应该尽快(如被添加到vc树中时)
  internal fun attach(context: Context, activity: Activity, container: ViewGroup?) {
    this.context = context
    this.activity = activity
    this.container = container

    // 子vc的viewHost是自己的contentView，即子vc的view是自己view的孩子
    val childViewHost: ViewHost? = contentView?.let {
      ViewHost.byViewProvider({ it })
    }
    childViewControllerManager =
      ViewControllerManagerImpl(this, activity, childViewHost)
  }

  /**
   * 设置vc的状态，会驱动vc的生命周期
   * @param state 允许不连续的状态跳转，比如当前状态是CREATED，state是RESUMED，那么会执行start、resume
   */
  internal fun setCurrentState(state: Lifecycle.State) {
    // 保证连续的生命周期，不能跳跃
    while (true) {
      val currentState = lifecycleRegistry.currentState
      if (currentState == state) {
        break
      }

      // 是否要向前走
      val forward = currentState < state
      val targetState = if (forward) {
        // 前进
        when (currentState) {
          Lifecycle.State.INITIALIZED -> Lifecycle.State.CREATED
          Lifecycle.State.CREATED -> Lifecycle.State.STARTED
          Lifecycle.State.STARTED -> Lifecycle.State.RESUMED
          else -> throw IllegalStateException("ViewController invalid state: $currentState -> $state")
        }
      } else {
        // 后退
        when (currentState) {
          Lifecycle.State.CREATED -> Lifecycle.State.DESTROYED
          Lifecycle.State.STARTED -> Lifecycle.State.CREATED
          Lifecycle.State.RESUMED -> Lifecycle.State.STARTED
          else -> throw IllegalStateException("ViewController invalid state: $currentState -> $state")
        }
      }
      // 状态更新
      lifecycleRegistry.currentState = targetState
    }
  }

  /**
   * 设置vc自己的contentView, 也就意味着必须有container, 也就意味着父vc添加自己的时候必须传递containerView
   */
  protected fun setContentView(@LayoutRes layoutId: Int) {
    if (container == null) {
      throw IllegalStateException("ViewController $this does not attach to a container, can not call setContentView")
    }
    setContentView(LayoutInflater.from(context).inflate(layoutId, container, false))
  }

  /**
   * 设置vc自己的contentView, 也就意味着必须有container, 也就意味着父vc添加自己的时候必须传递containerView
   */
  protected fun setContentView(view: View) {
    if (container == null) {
      throw IllegalStateException("ViewController $this does not attach to a container, can not call setContentView")
    }
    container?.let {
      contentView = view
      it.addView(contentView)
    }
  }

  /**
   * 添加子vc
   * @param container vc的容器
   */
  @MainThread
  fun addViewController(container: ViewGroup, viewController: ViewController) {
    childViewControllerManager.addViewController(container, viewController)
  }

  /**
   * 添加子vc
   * @param containerId 容器view的id，如果为0的话表示不需要container
   */
  @MainThread
  fun addViewController(@IdRes containerId: Int, viewController: ViewController) {
    childViewControllerManager.addViewController(containerId, viewController)
  }

  /**
   * 添加子vc，该vc不会操作ViewTree
   */
  @MainThread
  fun addViewController(viewController: ViewController) {
    addViewController(0, viewController)
  }

  // 删除vc
  @MainThread
  fun removeViewController(viewController: ViewController) {
    childViewControllerManager.removeViewController(viewController)
  }

  private fun performDestroy() {
    viewModelStore.clear()
    // 从父view中移除自己的view
    contentView?.let {
      container?.removeView(it)
    }
    // 回调
    onDestroy()
  }

  @CallSuper
  protected open fun onCreate() {
  }

  @CallSuper
  protected open fun onStart() {
  }

  @CallSuper
  protected open fun onResume() {
  }

  @CallSuper
  protected open fun onPause() {
  }

  @CallSuper
  protected open fun onStop() {
  }

  @CallSuper
  protected open fun onDestroy() {
  }
}