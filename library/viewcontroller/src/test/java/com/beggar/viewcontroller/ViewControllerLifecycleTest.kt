package com.kuaishou.live.viewcontroller.lifecycle

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.beggar.viewcontroller.ViewController
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.O_MR1]
)
class ViewControllerLifecycleTest {

  @Test
  fun testViewControllerLifecycleOrder() {
    // 生命周期的调用顺序
    val parentFlags = BooleanArray(6) { false }
    val childFlags = BooleanArray(6) { false }

    val parentController = object: ViewController() {
      override fun onCreate() {
        // parent  -> children
        parentFlags[Lifecycle.Event.ON_CREATE.ordinal] = true
        assertFalse(childFlags[Lifecycle.Event.ON_CREATE.ordinal])
      }

      override fun onResume() {
        // parent  -> children
        parentFlags[Lifecycle.Event.ON_RESUME.ordinal] = true
        assertFalse(childFlags[Lifecycle.Event.ON_RESUME.ordinal])
      }

      override fun onStart() {
        // parent  -> children
        parentFlags[Lifecycle.Event.ON_START.ordinal] = true
        assertFalse(childFlags[Lifecycle.Event.ON_START.ordinal])
      }

      override fun onStop() {
        // children  -> parent
        parentFlags[Lifecycle.Event.ON_STOP.ordinal] = true
        assertTrue(childFlags[Lifecycle.Event.ON_STOP.ordinal])
      }

      override fun onPause() {
        // children  -> parent
        parentFlags[Lifecycle.Event.ON_PAUSE.ordinal] = true
        assertTrue(childFlags[Lifecycle.Event.ON_PAUSE.ordinal])
      }

      override fun onDestroy() {
        // children  -> parent
        parentFlags[Lifecycle.Event.ON_DESTROY.ordinal] = true
        assertTrue(childFlags[Lifecycle.Event.ON_DESTROY.ordinal])
      }
    }

    parentController.lifecycle.addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        childFlags[event.ordinal] = true
      }
    })
    // execute steps
    parentController.setCurrentState(Lifecycle.State.RESUMED)
    parentController.setCurrentState(Lifecycle.State.DESTROYED)

    parentFlags.forEach {
      assertTrue(it)
    }

    childFlags.forEach {
      assertTrue(it)
    }
  }
}