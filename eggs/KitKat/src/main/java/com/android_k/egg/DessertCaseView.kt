package com.android_k.egg

import android.animation.*
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.*
import kotlin.collections.HashSet

class DessertCaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    companion object {
        private val TAG = DessertCaseView::class.java.simpleName
        private const val DEBUG = false

        const val START_DELAY = 5000
        const val DELAY = 2000
        const val DURATION = 500

        private const val TAG_POS = 0x2000001
        private const val TAG_SPAN = 0x2000002

        val SCALE = 0.25f

        private const val PROB_2X = 0.33f
        private const val PROB_3X = 0.1f
        private const val PROB_4X = 0.01f

        fun frand(): Float = Math.random().toFloat()
        fun frand(a: Float, b: Float): Float = frand() * (b - a) + a
        fun irand(a: Int, b: Int): Int = frand(a.toFloat(), b.toFloat()).toInt()
    }

    private var mStarted = false
    private var mCellSize: Int
    private var mWidth = 0
    private var mHeight = 0
    private var mRows = 0
    private var mColumns = 0

    private var mCells: Array<View?> = emptyArray()
    private val mFreeList = HashSet<Point>()
    private val mHandler = Handler()

    private val mDrawables = SparseArray<Drawable>()

    private val hsv = floatArrayOf(0f, 1f, .85f)

    init {
        val res: Resources = resources
        mCellSize = res.getDimensionPixelSize(R.dimen.k_dessert_case_cell_size)

        val opts = BitmapFactory.Options().apply {
            if (mCellSize < 512) inSampleSize = 2
            inMutable = true
        }

        var loaded: Bitmap? = null

        val lists = arrayOf(
            PASTRIES,
            RARE_PASTRIES,
            XRARE_PASTRIES,
            XXRARE_PASTRIES
        )

        for (list in lists) {
            for (resid in list) {
                opts.inBitmap = loaded
                loaded = BitmapFactory.decodeResource(res, resid, opts)
                val d = BitmapDrawable(res, convertToAlphaMask(loaded!!)).apply {
                    colorFilter = ColorMatrixColorFilter(ALPHA_MASK)
                    setBounds(0, 0, mCellSize, mCellSize)
                }
                mDrawables.put(resid, d)
            }
        }
    }

    private fun convertToAlphaMask(b: Bitmap): Bitmap {
        val a = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ALPHA_8)
        val c = Canvas(a)
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(MASK)
        }
        c.drawBitmap(b, 0f, 0f, pt)
        return a
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

    private val mJuggle = object : Runnable {
        override fun run() {
            val N = childCount
            if (N > 0) {
                val child = getChildAt((Math.random() * N).toInt())
                place(child, true)
            }
            fillFreeList()

            if (mStarted) {
                mHandler.postDelayed(this, DELAY.toLong())
            }
        }
    }

    private fun randomColor(): Int {
        val COLORS = 12
        hsv[0] = irand(0, COLORS) * (360f / COLORS)
        return Color.HSVToColor(hsv)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (mWidth == w && mHeight == h) return

        val wasStarted = mStarted
        if (wasStarted) stop()

        mWidth = w
        mHeight = h

        removeAllViewsInLayout()
        mFreeList.clear()

        mRows = mHeight / mCellSize
        mColumns = mWidth / mCellSize

        mCells = arrayOfNulls(mRows * mColumns)

        var scaleS = 1f
        if (mWidth > 0 && mHeight > 0 && mCellSize > 0) {
            scaleS = maxOf(
                mWidth / (mCellSize * mColumns.toFloat()),
                mHeight / (mCellSize * mRows.toFloat())
            )
        }

        val scale = SCALE * scaleS
        scaleX = scale
        scaleY = scale

        translationX = 0.5f * (mWidth - mCellSize * mColumns) * scale
        translationY = 0.5f * (mHeight - mCellSize * mRows) * scale

        for (j in 0 until mRows) {
            for (i in 0 until mColumns) {
                mFreeList.add(Point(i, j))
            }
        }

        if (wasStarted) start()
    }

    fun fillFreeList(animationLen: Int = DURATION) {
        val ctx = context
        val lp = LayoutParams(mCellSize, mCellSize)

        while (mFreeList.isNotEmpty()) {
            val pt = mFreeList.first()
            mFreeList.remove(pt)

            val i = pt.x
            val j = pt.y

            if (mCells[j * mColumns + i] != null) continue

            val v = ImageView(ctx).apply {
                setOnClickListener {
                    place(this, true)
                    postDelayed({ fillFreeList() }, (DURATION / 2).toLong())
                }
                setBackgroundColor(randomColor())
            }

            val which = frand()
            val d: Drawable? = when {
                which < 0.0005f -> mDrawables.get(pick(XXRARE_PASTRIES))
                which < 0.005f -> mDrawables.get(pick(XRARE_PASTRIES))
                which < 0.5f -> mDrawables.get(pick(RARE_PASTRIES))
                which < 0.7f -> mDrawables.get(pick(PASTRIES))
                else -> null
            }

            d?.let { v.overlay.add(it) }

            addView(v, lp)
            place(v, pt, false)
        }
    }

    private fun <T> pick(array: Array<T>): T =
        array[(Math.random() * array.size).toInt()]

    private fun pick(array: IntArray): Int =
        array[(Math.random() * array.size).toInt()]

    private fun pick(sa: SparseArray<Drawable>): Drawable =
        sa.valueAt((Math.random() * sa.size()).toInt())
}