package dora.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText

/**
 * 文本编辑器控件。
 *
 * 支持：
 * - 加粗
 * - 斜体
 * - 文字颜色
 * - 字号
 * - 有序列表
 * - 无序列表
 * - 多行文本编辑
 * - 选中文字局部设置样式
 */
class DoraTextEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_TEXT_SIZE = 16f
        private const val DEFAULT_TOOLBAR_HEIGHT = 48
        private const val DEFAULT_TEXT_COLOR = 0xFF333333.toInt()
        private const val DEFAULT_HINT_COLOR = 0xFF999999.toInt()
        private const val DEFAULT_DIVIDER_COLOR = 0xFFE5E5E5.toInt()
    }

    private val toolbarScrollView = HorizontalScrollView(context)
    private val toolbar = LinearLayout(context)
    private val editor = AppCompatEditText(context)

    private var toolbarVisible = true

    @ColorInt
    private var editorTextColor = DEFAULT_TEXT_COLOR

    @ColorInt
    private var editorHintColor = DEFAULT_HINT_COLOR

    @ColorInt
    private var toolbarTextColor = DEFAULT_TEXT_COLOR

    @ColorInt
    private var dividerColor = DEFAULT_DIVIDER_COLOR

    private var editorTextSize = DEFAULT_TEXT_SIZE

    private var hintText = "请输入内容"

    private var currentTextColor: Int? = null
    private var currentTextSize: Float? = null
    private var currentBold = false
    private var currentItalic = false

    private var orderedListMode = false
    private var unorderedListMode = false

    private var suppressTextWatcher = false

    init {
        orientation = VERTICAL

        setupToolbar()
        setupEditor()

        addView(
            toolbarScrollView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(DEFAULT_TOOLBAR_HEIGHT)
            )
        )

        addView(
            editor,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun setupToolbar() {
        toolbar.orientation = HORIZONTAL
        toolbar.gravity = Gravity.CENTER_VERTICAL

        toolbarScrollView.isHorizontalScrollBarEnabled = false
        toolbarScrollView.addView(
            toolbar,
            FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )

        addToolbarButton("B") {
            toggleBold()
        }

        addToolbarButton("I") {
            toggleItalic()
        }

        addToolbarButton("A") {
            showColorMenu(it)
        }

        addToolbarButton("16") {
            showTextSizeMenu(it)
        }

        addToolbarButton("•") {
            toggleUnorderedList()
        }

        addToolbarButton("1.") {
            toggleOrderedList()
        }
    }

    private fun setupEditor() {
        editor.setTextColor(editorTextColor)
        editor.setHintTextColor(editorHintColor)
        editor.textSize = editorTextSize
        editor.hint = hintText

        editor.gravity = Gravity.TOP or Gravity.START

        editor.setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(16)
        )

        editor.setBackgroundColor(Color.TRANSPARENT)

        editor.setSingleLine(false)

        editor.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        editor.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateCurrentStyleFromSelection()
            }
        }
    }

    private fun addToolbarButton(
        text: String,
        onClick: (View) -> Unit
    ) {
        val button = TextView(context)

        button.text = text
        button.textSize = 15f
        button.setTextColor(toolbarTextColor)
        button.gravity = Gravity.CENTER
        button.typeface = Typeface.DEFAULT_BOLD

        button.setPadding(
            dp(14),
            0,
            dp(14),
            0
        )

        button.minWidth = dp(44)

        button.setOnClickListener(onClick)

        toolbar.addView(
            button,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    /**
     * 加粗
     */
    fun toggleBold() {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start == -1 || end == -1) {
            return
        }

        if (start == end) {
            currentBold = !currentBold
            return
        }

        val editable = editor.text ?: return

        val existing = editable.getSpans(
            start,
            end,
            StyleSpan::class.java
        )

        var hasBold = false

        for (span in existing) {
            if (span.style == Typeface.BOLD ||
                span.style == Typeface.BOLD_ITALIC
            ) {
                hasBold = true
                break
            }
        }

        if (hasBold) {
            removeBold(
                editable,
                start,
                end
            )
        } else {
            editable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 斜体
     */
    fun toggleItalic() {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start == -1 || end == -1) {
            return
        }

        if (start == end) {
            currentItalic = !currentItalic
            return
        }

        val editable = editor.text ?: return

        val existing = editable.getSpans(
            start,
            end,
            StyleSpan::class.java
        )

        var hasItalic = false

        for (span in existing) {
            if (span.style == Typeface.ITALIC ||
                span.style == Typeface.BOLD_ITALIC
            ) {
                hasItalic = true
                break
            }
        }

        if (hasItalic) {
            removeItalic(
                editable,
                start,
                end
            )
        } else {
            editable.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun removeBold(
        editable: Editable,
        start: Int,
        end: Int
    ) {
        val spans = editable.getSpans(
            start,
            end,
            StyleSpan::class.java
        )

        spans.forEach { span ->
            if (span.style == Typeface.BOLD) {
                editable.removeSpan(span)
            }
        }
    }

    private fun removeItalic(
        editable: Editable,
        start: Int,
        end: Int
    ) {
        val spans = editable.getSpans(
            start,
            end,
            StyleSpan::class.java
        )

        spans.forEach { span ->
            if (span.style == Typeface.ITALIC) {
                editable.removeSpan(span)
            }
        }
    }

    /**
     * 设置文字颜色
     */
    fun setSelectionTextColor(
        @ColorInt color: Int
    ) {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start == -1 || end == -1) {
            return
        }

        if (start == end) {
            currentTextColor = color
            return
        }

        editor.text?.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 设置文字大小
     *
     * 单位 sp
     */
    fun setSelectionTextSize(size: Float) {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start == -1 || end == -1) {
            return
        }

        if (start == end) {
            currentTextSize = size
            return
        }

        editor.text?.setSpan(
            AbsoluteSizeSpan(
                size.toInt(),
                true
            ),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 无序列表
     */
    fun toggleUnorderedList() {
        val start = editor.selectionStart
        val end = editor.selectionEnd

        if (start == -1 || end == -1) {
            return
        }

        val editable = editor.text ?: return

        val lineStart = getLineStart(
            editable,
            start
        )

        val lineEnd = getLineEnd(
            editable,
            end
        )

        if (hasBulletSpan(
                editable,
                lineStart,
                lineEnd
            )
        ) {
            removeBulletSpans(
                editable,
                lineStart,
                lineEnd
            )

            unorderedListMode = false
        } else {
            applyBulletSpans(
                editable,
                lineStart,
                lineEnd
            )

            unorderedListMode = true
            orderedListMode = false
        }
    }

    private fun applyBulletSpans(
        editable: Editable,
        start: Int,
        end: Int
    ) {
        var lineStart = start

        while (lineStart < end) {

            val lineEnd = findNextLineEnd(
                editable,
                lineStart,
                end
            )

            editable.setSpan(
                BulletSpan(
                    dp(8),
                    editorTextColor
                ),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (lineEnd >= editable.length) {
                break
            }

            lineStart = lineEnd + 1
        }
    }

    private fun removeBulletSpans(
        editable: Editable,
        start: Int,
        end: Int
    ) {
        val spans = editable.getSpans(
            start,
            end,
            BulletSpan::class.java
        )

        spans.forEach {
            editable.removeSpan(it)
        }
    }

    private fun hasBulletSpan(
        editable: Editable,
        start: Int,
        end: Int
    ): Boolean {
        return editable.getSpans(
            start,
            end,
            BulletSpan::class.java
        ).isNotEmpty()
    }

    /**
     * 有序列表
     */
    fun toggleOrderedList() {
        val editable = editor.text ?: return

        val selectionStart = editor.selectionStart
        val selectionEnd = editor.selectionEnd

        if (selectionStart < 0 ||
            selectionEnd < 0
        ) {
            return
        }

        val start = getLineStart(
            editable,
            selectionStart
        )

        val end = getLineEnd(
            editable,
            selectionEnd
        )

        val existing = editable.getSpans(
            start,
            end,
            OrderedListSpan::class.java
        )

        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }

            orderedListMode = false
            return
        }

        var lineStart = start
        var index = 1

        while (lineStart < end) {

            val lineEnd = findNextLineEnd(
                editable,
                lineStart,
                end
            )

            editable.setSpan(
                OrderedListSpan(
                    index
                ),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            index++

            if (lineEnd >= editable.length) {
                break
            }

            lineStart = lineEnd + 1
        }

        orderedListMode = true
        unorderedListMode = false
    }

    private fun getLineStart(
        editable: Editable,
        position: Int
    ): Int {
        var index = position.coerceIn(
            0,
            editable.length
        )

        while (index > 0 &&
            editable[index - 1] != '\n'
        ) {
            index--
        }

        return index
    }

    private fun getLineEnd(
        editable: Editable,
        position: Int
    ): Int {
        var index = position.coerceIn(
            0,
            editable.length
        )

        while (index < editable.length &&
            editable[index] != '\n'
        ) {
            index++
        }

        return index
    }

    private fun findNextLineEnd(
        editable: Editable,
        start: Int,
        maxEnd: Int
    ): Int {
        var index = start

        while (
            index < editable.length &&
            index < maxEnd &&
            editable[index] != '\n'
        ) {
            index++
        }

        return index
    }

    /**
     * 颜色菜单
     */
    private fun showColorMenu(anchor: View) {
        val colors = intArrayOf(
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            0xFFFF9800.toInt(),
            0xFF9C27B0.toInt()
        )

        showSimplePopup(
            anchor,
            colors.map { color ->
                color to ""
            }
        ) { color, _ ->
            setSelectionTextColor(color)
        }
    }

    /**
     * 字号菜单
     */
    private fun showTextSizeMenu(anchor: View) {
        val sizes = arrayOf(
            12f,
            14f,
            16f,
            18f,
            20f,
            24f,
            28f
        )

        showSimplePopup(
            anchor,
            sizes.map {
                Color.TRANSPARENT to it.toString()
            }
        ) { _, value ->
            setSelectionTextSize(
                value.toFloat()
            )
        }
    }

    private fun showSimplePopup(
        anchor: View,
        items: List<Pair<Int, String>>,
        callback: (Int, String) -> Unit
    ) {
        val popup = android.widget.PopupWindow(
            context
        )

        val container = LinearLayout(context)

        container.orientation = HORIZONTAL
        container.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        items.forEach { item ->

            val itemView = TextView(context)

            if (item.second.isEmpty()) {
                itemView.text = "●"
                itemView.setTextColor(item.first)
            } else {
                itemView.text = item.second
                itemView.setTextColor(toolbarTextColor)
            }

            itemView.gravity = Gravity.CENTER
            itemView.textSize = 14f

            itemView.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
            )

            itemView.setOnClickListener {
                callback(
                    item.first,
                    item.second
                )

                popup.dismiss()
            }

            container.addView(itemView)
        }

        popup.contentView = container
        popup.width = LayoutParams.WRAP_CONTENT
        popup.height = LayoutParams.WRAP_CONTENT
        popup.isFocusable = true
        popup.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.WHITE
            )
        )

        popup.elevation = dp(6).toFloat()

        popup.showAsDropDown(
            anchor,
            0,
            -dp(48)
        )
    }

    private fun updateCurrentStyleFromSelection() {
        val editable = editor.text ?: return

        val position = editor.selectionStart

        if (position < 0 ||
            position >= editable.length
        ) {
            return
        }

        val styleSpans = editable.getSpans(
            position,
            position + 1,
            StyleSpan::class.java
        )

        currentBold = false
        currentItalic = false

        styleSpans.forEach {
            if (it.style == Typeface.BOLD ||
                it.style == Typeface.BOLD_ITALIC
            ) {
                currentBold = true
            }

            if (it.style == Typeface.ITALIC ||
                it.style == Typeface.BOLD_ITALIC
            ) {
                currentItalic = true
            }
        }
    }

    /**
     * 设置编辑器文字
     */
    fun setContent(value: CharSequence?) {
        editor.setText(value)
    }

    /**
     * 获取编辑器内容
     */
    fun getContent(): CharSequence {
        return editor.text ?: ""
    }

    /**
     * 获取 Editable
     */
    fun getEditable(): Editable? {
        return editor.text
    }

    /**
     * 设置提示文字
     */
    fun setHintText(value: String) {
        hintText = value
        editor.hint = value
    }

    /**
     * 设置文字颜色
     */
    fun setEditorTextColor(
        @ColorInt color: Int
    ) {
        editorTextColor = color
        editor.setTextColor(color)
    }

    /**
     * 设置默认文字大小
     */
    fun setEditorTextSize(size: Float) {
        editorTextSize = size
        editor.textSize = size
    }

    /**
     * 显示/隐藏工具栏
     */
    fun setToolbarVisible(visible: Boolean) {
        toolbarVisible = visible

        toolbarScrollView.visibility =
            if (visible) {
                VISIBLE
            } else {
                GONE
            }
    }

    /**
     * 是否显示工具栏
     */
    fun isToolbarVisible(): Boolean {
        return toolbarVisible
    }

    /**
     * 获取内部 EditText
     */
    fun getEditText(): EditText {
        return editor
    }

    /**
     * 设置选区
     */
    fun setSelection(
        start: Int,
        end: Int
    ) {
        editor.setSelection(
            start.coerceAtLeast(0),
            end.coerceAtMost(
                editor.length()
            )
        )
    }

    /**
     * 清空内容
     */
    fun clear() {
        editor.text?.clear()
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density +
                0.5f
            ).toInt()
    }

    /**
     * 有序列表 Span
     */
    private class OrderedListSpan(
        private val number: Int
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 50
        }

        override fun drawLeadingMargin(
            c: android.graphics.Canvas,
            p: Paint,
            x: Int,
            dir: Int,
            top: Int,
            baseline: Int,
            bottom: Int,
            text: CharSequence,
            start: Int,
            end: Int,
            first: Boolean,
            layout: android.text.Layout
        ) {
            if (!first) {
                return
            }

            p.color = p.color
            p.textSize = p.textSize

            c.drawText(
                "$number.",
                x.toFloat(),
                baseline.toFloat(),
                p
            )
        }
    }
}