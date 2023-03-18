package com.beggar.viewcontroller

import android.view.View
import androidx.annotation.IdRes

/**
 * author: BeggarLan
 * created on: 2023/3/18 22:41
 * description: 持有一个view
 */
interface ViewHost {

  fun <T : View> findViewById(@IdRes resId: Int): T?

  /**
   * view是否是viewHost的子view
   */
  fun isChildView(view: View): Boolean

  companion object {
    /**
     * 构造viewHost的工具方法
     * @param viewProvider 提供hostView
     */
    @JvmStatic
    fun byViewProvider(viewProvider: () -> View): ViewHost {
      return SimpleViewHost(viewProvider);
    }

    /**
     * 构造viewHost的工具方法
     * @param view 作为host
     */
    @JvmStatic
    fun byView(view: View): ViewHost {
      return SimpleViewHost { view }
    }

    /**
     * childView是否是container的子view
     */
    @JvmStatic
    fun isChildView(container: View, childView: View): Boolean {
      var v: View? = childView
      // 递归找
      while (v != null) {
        if (v == container) {
          return true;
        }
        v = v.parent as? View
      }
      return false
    }

  }
}

/**
 * viewHost的实现
 */
private class SimpleViewHost(private val viewProvider: () -> View) : ViewHost {
  override fun <T : View> findViewById(resId: Int): T? {
    return viewProvider().findViewById(resId)
  }

  override fun isChildView(view: View): Boolean {
    return ViewHost.isChildView(viewProvider(), view)
  }
}
