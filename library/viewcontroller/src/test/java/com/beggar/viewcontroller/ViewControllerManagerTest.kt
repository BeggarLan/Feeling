package com.kuaishou.live.viewcontroller.lifecycle

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.beggar.viewcontroller.ViewController
import com.beggar.viewcontroller.ViewControllerManagerImpl
import com.beggar.viewcontroller.ViewHost
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class ActivityA : Activity() {

}

class SimpleLifecycleOwner: LifecycleOwner {
  private val lifecycleRegistry = LifecycleRegistry(this)

  override fun getLifecycle(): Lifecycle {
    return lifecycleRegistry
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.O_MR1]
)
class ViewControllerManagerTest {

  @Test
  fun testViewControllerLifecycles() {
    val activityA = ActivityA()

    // 所有的生命周期都要被正确调用
    val lifecycleOwner = SimpleLifecycleOwner()
    val controllerManager = ViewControllerManagerImpl(lifecycleOwner, activityA ,RuntimeEnvironment.getApplication(),
    object : ViewHost {
      override fun <T : View> findViewById(resId: Int): T? {
        return activityA.findViewById(resId)
      }

      override fun isChildView(view: View): Boolean {
        return ViewHost.isChildView(activityA.window.decorView, view)
      }
    })
    lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED

    val flags = BooleanArray(6) { false }
    val controller = object: ViewController() {
      override fun onCreate() {
        flags[Lifecycle.Event.ON_CREATE.ordinal] = true
      }

      override fun onResume() {
        flags[Lifecycle.Event.ON_RESUME.ordinal] = true
      }

      override fun onStart() {
        flags[Lifecycle.Event.ON_START.ordinal] = true
      }

      override fun onPause() {
        flags[Lifecycle.Event.ON_PAUSE.ordinal] = true
      }

      override fun onStop() {
        flags[Lifecycle.Event.ON_STOP.ordinal] = true
      }

      override fun onDestroy() {
        flags[Lifecycle.Event.ON_DESTROY.ordinal] = true
      }
    }
    controllerManager.addViewController(0, controller)

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
    lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
    lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED

    flags.forEach {
      assertTrue(it)
    }
  }

  @Test
  fun testNestedViewControllerLifecycles() {
    val activityA = ActivityA()
    // 生命周期的调用顺序
    val parentFlags = BooleanArray(6) { false }
    val childFlags = BooleanArray(6) { false }

    val lifecycleOwner = SimpleLifecycleOwner()
    val controllerManager = ViewControllerManagerImpl(lifecycleOwner, activityA, RuntimeEnvironment.getApplication(),
    object : ViewHost {
      override fun <T : View> findViewById(resId: Int): T? {
        return activityA.findViewById(resId)
      }

      override fun isChildView(view: View): Boolean {
        return ViewHost.isChildView(activityA.window.decorView, view)
      }
    })

    val childController = object: ViewController() {
      override fun onCreate() {
        // parent  -> children
        childFlags[Lifecycle.Event.ON_CREATE.ordinal] = true
        assertTrue(parentFlags[Lifecycle.Event.ON_CREATE.ordinal])
      }

      override fun onResume() {
        // parent  -> children
        childFlags[Lifecycle.Event.ON_RESUME.ordinal] = true
        assertTrue(parentFlags[Lifecycle.Event.ON_RESUME.ordinal])
      }

      override fun onStart() {
        // parent  -> children
        childFlags[Lifecycle.Event.ON_START.ordinal] = true
        assertTrue(parentFlags[Lifecycle.Event.ON_START.ordinal])
      }

      override fun onStop() {
        // children  -> parent
        childFlags[Lifecycle.Event.ON_STOP.ordinal] = true
        assertFalse(parentFlags[Lifecycle.Event.ON_STOP.ordinal])
      }

      override fun onPause() {
        // children  -> parent
        childFlags[Lifecycle.Event.ON_PAUSE.ordinal] = true
        assertFalse(parentFlags[Lifecycle.Event.ON_PAUSE.ordinal])
      }

      override fun onDestroy() {
        // children  -> parent
        childFlags[Lifecycle.Event.ON_DESTROY.ordinal] = true
        assertFalse(parentFlags[Lifecycle.Event.ON_DESTROY.ordinal])
      }
    }

    val parentController = object: ViewController() {
      override fun onCreate() {
        parentFlags[Lifecycle.Event.ON_CREATE.ordinal] = true
        addViewController(0, childController)
      }

      override fun onResume() {
        parentFlags[Lifecycle.Event.ON_RESUME.ordinal] = true
      }

      override fun onStart() {
        parentFlags[Lifecycle.Event.ON_START.ordinal] = true
      }

      override fun onStop() {
        parentFlags[Lifecycle.Event.ON_STOP.ordinal] = true
      }

      override fun onPause() {
        parentFlags[Lifecycle.Event.ON_PAUSE.ordinal] = true
      }

      override fun onDestroy() {
        parentFlags[Lifecycle.Event.ON_DESTROY.ordinal] = true
      }
    }

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
    controllerManager.addViewController(0, parentController)

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
    lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED

    parentFlags.forEach {
      assertTrue(it)
    }

    childFlags.forEach {
      assertTrue(it)
    }
  }

