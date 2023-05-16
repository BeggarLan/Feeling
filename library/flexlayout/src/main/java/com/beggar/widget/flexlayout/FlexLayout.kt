package com.beggar.widget.flexlayout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi

/**
 * author: BeggarLan
 * created on: 2023/3/28 11:21 AM
 * description: 一行可以放多个view，可以多行的ViewGroup
 */
class FlexLayout : ViewGroup {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
      : super(context, attrs, defStyleAttr)
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
      : super(context, attrs, defStyleAttr, defStyleRes)

  // 横向item之间的间距
  private val horizontalSpacePx = 10;

  // 竖向item之间的间距
  private val verticalSpacePx = 10;

  // 所有的view，每一行的都放到一个list中
  private val allViews: ArrayList<List<View>> = ArrayList()

  // 每一行的高度
  private val allLineHeightPx = ArrayList<Int>()

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    allViews.clear()
    allLineHeightPx.clear()

    // 自己的宽高
    val selfWith = MeasureSpec.getSize(widthMeasureSpec)
    val selfHeight = MeasureSpec.getSize(heightMeasureSpec)

    // 自己需要的宽高
    var needWidth = 0
    var needHeight = 0

    // 当前行的view
    var lineViews = ArrayList<View>()
    // 当前行的宽
    var lineWidth = paddingLeft + paddingRight
    // 当前行的高
    var lineHeight = 0

    for (i in 0 until childCount) {
      val childView = getChildAt(i)
      val childLayoutParams = childView.layoutParams
      // 计算出孩子的MeasureSpec
      val childWidthMeasureSpec =
        getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, childLayoutParams.width)
      val childHeightMeasureSpec =
        getChildMeasureSpec(heightMeasureSpec, paddingBottom + paddingTop, childLayoutParams.width)
      childView.measure(childWidthMeasureSpec, childHeightMeasureSpec)

      // 经过测量后，孩子的宽高
      val childMeasuredWidth = childView.measuredWidth
      val childMeasuredHeight = childView.measuredHeight

      // 这一行放不下了，要换行
      if (lineWidth + childMeasuredWidth + horizontalSpacePx > selfWith) {
        needWidth = Math.max(needWidth, lineWidth)
        needHeight = needHeight + lineHeight + verticalSpacePx

        // 当前行的所有view
        allViews.add(lineViews)
        // 当前行的高度
        allLineHeightPx.add(lineHeight)

        lineViews = ArrayList<View>()
        lineWidth = paddingLeft + paddingRight
        lineHeight = 0
      }

      lineViews.add(childView)
      lineWidth = lineWidth + childMeasuredWidth + horizontalSpacePx
      lineHeight = Math.max(lineHeight, childMeasuredHeight)
    }

    val selfWidthMode = MeasureSpec.getMode(widthMeasureSpec)
    val selfHeightMode = MeasureSpec.getMode(heightMeasureSpec)

    // 如果自己设置了精确的宽高，那么使用自己设置的即可
    val realWidth = if (selfWidthMode == MeasureSpec.EXACTLY) selfWith else needWidth
    val realHeight = if (selfHeightMode == MeasureSpec.EXACTLY) selfHeight else needHeight

    setMeasuredDimension(realWidth, realHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    var curl = paddingLeft
    var curt = paddingTop

    for (i in 0 until allViews.size) {
      val currentLineViews = allViews.get(i)
      currentLineViews.forEach { view ->
        run {
          // 子view布局
          view.layout(curl, curt, curl + view.measuredWidth, curt + view.measuredHeight)
          // 更新
          curl += view.measuredWidth + horizontalSpacePx
        }
      }
      // 开始新的一行
      curl = paddingLeft
      curt = curt + allLineHeightPx.get(i) + verticalSpacePx
    }
  }

}