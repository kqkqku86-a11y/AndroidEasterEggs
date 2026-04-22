package com.android_k.egg

import android.animation.*
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.*
import kotlin.math.max

class DessertCaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    companion object {
        const val START_DELAY = 5000
        const val DELAY = 2000
        const val DURATION = 500

        const val TAG_POS = 0x2000001
        const val TAG_SPAN = 0x2000002

        const val SCALE = 0.25f
    }

    private val mDrawables = SparseArray<Drawable>()
    private val mFreeList = HashSet<Point>()
    private val mHandler = Handler()

    private var mStarted = false
    private var mCellSize = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRows = 0
    private var mColumns = 0
    private var mCells: Array<View?>? = null

    init {
        val res: Resources = resources
        mCellSize = res.getDimensionPixelSize(R.dimen.k_dessert_case_cell_size)

        val opts = BitmapFactory.Options().apply {
            inMutable = true
            if (mCellSize < 512) inSampleSize = 2
        }

        var loaded: Bitmap? = null

        val lists = arrayOf(
            PASTRIES, RARE_PASTRIES, XRARE_PASTRIES, XXRARE_PASTRIES
        )

        for (list in lists) {
            for (resid in list) {
                opts.inBitmap = loaded
                loaded = BitmapFactory.decodeResource(res, resid, opts)

                val d = BitmapDrawable(res, convertToAlphaMask(loaded!!)).apply {
                    setBounds(0, 0, mCellSize, mCellSize)
                }

                mDrawables.append(resid, d)
            }
        }
    }

    private val mJuggle = object : Runnable {
        override fun run() {
            val N = childCount
            val child = getChildAt((Math.random() * N).toInt())
            place(child, true)

            fillFreeList()

            if (mStarted) {
                mHandler.postDelayed(this, DELAY.toLong())
            }
        }
    }

    fun start() {
        if (!mStarted) {
            mStarted = true
            fillFreeList(DURATION * 4)
        }
        mHandler.postDelayed(mJuggle, START_DELAY.toLong())
    }

    fun stop() {
        mStarted = false
        mHandler.removeCallbacks(mJuggle)
    }

    private fun randomColor(): Int {
        val hsv = floatArrayOf((Math.random() * 360).toFloat(), 1f, 0.85f)
        return Color.HSVToColor(hsv)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = w
        mHeight = h

        removeAllViews()
        mFreeList.clear()

        mRows = mHeight / mCellSize
        mColumns = mWidth / mCellSize

        mCells = arrayOfNulls(mRows * mColumns)

        val scale = SCALE * max(
            mWidth / (mCellSize * mColumns.toFloat()),
            mHeight / (mCellSize * mRows.toFloat())
        )

        scaleX = scale
        scaleY = scale

        for (j in 0 until mRows) {
            for (i in 0 until mColumns) {
                mFreeList.add(Point(i, j))
            }
        }
    }

    fun fillFreeList(animationLen: Int = DURATION) {
        val lp = LayoutParams(mCellSize, mCellSize)

        while (mFreeList.isNotEmpty()) {
            val pt = mFreeList.first()
            mFreeList.remove(pt)

            val v = ImageView(context).apply {
                setBackgroundColor(randomColor())
                setOnClickListener {
                    place(this, true)
                    postDelayed({ fillFreeList() }, DURATION / 2L)
                }
            }

            addView(v, lp)
            place(v, pt, false)

            if (animationLen > 0) {
                v.alpha = 0f
                v.animate().alpha(1f).setDuration(animationLen.toLong()).start()
            }
        }
    }

    fun place(v: View, animate: Boolean) {
        place(
            v,
            Point((Math.random() * mColumns).toInt(), (Math.random() * mRows).toInt()),
            animate
        )
    }

    fun place(v: View, pt: Point, animate: Boolean) {
        val i = pt.x
        val j = pt.y

        val scale = 1

        v.x = (i * mCellSize).toFloat()
        v.y = (j * mCellSize).toFloat()

        if (animate) {
            v.animate()
                .scaleX(scale.toFloat())
                .scaleY(scale.toFloat())
                .setDuration(DURATION.toLong())
                .start()
        }
    }

    companion object {
        private val PASTRIES = intArrayOf(
            R.drawable.k_dessert_kitkat,
            R.drawable.k_dessert_android
        )

        private val RARE_PASTRIES = intArrayOf(
            R.drawable.k_dessert_cupcake,
            R.drawable.k_dessert_donut
        )

        private val XRARE_PASTRIES = intArrayOf(
            R.drawable.k_dessert_petitfour
        )

        private val XXRARE_PASTRIES = intArrayOf(
            R.drawable.k_dessert_zombiegingerbread
        )

        private fun convertToAlphaMask(b: Bitmap): Bitmap {
            val a = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ALPHA_8)
            val c = Canvas(a)
            val pt = Paint(Paint.ANTI_ALIAS_FLAG)
            c.drawBitmap(b, 0f, 0f, pt)
            return a
        }
    }
}