  /**
   * 测试在回调子vc生命周期过程中add/remove vc
   * 1. 无ConcurrentModifyException
   * 2. 父/子vc生命周期不会重复调用
   * 3. removeViewController之后父/子VC getCurrentState与回调对应的生命周期是一致的
   */
  @Test
  fun testViewControllerConcurrentModification() {
    val activityA = ActivityA()

    val lifecycleOwner = SimpleLifecycleOwner()
    val controllerManager = ViewControllerManagerImpl(lifecycleOwner, activityA, RuntimeEnvironment.getApplication(),
      object : ViewHost {
        override fun <T : View> findViewById(resId: Int): T? {
          return activityA.findViewById(resId)
        }

        override fun isChildView(view: View): Boolean {
          return ViewHost.isChildView(activityA.window.decorView, view)
        }
      })

    val parentFlags = IntArray(6) { 0 }
    val firstChildFlags = IntArray(6) { 0 }
    val secondChildFlags = IntArray(6) { 0 }
    val thirdChildFlags = IntArray(6) { 0 }
    val forthChildFlags = IntArray(6) { 0 }

    val parentController = object : ViewController() {
      override fun onCreate() {
        parentFlags[Lifecycle.Event.ON_CREATE.ordinal] += 1
        assertEquals(Lifecycle.State.CREATED, this.lifecycle.currentState)
        addViewController(ChildController(parentFlags, firstChildFlags, {
          addViewController(ChildController(parentFlags, forthChildFlags, null, null))
        }, null))
        addViewController(ChildController(parentFlags, secondChildFlags, null) {
          removeViewController(it)
        })
        addViewController(ChildController(parentFlags, thirdChildFlags, null, null))
        // onCreate中add完子vc之后,子vc的生命周期应该要同步走完onCreate
        listOf(firstChildFlags, secondChildFlags, thirdChildFlags).forEach {
          assertEquals(1, it[Lifecycle.Event.ON_CREATE.ordinal])
        }
      }

      override fun onStart() {
        parentFlags[Lifecycle.Event.ON_START.ordinal] += 1
        assertEquals(Lifecycle.State.STARTED, this.lifecycle.currentState)
      }

      override fun onResume() {
        parentFlags[Lifecycle.Event.ON_RESUME.ordinal] += 1
        assertEquals(Lifecycle.State.RESUMED, this.lifecycle.currentState)
      }

      override fun onPause() {
        parentFlags[Lifecycle.Event.ON_PAUSE.ordinal] += 1
        assertEquals(Lifecycle.State.STARTED, this.lifecycle.currentState)
      }

      override fun onStop() {
        parentFlags[Lifecycle.Event.ON_STOP.ordinal] += 1
        assertEquals(Lifecycle.State.CREATED, this.lifecycle.currentState)
      }

      override fun onDestroy() {
        parentFlags[Lifecycle.Event.ON_DESTROY.ordinal] += 1
        assertEquals(Lifecycle.State.DESTROYED, this.lifecycle.currentState)
      }
    }

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
    controllerManager.addViewController(parentController)

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
    controllerManager.removeViewController(parentController)

    listOf(parentFlags, firstChildFlags, secondChildFlags, thirdChildFlags, forthChildFlags).forEach {
      it.forEach { flag ->
        assertEquals(1, flag)
      }
    }
  }

  private class ChildController(
    private val parentFlags: IntArray,
    private val childFlags: IntArray,
    private val onStartCallback: ((ViewController) -> Unit)?,
    private val onDestroyCallback: ((ViewController) -> Unit)?,
    ) : ViewController() {
    override fun onCreate() {
      super.onCreate()
      childFlags[Lifecycle.Event.ON_CREATE.ordinal] += 1
      assertEquals(1, parentFlags[Lifecycle.Event.ON_CREATE.ordinal])
      assertEquals(Lifecycle.State.CREATED, this.lifecycle.currentState)
    }

    override fun onStart() {
      super.onStart()
      childFlags[Lifecycle.Event.ON_START.ordinal] += 1
      assertEquals(1, parentFlags[Lifecycle.Event.ON_START.ordinal])
      assertEquals(Lifecycle.State.STARTED, this.lifecycle.currentState)
      onStartCallback?.invoke(this)
    }

    override fun onResume() {
      super.onResume()
      childFlags[Lifecycle.Event.ON_RESUME.ordinal] += 1
      assertEquals(Lifecycle.State.RESUMED, this.lifecycle.currentState)
      assertEquals(1, parentFlags[Lifecycle.Event.ON_RESUME.ordinal])
    }

    override fun onPause() {
      super.onPause()
      childFlags[Lifecycle.Event.ON_PAUSE.ordinal] += 1
      assertEquals(Lifecycle.State.STARTED, this.lifecycle.currentState)
      assertEquals(0, parentFlags[Lifecycle.Event.ON_PAUSE.ordinal])
    }

    override fun onStop() {
      childFlags[Lifecycle.Event.ON_STOP.ordinal] += 1
      assertEquals(Lifecycle.State.CREATED, this.lifecycle.currentState)
      assertEquals(0, parentFlags[Lifecycle.Event.ON_STOP.ordinal])
    }

    override fun onDestroy() {
      childFlags[Lifecycle.Event.ON_DESTROY.ordinal] += 1
      assertEquals(Lifecycle.State.DESTROYED, this.lifecycle.currentState)
      assertEquals(0, parentFlags[Lifecycle.Event.ON_DESTROY.ordinal])
      onDestroyCallback?.invoke(this)
    }
  }
}