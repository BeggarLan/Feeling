package com.beggar.viewcontroller

import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.MainThread

/**
 * author: BeggarLan
 * created on: 2023/3/18 21:14
 * description: 用于管理VC
 */
interface ViewControllerManager {

  /**
   * 添加子vc
   * @param container vc的容器
   */
  @MainThread
  fun addViewController(container: ViewGroup, viewController: ViewController)

  /**
   * 添加子vc
   * @param containerId 容器view的id，如果为0的话表示不需要container
   */
  @MainThread
  fun addViewController(@IdRes containerId: Int, viewController: ViewController)

  /**
   * 添加子vc，该vc不会操作ViewTree
   */
  @MainThread
  fun addViewController(viewController: ViewController) {
    addViewController(0, viewController)
  }

  // 删除vc
  @MainThread
  fun removeViewController(viewController: ViewController)

}