package dora.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import androidx.core.graphics.drawable.toDrawable

/**
 * Dora 富文本编辑器。
 *
 * 支持：
 *
 * - 加粗
 * - 斜体
 * - 文字颜色
 * - 字号
 * - 有序列表
 * - 无序列表
 * - 多行文本编辑
 * - 选中文字局部设置样式
 * - .dtxt 文件保存
 * - .dtxt 文件加载
 *
 * .dtxt 格式：
 *
 * {
 *   "format": "dtxt",
 *   "version": 1,
 *   "text": "...",
 *   "spans": [...]
 * }
 */
class DoraTextEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {

        /**
         * DTXT 文件格式标识。
         */
        private const val DTXT_FORMAT = "dtxt"

        /**
         * 当前 DTXT 文件版本。
         */
        private const val DTXT_VERSION = 1

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

    /**
     * 防止 setText/loadDtxt 等操作触发外部 TextWatcher。
     */
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

    /**
     * 初始化工具栏。
     */
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

    /**
     * 初始化编辑器。
     */
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
        editor.isSingleLine = false
        editor.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editor.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateCurrentStyleFromSelection()
            }
        }
    }

    /**
     * 添加工具栏按钮。
     */
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
     * 加粗。
     */
    fun toggleBold() {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start == -1 || end == -1) {
            return
        }
        /**
         * 没有选择文字时，只改变接下来输入的文字样式。
         */
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
            if (
                span.style == Typeface.BOLD ||
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
     * 斜体。
     */
    fun toggleItalic() {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start == -1 || end == -1) {
            return
        }
        /**
         * 没有选择文字时，只改变接下来输入的文字样式。
         */
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
            if (
                span.style == Typeface.ITALIC ||
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

    /**
     * 删除粗体。
     */
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
            when (span.style) {
                Typeface.BOLD -> {
                    editable.removeSpan(span)
                }
                Typeface.BOLD_ITALIC -> {
                    val spanStart = editable.getSpanStart(span)
                    val spanEnd = editable.getSpanEnd(span)
                    editable.removeSpan(span)
                    /**
                     * 保留斜体。
                     */
                    editable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        maxOf(start, spanStart),
                        minOf(end, spanEnd),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    /**
     * 删除斜体。
     */
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
            when (span.style) {
                Typeface.ITALIC -> {
                    editable.removeSpan(span)
                }
                Typeface.BOLD_ITALIC -> {
                    val spanStart = editable.getSpanStart(span)
                    val spanEnd = editable.getSpanEnd(span)
                    editable.removeSpan(span)
                    /**
                     * 保留粗体。
                     */
                    editable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        maxOf(start, spanStart),
                        minOf(end, spanEnd),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    /**
     * 设置文字颜色。
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
     * 设置文字大小。
     *
     * @param size 单位 sp
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
     * 无序列表。
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
                lineEnd)) {
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

    /**
     * 添加无序列表 Span。
     */
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

    /**
     * 删除无序列表 Span。
     */
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

    /**
     * 判断指定区域是否存在无序列表。
     */
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
     * 有序列表。
     */
    fun toggleOrderedList() {
        val editable = editor.text ?: return
        val selectionStart = editor.selectionStart
        val selectionEnd = editor.selectionEnd
        if (
            selectionStart < 0 ||
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

    /**
     * 获取行开始位置。
     */
    private fun getLineStart(
        editable: Editable,
        position: Int
    ): Int {
        var index = position.coerceIn(
            0,
            editable.length
        )
        while (
            index > 0 &&
            editable[index - 1] != '\n'
        ) {
            index--
        }
        return index
    }

    /**
     * 获取行结束位置。
     */
    private fun getLineEnd(
        editable: Editable,
        position: Int
    ): Int {
        var index = position.coerceIn(
            0,
            editable.length
        )
        while (
            index < editable.length &&
            editable[index] != '\n'
        ) {
            index++
        }
        return index
    }

    /**
     * 获取下一行结束位置。
     */
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
     * 显示颜色菜单。
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
     * 显示字号菜单。
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

    /**
     * 显示简单 Popup。
     */
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
            Color.WHITE.toDrawable()
        )
        popup.elevation = dp(6).toFloat()
        popup.showAsDropDown(
            anchor,
            0,
            -dp(48)
        )
    }

    /**
     * 根据当前光标位置更新当前样式。
     */
    private fun updateCurrentStyleFromSelection() {
        val editable = editor.text ?: return
        val position = editor.selectionStart
        if (
            position < 0 ||
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
            if (
                it.style == Typeface.BOLD ||
                it.style == Typeface.BOLD_ITALIC
            ) {
                currentBold = true
            }
            if (
                it.style == Typeface.ITALIC ||
                it.style == Typeface.BOLD_ITALIC
            ) {
                currentItalic = true
            }
        }
    }

    // ============================================================
    // DTXT
    // ============================================================

    /**
     * 保存当前编辑内容为 .dtxt 文件。
     *
     * 例如：
     *
     * val file = File(filesDir, "文章.dtxt")
     * editor.saveDtxt(file)
     *
     * .dtxt 使用 UTF-8 JSON。
     */
    @Throws(Exception::class)
    fun saveDtxt(file: File) {
        val editable = editor.text
            ?: SpannableStringBuilder()
        val root = JSONObject()
        root.put(
            "format",
            DTXT_FORMAT
        )
        root.put(
            "version",
            DTXT_VERSION
        )
        root.put(
            "text",
            editable.toString()
        )
        val spans = JSONArray()
        saveStyleSpans(
            editable,
            spans
        )
        saveColorSpans(
            editable,
            spans
        )
        saveSizeSpans(
            editable,
            spans
        )
        saveBulletSpans(
            editable,
            spans
        )
        saveOrderedListSpans(
            editable,
            spans
        )
        root.put(
            "spans",
            spans
        )
        file.parentFile?.mkdirs()
        file.writeText(
            root.toString(),
            StandardCharsets.UTF_8
        )
    }

    /**
     * 保存 StyleSpan。
     */
    private fun saveStyleSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        val styleSpans = editable.getSpans(
            0,
            editable.length,
            StyleSpan::class.java
        )
        styleSpans.forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start !in 0..<end) {
                return@forEach
            }
            when (span.style) {
                Typeface.BOLD -> {
                    spans.put(
                        JSONObject()
                            .put("type", "bold")
                            .put("start", start)
                            .put("end", end)
                    )
                }
                Typeface.ITALIC -> {
                    spans.put(
                        JSONObject()
                            .put("type", "italic")
                            .put("start", start)
                            .put("end", end)
                    )
                }
                Typeface.BOLD_ITALIC -> {
                    spans.put(
                        JSONObject()
                            .put("type", "bold_italic")
                            .put("start", start)
                            .put("end", end)
                    )
                }
            }
        }
    }

    /**
     * 保存文字颜色。
     */
    private fun saveColorSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        val colorSpans = editable.getSpans(
            0,
            editable.length,
            ForegroundColorSpan::class.java
        )
        colorSpans.forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start !in 0..<end) {
                return@forEach
            }
            spans.put(
                JSONObject()
                    .put("type", "color")
                    .put("start", start)
                    .put("end", end)
                    .put(
                        "value",
                        span.foregroundColor
                    )
            )
        }
    }

    /**
     * 保存字号。
     */
    private fun saveSizeSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        val sizeSpans = editable.getSpans(
            0,
            editable.length,
            AbsoluteSizeSpan::class.java
        )
        sizeSpans.forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start !in 0..<end) {
                return@forEach
            }
            spans.put(
                JSONObject()
                    .put("type", "size")
                    .put("start", start)
                    .put("end", end)
                    .put("value", span.size)
                    .put("dip", span.dip)
            )
        }
    }

    /**
     * 保存无序列表。
     */
    private fun saveBulletSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        val bulletSpans = editable.getSpans(
            0,
            editable.length,
            BulletSpan::class.java
        )
        bulletSpans.forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start !in 0..<end) {
                return@forEach
            }
            spans.put(
                JSONObject()
                    .put("type", "bullet")
                    .put("start", start)
                    .put("end", end)
            )
        }
    }

    /**
     * 保存有序列表。
     */
    private fun saveOrderedListSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        val orderedSpans = editable.getSpans(
            0,
            editable.length,
            OrderedListSpan::class.java
        )
        orderedSpans.forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start !in 0..<end) {
                return@forEach
            }
            spans.put(
                JSONObject()
                    .put("type", "ordered")
                    .put("start", start)
                    .put("end", end)
                    .put("number", span.number)
            )
        }
    }

    /**
     * 从 .dtxt 文件加载内容到编辑器。
     *
     * 例如：
     *
     * val file = File(filesDir, "文章.dtxt")
     * editor.loadDtxt(file)
     */
    @Throws(Exception::class)
    fun loadDtxt(file: File) {
        if (!file.exists()) {
            throw IllegalArgumentException(
                "DTXT 文件不存在：${file.absolutePath}"
            )
        }
        val json = file.readText(
            StandardCharsets.UTF_8
        )
        loadDtxtContent(json)
    }

    /**
     * 直接加载 DTXT 字符串。
     *
     * 方便网络、数据库等场景。
     */
    @Throws(Exception::class)
    fun loadDtxtContent(
        json: String
    ) {
        val root = JSONObject(json)
        val format = root.optString(
            "format",
            ""
        )
        if (format != DTXT_FORMAT) {
            throw IllegalArgumentException(
                "不是有效的 DTXT 文件"
            )
        }
        val version = root.optInt(
            "version",
            1
        )
        if (version != DTXT_VERSION) {
            throw IllegalArgumentException(
                "不支持的 DTXT 版本：$version"
            )
        }
        val text = root.optString(
            "text",
            ""
        )
        val builder = SpannableStringBuilder(
            text
        )
        val spans = root.optJSONArray(
            "spans"
        ) ?: JSONArray()
        for (i in 0 until spans.length()) {
            val item = spans.optJSONObject(i)
                ?: continue
            applyDtxtSpan(
                builder,
                item
            )
        }
        suppressTextWatcher = true
        try {
            editor.setText(
                builder
            )
            editor.setSelection(
                builder.length
            )
        } finally {
            suppressTextWatcher = false
        }
        updateCurrentStyleFromSelection()
    }

    /**
     * 根据 DTXT 数据恢复 Span。
     */
    private fun applyDtxtSpan(
        builder: SpannableStringBuilder,
        item: JSONObject
    ) {
        val type = item.optString(
            "type",
            ""
        )
        var start = item.optInt(
            "start",
            -1
        )
        var end = item.optInt(
            "end",
            -1
        )
        /**
         * 防止非法文件导致数组越界。
         */
        start = start.coerceIn(
            0,
            builder.length
        )
        end = end.coerceIn(
            0,
            builder.length
        )
        if (start >= end) {
            return
        }
        when (type) {
            "bold" -> {
                builder.setSpan(
                    StyleSpan(
                        Typeface.BOLD
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "italic" -> {
                builder.setSpan(
                    StyleSpan(
                        Typeface.ITALIC
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "bold_italic" -> {
                builder.setSpan(
                    StyleSpan(
                        Typeface.BOLD_ITALIC
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "color" -> {
                val color = item.optInt(
                    "value",
                    editorTextColor
                )
                builder.setSpan(
                    ForegroundColorSpan(
                        color
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "size" -> {
                val size = item.optInt(
                    "value",
                    DEFAULT_TEXT_SIZE.toInt()
                )
                val dip = item.optBoolean(
                    "dip",
                    true
                )
                builder.setSpan(
                    AbsoluteSizeSpan(
                        size,
                        dip
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "bullet" -> {
                builder.setSpan(
                    BulletSpan(
                        dp(8),
                        editorTextColor
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            "ordered" -> {
                val number = item.optInt(
                    "number",
                    1
                )
                builder.setSpan(
                    OrderedListSpan(
                        number
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    // ============================================================
    // Content API
    // ============================================================

    /**
     * 设置编辑器文字。
     *
     * 注意：
     * 如果 value 本身是 Spanned，
     * 其中的 Span 也会保留。
     */
    fun setContent(
        value: CharSequence?
    ) {
        editor.setText(value)
    }

    /**
     * 获取编辑器内容。
     *
     * 如果存在富文本 Span，
     * 返回的实际上是 Editable。
     */
    fun getContent(): CharSequence {
        return editor.text ?: ""
    }

    /**
     * 获取 Editable。
     */
    fun getEditable(): Editable? {
        return editor.text
    }

    /**
     * 获取纯文本。
     */
    fun getPlainText(): String {
        return editor.text?.toString() ?: ""
    }

    /**
     * 获取 DTXT JSON 内容。
     *
     * 不需要创建文件。
     */
    @Throws(Exception::class)
    fun getDtxtContent(): String {
        val editable = editor.text
            ?: SpannableStringBuilder()
        val root = JSONObject()
        root.put(
            "format",
            DTXT_FORMAT
        )
        root.put(
            "version",
            DTXT_VERSION
        )
        root.put(
            "text",
            editable.toString()
        )
        val spans = JSONArray()
        saveStyleSpans(
            editable,
            spans
        )
        saveColorSpans(
            editable,
            spans
        )
        saveSizeSpans(
            editable,
            spans
        )
        saveBulletSpans(
            editable,
            spans
        )
        saveOrderedListSpans(
            editable,
            spans
        )
        root.put(
            "spans",
            spans
        )
        return root.toString()
    }

    /**
     * 设置提示文字。
     */
    fun setHintText(
        value: String
    ) {
        hintText = value
        editor.hint = value
    }

    /**
     * 设置编辑器文字颜色。
     */
    fun setEditorTextColor(
        @ColorInt color: Int
    ) {
        editorTextColor = color
        editor.setTextColor(color)
    }

    /**
     * 设置默认文字大小。
     *
     * 单位 sp。
     */
    fun setEditorTextSize(
        size: Float
    ) {
        editorTextSize = size
        editor.textSize = size
    }

    /**
     * 显示/隐藏工具栏。
     */
    fun setToolbarVisible(
        visible: Boolean
    ) {
        toolbarVisible = visible
        toolbarScrollView.visibility =
            if (visible) {
                VISIBLE
            } else {
                GONE
            }
    }

    /**
     * 是否显示工具栏。
     */
    fun isToolbarVisible(): Boolean {
        return toolbarVisible
    }

    /**
     * 获取内部 EditText。
     */
    fun getEditText(): EditText {
        return editor
    }

    /**
     * 设置选区。
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
     * 清空内容。
     */
    fun clear() {
        editor.text?.clear()
    }

    /**
     * dp 转 px。
     */
    private fun dp(
        value: Int
    ): Int {
        return (
                value *
                        resources.displayMetrics.density +
                        0.5f
                ).toInt()
    }

    /**
     * 有序列表 Span。
     *
     * number 必须可读取，
     * 这样保存 DTXT 时才能恢复编号。
     */
    private class OrderedListSpan(
        val number: Int
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
            c.drawText(
                "$number.",
                x.toFloat(),
                baseline.toFloat(),
                p
            )
        }
    }
}