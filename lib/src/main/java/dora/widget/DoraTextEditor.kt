package dora.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Layout
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.toDrawable
import dora.widget.texteditor.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Dora 富文本编辑器。
 *
 * 基于 Android Spannable 实现。
 *
 * 支持：
 *
 * - 粗体
 * - 斜体
 * - 下划线
 * - 删除线
 * - 字体颜色
 * - 字号
 * - H1 / H2 / H3
 * - 左 / 中 / 右 对齐
 * - 增加 / 减少缩进
 * - 无序列表
 * - 有序列表
 * - Star List
 * - Quote
 * - BlockQuote
 * - 上标
 * - 下标
 * - 水平线
 * - DTXT 保存
 * - DTXT 加载
 *
 * DTXT：
 *
 * {
 *   "format": "dtxt",
 *   "version": 1,
 *   "text": "...",
 *   "justify": false,
 *   "spans": [...]
 * }
 */
class DoraTextEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {

        private const val DTXT_FORMAT = "dtxt"

        /**
         * DTXT 版本。
         */
        private const val DTXT_VERSION = 1

        private const val DEFAULT_TEXT_SIZE = 16f

        private const val DEFAULT_TOOLBAR_HEIGHT = 48

        private const val DEFAULT_TEXT_COLOR = 0xFF333333.toInt()

        private const val DEFAULT_HINT_COLOR = 0xFF999999.toInt()

        private const val DEFAULT_TOOLBAR_COLOR = 0xFFF5F5F5.toInt()

        private const val DEFAULT_DIVIDER_COLOR = 0xFFE5E5E5.toInt()

        private const val DEFAULT_CODE_BACKGROUND = 0xFFF5F5F5.toInt()

        private const val DEFAULT_QUOTE_COLOR = 0xFF888888.toInt()
    }

    // ============================================================
    // View
    // ============================================================

    private val toolbarScrollView = HorizontalScrollView(context)

    private val toolbarContainer = LinearLayout(context)

    private val toolbar = LinearLayout(context)

    private val toolbarDivider = View(context)

    private val editor = AppCompatEditText(context)

    // ============================================================
    // Attributes
    // ============================================================

    private var toolbarVisible = true

    /**
     * Toolbar 高度，单位 px。
     */
    private var toolbarHeight = dp(DEFAULT_TOOLBAR_HEIGHT)

    @ColorInt
    private var editorTextColor = DEFAULT_TEXT_COLOR

    @ColorInt
    private var editorHintColor = DEFAULT_HINT_COLOR

    @ColorInt
    private var toolbarTextColor = DEFAULT_TEXT_COLOR

    @ColorInt
    private var toolbarColor = DEFAULT_TOOLBAR_COLOR

    @ColorInt
    private var dividerColor = DEFAULT_DIVIDER_COLOR

    /**
     * EditText 默认字号，单位 sp。
     */
    private var editorTextSize = DEFAULT_TEXT_SIZE

    private var hintText = "请输入内容"

    private var currentTextColor: Int? = null

    private var currentTextSize: Float? = null

    private var currentBold = false

    private var currentItalic = false

    private var currentUnderline = false

    private var currentStrikeThrough = false

    /**
     * 两端对齐。
     *
     * Android 没有 Layout.Alignment.ALIGN_JUSTIFY。
     *
     * 两端对齐通过 EditText.justificationMode 实现。
     */
    private var justifyEnabled =
        false

    private var spellCheckEnabled =
        true

    private var wordWrapEnabled =
        true

    private var suppressTextWatcher =
        false

    // ============================================================
    // Init
    // ============================================================

    init {
        orientation = VERTICAL
        initAttributes(attrs)
        toolbarContainer.orientation = VERTICAL
        toolbarContainer.setBackgroundColor(toolbarColor)
        setupToolbar()
        setupEditor()
        toolbarContainer.addView(
            toolbarScrollView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                toolbarHeight
            )
        )
        toolbarDivider.setBackgroundColor(dividerColor)
        toolbarContainer.addView(
            toolbarDivider,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
        addView(
            toolbarContainer,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
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
        toolbarContainer.visibility =
            if (toolbarVisible) {
                VISIBLE
            } else {
                GONE
            }
    }

    // ============================================================
    // Attributes
    // ============================================================

    private fun initAttributes(
        attrs: AttributeSet?
    ) {
        if (attrs == null) {
            return
        }
        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.DoraTextEditor
            )
        try {
            toolbarVisible =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_toolbarVisible,
                    true
                )
            toolbarHeight =
                typedArray.getDimensionPixelSize(
                    R.styleable.DoraTextEditor_dview_te_toolbarHeight,
                    dp(DEFAULT_TOOLBAR_HEIGHT)
                )
            toolbarColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_toolbarColor,
                    DEFAULT_TOOLBAR_COLOR
                )
            editorTextColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_textColor,
                    DEFAULT_TEXT_COLOR
                )
            editorHintColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_hintColor,
                    DEFAULT_HINT_COLOR
                )
            hintText =
                typedArray.getString(
                    R.styleable.DoraTextEditor_dview_te_hint
                ) ?: "请输入内容"
            editorTextSize =
                typedArray.getDimension(
                    R.styleable.DoraTextEditor_dview_te_textSize,
                    spToPx(DEFAULT_TEXT_SIZE)
                ) /
                        resources.displayMetrics.scaledDensity
            dividerColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_dividerColor,
                    DEFAULT_DIVIDER_COLOR
                )
            spellCheckEnabled =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_spellCheck,
                    true
                )
            wordWrapEnabled =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_wordWrap,
                    true
                )
            justifyEnabled =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_justify,
                    false
                )
        } finally {
            typedArray.recycle()
        }
    }

    // ============================================================
    // Toolbar
    // ============================================================

    private fun setupToolbar() {
        toolbar.orientation = HORIZONTAL
        toolbar.gravity = Gravity.CENTER_VERTICAL
        toolbar.setBackgroundColor(toolbarColor)
        toolbarScrollView.isHorizontalScrollBarEnabled = false
        toolbarScrollView.addView(
            toolbar,
            FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )
        addIconButton(
            R.drawable.ic_dview_editor_fonts,
            "字体"
        ) {
            showTextSizeMenu(it)
        }
        addIconButton(
            R.drawable.ic_dview_editor_palette,
            "文字颜色"
        ) {
            showColorMenu(it)
        }
        addDivider()
        /*
         * 基础文字
         */
        addIconButton(
            R.drawable.ic_dview_editor_type_bold,
            "粗体"
        ) {
            toggleBold()
        }
        addIconButton(
            R.drawable.ic_dview_editor_type_italic,
            "斜体"
        ) {
            toggleItalic()
        }
        addIconButton(
            R.drawable.ic_dview_editor_type_underline,
            "下划线"
        ) {
            toggleUnderline()
        }
        addIconButton(
            R.drawable.ic_dview_editor_type_strikethrough,
            "删除线"
        ) {
            toggleStrikeThrough()
        }
        addDivider()
        /*
         * 标题
         */
        addIconButton(
            R.drawable.ic_dview_editor_type_h1,
            "H1"
        ) {
            setHeading(1)
        }
        addIconButton(
            R.drawable.ic_dview_editor_type_h2,
            "H2"
        ) {
            setHeading(2)
        }
        addIconButton(
            R.drawable.ic_dview_editor_type_h3,
            "H3"
        ) {
            setHeading(3)
        }
//        addIconButton(
//            R.drawable.ic_dview_editor_type,
//            "正文"
//        ) {
//            setBodyText()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_paragraph,
//            "段落"
//        ) {
//            insertParagraphBreak()
//        }
        addDivider()
        /*
         * 对齐
         */
        addIconButton(
            R.drawable.ic_dview_editor_text_left,
            "左对齐"
        ) {
            setAlignment(
                Layout.Alignment.ALIGN_NORMAL
            )
        }
        addIconButton(
            R.drawable.ic_dview_editor_text_center,
            "居中"
        ) {
            setAlignment(
                Layout.Alignment.ALIGN_CENTER
            )
        }
        addIconButton(
            R.drawable.ic_dview_editor_text_right,
            "右对齐"
        ) {
            setAlignment(
                Layout.Alignment.ALIGN_OPPOSITE
            )
        }
//        addIconButton(
//            R.drawable.ic_dview_editor_text_paragraph,
//            "两端对齐"
//        ) {
//            setJustify()
//        }
        addDivider()
        /*
         * 缩进
         */
//        addIconButton(
//            R.drawable.ic_dview_editor_indent,
//            "增加缩进"
//        ) {
//            increaseIndent()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_unindent,
//            "减少缩进"
//        ) {
//            decreaseIndent()
//        }
        addIconButton(
            R.drawable.ic_dview_editor_text_indent_left,
            "左缩进"
        ) {
            increaseIndent()
        }
        addIconButton(
            R.drawable.ic_dview_editor_text_indent_right,
            "右缩进"
        ) {
            decreaseIndent()
        }
        addDivider()
        /*
         * 列表
         */
        addIconButton(
            R.drawable.ic_dview_editor_list_ul,
            "无序列表"
        ) {
            toggleUnorderedList()
        }
        addIconButton(
            R.drawable.ic_dview_editor_list_ol,
            "有序列表"
        ) {
            toggleOrderedList()
        }
//        addIconButton(
//            R.drawable.ic_dview_editor_123,
//            "数字列表"
//        ) {
//            toggleOrderedList()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_list_task,
//            "任务列表"
//        ) {
//            toggleTaskList()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_list_check,
//            "检查列表"
//        ) {
//            toggleCheckList()
//        }
        addIconButton(
            R.drawable.ic_dview_editor_list_stars,
            "星标列表"
        ) {
            toggleStarList()
        }
//        addIconButton(
//            R.drawable.ic_dview_editor_list_nested,
//            "嵌套列表"
//        ) {
//            increaseIndent()
//        }
        addDivider()
        /*
         * 引用
         */
        addIconButton(
            R.drawable.ic_dview_editor_quote,
            "引用"
        ) {
            toggleBlockQuoteLeft()
        }
        /*
         * 水平线
         */
        addIconButton(
            R.drawable.ic_dview_editor_hr,
            "水平线"
        ) {
            insertHorizontalRule()
        }
//        addIconButton(
//            R.drawable.ic_dview_editor_blockquote_left,
//            "左引用"
//        ) {
//            toggleBlockQuoteLeft()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_blockquote_right,
//            "右引用"
//        ) {
//            toggleBlockQuoteRight()
//        }
        addDivider()
        /*
         * 代码
         */
//        addIconButton(
//            R.drawable.ic_dview_editor_code_slash,
//            "代码"
//        ) {
//            toggleCodeBlock()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_code_square,
//            "代码块"
//        ) {
//            toggleCodeBlock()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_code_square_fill,
//            "代码块"
//        ) {
//            toggleCodeBlock()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_code,
//            "代码"
//        ) {
//            toggleCode()
//        }
        /*
         * 上下标
         */
        addIconButton(
            R.drawable.ic_dview_editor_superscript,
            "上标"
        ) {
            toggleSuperscript()
        }
        addIconButton(
            R.drawable.ic_dview_editor_subscript,
            "下标"
        ) {
            toggleSubscript()
        }
        addDivider()
        /*
         * 特殊字符
         */
        addIconButton(
            R.drawable.ic_dview_editor_hash,
            "#"
        ) {
            insertText("#")
        }
        addIconButton(
            R.drawable.ic_dview_editor_percent,
            "%"
        ) {
            insertText("%")
        }
        addIconButton(
            R.drawable.ic_dview_editor_infinity,
            "∞"
        ) {
            insertText("∞")
        }
        addIconButton(
            R.drawable.ic_dview_editor_asterisk,
            "*"
        ) {
            insertText("*")
        }
        addIconButton(
            R.drawable.ic_dview_editor_plus_slash_minus,
            "±"
        ) {
            insertText("±")
        }
        addIconButton(
            R.drawable.ic_dview_editor_braces,
            "{}"
        ) {
            insertText("{}")
        }
        addIconButton(
            R.drawable.ic_dview_editor_braces_asterisk,
            "{*}"
        ) {
            insertText("{*}")
        }

//        addIconButton(
//            R.drawable.ic_dview_editor_spellcheck,
//            "拼写检查"
//        ) {
//            toggleSpellCheck()
//        }
//        addIconButton(
//            R.drawable.ic_dview_editor_text_wrap,
//            "自动换行"
//        ) {
//            toggleWordWrap()
//        }
//        addDivider()
        /*
         * 清除格式
         */
//        addIconButton(
//            R.drawable.ic_dview_editor_vr,
//            "清除格式"
//        ) {
//            clearSelectionFormatting()
//        }
    }

    private fun addIconButton(
        iconRes: Int,
        description: String,
        onClick: (View) -> Unit
    ) {
        val button = ImageButton(context)
        button.setImageResource(iconRes)
        button.imageTintList = ColorStateList.valueOf(toolbarTextColor)
        button.contentDescription = description
        button.scaleType = ImageView.ScaleType.CENTER
        button.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )
        button.setBackgroundColor(Color.TRANSPARENT)
        button.setOnClickListener(onClick)
        toolbar.addView(
            button,
            LayoutParams(
                dp(44),
                LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun addDivider() {
        val divider = View(context)
        divider.setBackgroundColor(dividerColor)
        toolbar.addView(
            divider,
            LayoutParams(
                dp(1),
                dp(28)
            ).apply {
                marginStart =
                    dp(4)

                marginEnd =
                    dp(4)
            }
        )
    }

    // ============================================================
    // Editor
    // ============================================================

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
        editor.inputType = createInputType()
        editor.setHorizontallyScrolling(!wordWrapEnabled)
        applyJustifyMode()
        editor.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateCurrentStyleFromSelection()
            }
        }
    }

    private fun createInputType(): Int {
        var type = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        if (spellCheckEnabled) {
            type = type or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }
        return type
    }

    // ============================================================
    // Bold
    // ============================================================

    fun toggleBold() {
        val range = getSelectionRange()
            ?: run {
                currentBold = !currentBold
                return
            }
        val editable = editor.text
            ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            StyleSpan::class.java
        )
        var hasBold = false
        spans.forEach {
            if (
                it.style == Typeface.BOLD ||
                it.style == Typeface.BOLD_ITALIC
            ) {
                hasBold =
                    true
            }
        }
        if (hasBold) {
            removeBold(
                editable,
                range.first,
                range.second
            )
        } else {
            editable.setSpan(
                StyleSpan(
                    Typeface.BOLD
                ),
                range.first,
                range.second,
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
            when (span.style) {
                Typeface.BOLD -> {
                    editable.removeSpan(span)
                }

                Typeface.BOLD_ITALIC -> {
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)
                    editable.removeSpan(span)
                    editable.setSpan(
                        StyleSpan(
                            Typeface.ITALIC
                        ),
                        maxOf(
                            start,
                            s
                        ),
                        minOf(
                            end,
                            e
                        ),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    // ============================================================
    // Italic
    // ============================================================

    fun toggleItalic() {
        val range = getSelectionRange()
        if (range == null) {
            currentItalic = !currentItalic
            return
        }
        val editable = editor.text ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            StyleSpan::class.java
        )
        var hasItalic = false
        spans.forEach {
            if (it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC) {
                hasItalic = true
            }
        }
        if (hasItalic) {
            removeItalic(
                editable,
                range.first,
                range.second
            )
        } else {
            editable.setSpan(
                StyleSpan(
                    Typeface.ITALIC
                ),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
            when (span.style) {
                Typeface.ITALIC -> {
                    editable.removeSpan(
                        span
                    )
                }

                Typeface.BOLD_ITALIC -> {
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)
                    editable.removeSpan(span)
                    editable.setSpan(
                        StyleSpan(
                            Typeface.BOLD
                        ),
                        maxOf(
                            start,
                            s
                        ),
                        minOf(
                            end,
                            e
                        ),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    // ============================================================
    // Underline
    // ============================================================

    fun toggleUnderline() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            UnderlineSpan::class.java
        )
        if (spans.isNotEmpty()) {
            spans.forEach {
                editable.removeSpan(it)
            }
        } else {
            editable.setSpan(
                UnderlineSpan(),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // ============================================================
    // Strike Through
    // ============================================================

    fun toggleStrikeThrough() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            StrikethroughSpan::class.java
        )
        if (spans.isNotEmpty()) {
            spans.forEach {
                editable.removeSpan(it)
            }
        } else {
            editable.setSpan(
                StrikethroughSpan(),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // ============================================================
    // Color
    // ============================================================

    fun setSelectionTextColor(@ColorInt color: Int) {
        val range = getSelectionRange()
        if (range == null) {
            currentTextColor = color
            return
        }
        editor.text?.setSpan(
            ForegroundColorSpan(
                color
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun showColorMenu(anchor: View) {
        val colors =
            intArrayOf(
                Color.BLACK,
                0xFF333333.toInt(),
                Color.RED,
                0xFFE91E63.toInt(),
                0xFF9C27B0.toInt(),
                Color.BLUE,
                0xFF03A9F4.toInt(),
                Color.GREEN,
                0xFF009688.toInt(),
                0xFFFF9800.toInt(),
                0xFFFFC107.toInt()
            )
        val views = colors.map {
            ColorMenuItem(it)
        }
        showPopup(
            anchor,
            views
        ) { view ->
            setSelectionTextColor(
                view.color
            )
        }
    }

    private class ColorMenuItem(@ColorInt val color: Int) {

        var view: View? = null
    }

    // ============================================================
    // Text Size
    // ============================================================

    fun setSelectionTextSize(size: Float) {
        val range = getSelectionRange()
        if (range == null) {
            currentTextSize = size
            return
        }
        editor.text?.setSpan(
            AbsoluteSizeSpan(
                size.toInt(),
                true
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun showTextSizeMenu(anchor: View) {
        val sizes = arrayOf(
            10f,
            12f,
            14f,
            16f,
            18f,
            20f,
            22f,
            24f,
            28f,
            32f,
            36f,
            48f
        )
        showTextPopup(
            anchor, sizes.map { it.toString() }
        ) { value ->
            setSelectionTextSize(
                value.toFloat()
            )
        }
    }

    // ============================================================
    // Heading
    // ============================================================

    fun setHeading(level: Int) {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        removeParagraphFormatting(
            editable,
            range.first,
            range.second
        )
        val size = when (level) {
            1 -> 32
            2 -> 26
            3 -> 22
            else -> 16
        }
        editable.setSpan(
            AbsoluteSizeSpan(
                size,
                true
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        editable.setSpan(
            StyleSpan(
                Typeface.BOLD
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun setBodyText() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        removeParagraphFormatting(
            editable,
            range.first,
            range.second
        )
        editable.setSpan(
            AbsoluteSizeSpan(
                DEFAULT_TEXT_SIZE.toInt(),
                true
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun removeParagraphFormatting(
        editable: Editable,
        start: Int,
        end: Int
    ) {
        editable.getSpans(
            start,
            end,
            AbsoluteSizeSpan::class.java
        ).forEach {
            editable.removeSpan(it)
        }
        editable.getSpans(
            start,
            end,
            AlignmentSpan::class.java
        ).forEach {
            editable.removeSpan(it)
        }
    }

    // ============================================================
    // Alignment
    // ============================================================

    fun setAlignment(alignment: Layout.Alignment) {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        editable.getSpans(
            range.first,
            range.second,
            AlignmentSpan::class.java
        ).forEach {
            editable.removeSpan(it)
        }
        editable.setSpan(
            AlignmentSpan.Standard(
                alignment
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun setJustify() {
        justifyEnabled = !justifyEnabled
        applyJustifyMode()
    }

    fun setJustify(
        justify: Boolean
    ) {
        justifyEnabled = justify
        applyJustifyMode()
    }

    fun isJustifyEnabled(): Boolean {
        return justifyEnabled
    }

    private fun applyJustifyMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editor.justificationMode =
                if (justifyEnabled) {
                    LineBreaker.JUSTIFICATION_MODE_INTER_WORD
                } else {
                    LineBreaker.JUSTIFICATION_MODE_NONE
                }
        }
    }

    // ============================================================
    // Indent
    // ============================================================

    fun increaseIndent() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val old = editable.getSpans(
            range.first,
            range.second,
            DoraIndentSpan::class.java
        )
        val current = old.firstOrNull()?.level ?: 0
        old.forEach {
            editable.removeSpan(it)
        }
        editable.setSpan(
            DoraIndentSpan(
                current + 1,
                dp(24)
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun decreaseIndent() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val old = editable.getSpans(
            range.first,
            range.second,
            DoraIndentSpan::class.java
        )
        if (old.isEmpty()) {
            return
        }
        val current = old.first().level
        old.forEach {
            editable.removeSpan(it)
        }
        if (current > 1) {
            editable.setSpan(
                DoraIndentSpan(
                    current - 1,
                    dp(24)
                ),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // ============================================================
    // Unordered List
    // ============================================================

    fun toggleUnorderedList() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            BulletSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            BulletSpan(
                dp(8),
                editorTextColor
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ============================================================
    // Ordered List
    // ============================================================

    fun toggleOrderedList() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            OrderedListSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            OrderedListSpan(1),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ============================================================
    // Task List
    // ============================================================

    fun toggleTaskList() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            TaskListSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            TaskListSpan(false),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun toggleCheckList() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            CheckListSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            CheckListSpan(false),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun toggleStarList() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            StarListSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            StarListSpan(),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ============================================================
    // Quote
    // ============================================================

    fun toggleQuote() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            QuoteSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            QuoteSpan(),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun toggleBlockQuoteLeft() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            BlockQuoteSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            BlockQuoteSpan(
                right = false
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun toggleBlockQuoteRight() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        editable.getSpans(
            range.first,
            range.second,
            BlockQuoteSpan::class.java
        ).forEach {
            editable.removeSpan(it)
        }
        editable.setSpan(
            BlockQuoteSpan(
                right = true
            ),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ============================================================
    // Code
    // ============================================================

    fun toggleCode() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            CodeSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            CodeSpan(),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun toggleCodeBlock() {
        val range = getCurrentParagraphRange() ?: return
        val editable = editor.text ?: return
        val existing = editable.getSpans(
            range.first,
            range.second,
            CodeBlockSpan::class.java
        )
        if (existing.isNotEmpty()) {
            existing.forEach {
                editable.removeSpan(it)
            }
            return
        }
        editable.setSpan(
            CodeBlockSpan(),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ============================================================
    // Superscript / Subscript
    // ============================================================

    fun toggleSuperscript() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            SuperscriptSpan::class.java
        )
        if (spans.isNotEmpty()) {
            spans.forEach {
                editable.removeSpan(it)
            }
        } else {
            editable.setSpan(
                SuperscriptSpan(),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun toggleSubscript() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        val spans = editable.getSpans(
            range.first,
            range.second,
            SubscriptSpan::class.java
        )
        if (spans.isNotEmpty()) {
            spans.forEach {
                editable.removeSpan(it)
            }
        } else {
            editable.setSpan(
                SubscriptSpan(),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // ============================================================
    // Horizontal Rule
    // ============================================================

    fun insertHorizontalRule() {
        val position = editor.selectionStart
        if (position < 0) {
            return
        }
        val editable = editor.text ?: return
        val text = "\n────────────────────\n"
        editable.insert(
            position,
            text
        )
        editor.setSelection(
            (position + text.length)
                .coerceAtMost(
                    editor.length()
                )
        )
    }

    // ============================================================
    // Paragraph
    // ============================================================

    fun insertParagraphBreak() {
        val position = editor.selectionStart
        if (position < 0) {
            return
        }
        editor.text?.insert(
            position,
            "\n"
        )
    }

    // ============================================================
    // Insert Text
    // ============================================================

    fun insertText(value: String) {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start < 0 || end < 0) {
            return
        }
        editor.text?.replace(
            minOf(start, end),
            maxOf(start, end),
            value
        )
    }

    // ============================================================
    // Spell Check
    // ============================================================

    fun toggleSpellCheck() {
        spellCheckEnabled = !spellCheckEnabled
        editor.inputType = createInputType()
        val selection = editor.selectionStart
            .coerceAtLeast(0)
            .coerceAtMost(
                editor.length()
            )
        editor.setSelection(selection)
    }

    fun setSpellCheckEnabled(enabled: Boolean) {
        spellCheckEnabled = enabled
        editor.inputType = createInputType()
    }

    fun isSpellCheckEnabled(): Boolean {
        return spellCheckEnabled
    }

    // ============================================================
    // Word Wrap
    // ============================================================

    fun toggleWordWrap() {
        wordWrapEnabled = !wordWrapEnabled
        editor.setHorizontallyScrolling(!wordWrapEnabled)
    }

    fun setWordWrapEnabled(enabled: Boolean) {
        wordWrapEnabled = enabled
        editor.setHorizontallyScrolling(!enabled)
    }

    fun isWordWrapEnabled(): Boolean {
        return wordWrapEnabled
    }

    // ============================================================
    // Clear Formatting
    // ============================================================

    fun clearSelectionFormatting() {
        val range = getSelectionRange() ?: return
        val editable = editor.text ?: return
        editable.getSpans(
            range.first,
            range.second,
            Any::class.java
        ).forEach { span ->
            when (span) {
                is StyleSpan,
                is ForegroundColorSpan,
                is BackgroundColorSpan,
                is AbsoluteSizeSpan,
                is RelativeSizeSpan,
                is UnderlineSpan,
                is StrikethroughSpan,
                is SuperscriptSpan,
                is SubscriptSpan,
                is TypefaceSpan,
                is AlignmentSpan,
                is LeadingMarginSpan -> {
                    editable.removeSpan(
                        span
                    )
                }
            }
        }
    }

    // ============================================================
    // Popup
    // ============================================================
    private fun showTextPopup(
        anchor: View,
        values: List<String>,
        callback: (String) -> Unit
    ) {
        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(6),
                0,
                dp(6),
                0
            )
        }
        scrollView.addView(
            container,
            FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )
        val popupWidth = toolbarScrollView.width
        val popup = PopupWindow(
            scrollView,
            popupWidth,
            toolbarHeight,
            true
        ).apply {
            setBackgroundDrawable(Color.WHITE.toDrawable())
            elevation = dp(6).toFloat()
            isOutsideTouchable = true
            isFocusable = true
        }
        values.forEach { value ->
            val item = TextView(context).apply {
                text = value
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(toolbarTextColor)
                includeFontPadding = false
                setPadding(
                    dp(12),
                    0,
                    dp(12),
                    0
                )
            }
            container.addView(
                item,
                LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT
                )
            )
            item.setOnClickListener {
                callback(value)
                popup.dismiss()
            }
        }
        popup.showAsDropDown(
            anchor,
            0,
            -toolbarHeight
        )
    }

    private fun showPopup(
        anchor: View,
        items: List<ColorMenuItem>,
        callback: (ColorMenuItem) -> Unit
    ) {
        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(6),
                dp(4),
                dp(6),
                dp(4)
            )
        }
        scrollView.addView(
            container,
            FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        )
        val popupWidth = toolbarScrollView.width
        val popup = PopupWindow(
            scrollView,
            popupWidth,
            toolbarHeight,
            true
        ).apply {
            setBackgroundDrawable(Color.WHITE.toDrawable())
            elevation = dp(6).toFloat()
            isOutsideTouchable = true
            isFocusable = true
        }
        items.forEach { item ->
            val view = View(context).apply {
                setBackgroundColor(item.color)
            }
            item.view = view
            container.addView(
                view,
                LayoutParams(
                    dp(28),
                    dp(28)
                ).apply {
                    marginStart = dp(5)
                    marginEnd = dp(5)
                }
            )
            view.setOnClickListener {
                callback(item)
                popup.dismiss()
            }
        }
        popup.showAsDropDown(
            anchor,
            0,
            -toolbarHeight
        )
    }

    // ============================================================
    // Selection
    // ============================================================

    private fun getSelectionRange(): Pair<Int, Int>? {
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start < 0 || end < 0) {
            return null
        }
        val s = minOf(start, end)
        val e = maxOf(start, end)
        if (s == e) {
            return null
        }
        return s to e
    }

    private fun getCurrentParagraphRange(): Pair<Int, Int>? {
        val editable = editor.text ?: return null
        if (editable.isEmpty()) {
            return null
        }
        val position = editor.selectionStart
            .coerceAtLeast(0)
            .coerceAtMost(
                editable.length
            )
        var start = position
        while (start > 0 && editable[start - 1] != '\n') {
            start--
        }
        var end = position
        while (end < editable.length && editable[end] != '\n') {
            end++
        }
        if (start == end) {
            return null
        }
        return start to end
    }

    // ============================================================
    // Current Style
    // ============================================================

    private fun updateCurrentStyleFromSelection() {
        val editable = editor.text ?: return
        val position = editor.selectionStart
        if (position < 0 || position >= editable.length) {
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
            if (it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC) {
                currentBold = true
            }
            if (it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC) {
                currentItalic = true
            }
        }
        currentUnderline = editable.getSpans(
            position,
            position + 1,
            UnderlineSpan::class.java
        ).isNotEmpty()
        currentStrikeThrough = editable.getSpans(
            position,
            position + 1,
            StrikethroughSpan::class.java
        ).isNotEmpty()
    }

    // ============================================================
    // DTXT Save
    // ============================================================

    @Throws(Exception::class)
    fun saveDtxt(file: File) {
        val root = buildDtxtJson()
        file.parentFile?.mkdirs()
        file.writeText(root.toString(), StandardCharsets.UTF_8)
    }

    @Throws(Exception::class)
    fun getDtxtContent(): String {
        return buildDtxtJson().toString()
    }

    private fun buildDtxtJson(): JSONObject {
        val editable = editor.text ?: SpannableStringBuilder()
        val root = JSONObject()
        root.put("format", DTXT_FORMAT)
        root.put("version", DTXT_VERSION)
        root.put("text", editable.toString())
        root.put("justify", justifyEnabled)
        val spans = JSONArray()
        saveStyleSpans(editable, spans)
        saveColorSpans(editable, spans)
        saveSizeSpans(editable, spans)
        saveUnderlineSpans(editable, spans)
        saveStrikeSpans(editable, spans)
        saveSuperSubSpans(editable, spans)
        saveBulletSpans(editable, spans)
        saveOrderedSpans(editable, spans)
        saveTaskSpans(editable, spans)
        saveCheckSpans(editable, spans)
        saveStarSpans(editable, spans)
        saveQuoteSpans(editable, spans)
        saveCodeSpans(editable, spans)
        saveAlignmentSpans(editable, spans)
        saveIndentSpans(editable, spans)
        root.put("spans", spans)
        return root
    }

    // ============================================================
    // Save Span
    // ============================================================

    private fun saveStyleSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            StyleSpan::class.java
        ).forEach { span ->
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start >= end) {
                return@forEach
            }
            val type = when (span.style) {
                Typeface.BOLD -> "bold"
                Typeface.ITALIC -> "italic"
                Typeface.BOLD_ITALIC -> "bold_italic"
                else -> return@forEach
            }
            spans.put(
                JSONObject()
                    .put(
                        "type",
                        type
                    )
                    .put(
                        "start",
                        start
                    )
                    .put(
                        "end",
                        end
                    )
            )
        }
    }

    private fun saveColorSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            ForegroundColorSpan::class.java
        ).forEach { span ->
            saveRange(
                editable,
                span,
                "color",
                spans
            )?.put(
                "value",
                span.foregroundColor
            )
        }
    }

    private fun saveSizeSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            AbsoluteSizeSpan::class.java
        ).forEach { span ->
            saveRange(
                editable,
                span,
                "size",
                spans
            )?.put(
                "value",
                span.size
            )?.put(
                "dip",
                span.dip
            )
        }
    }

    private fun saveUnderlineSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            UnderlineSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "underline",
                spans
            )
        }
    }

    private fun saveStrikeSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            StrikethroughSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "strike",
                spans
            )
        }
    }

    private fun saveSuperSubSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            SuperscriptSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "superscript",
                spans
            )
        }

        editable.getSpans(
            0,
            editable.length,
            SubscriptSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "subscript",
                spans
            )
        }
    }

    private fun saveBulletSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            BulletSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "bullet",
                spans
            )
        }
    }

    private fun saveOrderedSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            OrderedListSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "ordered",
                spans
            )?.put(
                "number",
                it.number
            )
        }
    }

    private fun saveTaskSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            TaskListSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "task",
                spans
            )?.put(
                "checked",
                it.checked
            )
        }
    }

    private fun saveCheckSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            CheckListSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "check",
                spans
            )?.put(
                "checked",
                it.checked
            )
        }
    }

    private fun saveStarSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            StarListSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "star",
                spans
            )
        }
    }

    private fun saveQuoteSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            QuoteSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "quote",
                spans
            )
        }
        editable.getSpans(
            0,
            editable.length,
            BlockQuoteSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "blockquote",
                spans
            )?.put(
                "right",
                it.right
            )
        }
    }

    private fun saveCodeSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            CodeSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "code",
                spans
            )
        }
        editable.getSpans(
            0,
            editable.length,
            CodeBlockSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "code_block",
                spans
            )
        }
    }

    private fun saveAlignmentSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            AlignmentSpan::class.java
        ).forEach {
            val alignment = when (it.alignment) {
                Layout.Alignment.ALIGN_CENTER -> "center"
                Layout.Alignment.ALIGN_OPPOSITE -> "right"
                else -> "left"
            }
            saveRange(
                editable,
                it,
                "alignment",
                spans
            )?.put(
                "value",
                alignment
            )
        }
    }

    private fun saveIndentSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            DoraIndentSpan::class.java
        ).forEach {
            saveRange(
                editable,
                it,
                "indent",
                spans
            )?.put(
                "level",
                it.level
            )
        }
    }

    private fun saveRange(
        editable: Editable,
        span: Any,
        type: String,
        spans: JSONArray
    ): JSONObject? {
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start !in 0..<end) {
            return null
        }
        val item = JSONObject()
            .put(
                "type",
                type
            )
            .put(
                "start",
                start
            )
            .put(
                "end",
                end
            )
        spans.put(item)
        return item
    }

    // ============================================================
    // DTXT Load
    // ============================================================

    @Throws(Exception::class)
    fun loadDtxt(file: File) {
        if (!file.exists()) {
            throw IllegalArgumentException(
                "DTXT 文件不存在：${file.absolutePath}"
            )
        }
        loadDtxtContent(file.readText(StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    fun loadDtxtContent(json: String) {
        val root = JSONObject(json)
        if (root.optString("format") != DTXT_FORMAT) {
            throw IllegalArgumentException("不是有效的 DTXT 文件")
        }
        val version = root.optInt("version", 1)
        if (version !in 1..DTXT_VERSION) {
            throw IllegalArgumentException("不支持的 DTXT 版本：$version")
        }
        val text = root.optString("text", "")
        justifyEnabled = root.optBoolean("justify", false)
        val builder = SpannableStringBuilder(text)
        val spans = root.optJSONArray("spans") ?: JSONArray()
        for (i in 0 until spans.length()) {
            val item = spans.optJSONObject(i) ?: continue
            applyDtxtSpan(builder, item)
        }
        suppressTextWatcher = true
        try {
            editor.setText(builder)
            editor.setSelection(builder.length)
            applyJustifyMode()
        } finally {
            suppressTextWatcher = false
        }
        updateCurrentStyleFromSelection()
    }

    private fun applyDtxtSpan(builder: SpannableStringBuilder, item: JSONObject) {
        val type = item.optString("type")
        val start = item.optInt(
            "start",
            -1
        ).coerceIn(
            0,
            builder.length
        )
        val end = item.optInt(
            "end",
            -1
        ).coerceIn(
            0,
            builder.length
        )
        if (start >= end) {
            return
        }
        when (type) {
            "bold" ->
                builder.setSpan(
                    StyleSpan(
                        Typeface.BOLD
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "italic" ->
                builder.setSpan(
                    StyleSpan(
                        Typeface.ITALIC
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "bold_italic" ->
                builder.setSpan(
                    StyleSpan(
                        Typeface.BOLD_ITALIC
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "color" ->
                builder.setSpan(
                    ForegroundColorSpan(
                        item.optInt(
                            "value",
                            editorTextColor
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "size" ->
                builder.setSpan(
                    AbsoluteSizeSpan(
                        item.optInt(
                            "value",
                            16
                        ),
                        item.optBoolean(
                            "dip",
                            true
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "underline" ->
                builder.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "strike" ->
                builder.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "superscript" ->
                builder.setSpan(
                    SuperscriptSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "subscript" ->
                builder.setSpan(
                    SubscriptSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "bullet" ->
                builder.setSpan(
                    BulletSpan(
                        dp(8),
                        editorTextColor
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "ordered" ->
                builder.setSpan(
                    OrderedListSpan(
                        item.optInt(
                            "number",
                            1
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "task" ->
                builder.setSpan(
                    TaskListSpan(
                        item.optBoolean(
                            "checked",
                            false
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "check" ->
                builder.setSpan(
                    CheckListSpan(
                        item.optBoolean(
                            "checked",
                            false
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "star" ->
                builder.setSpan(
                    StarListSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "quote" ->
                builder.setSpan(
                    QuoteSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "blockquote" ->
                builder.setSpan(
                    BlockQuoteSpan(
                        item.optBoolean(
                            "right",
                            false
                        )
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "code" ->
                builder.setSpan(
                    CodeSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "code_block" ->
                builder.setSpan(
                    CodeBlockSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            "alignment" -> {
                val alignment =
                    when (item.optString("value")) {
                        "center" -> Layout.Alignment.ALIGN_CENTER
                        "right" -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_NORMAL
                    }
                builder.setSpan(
                    AlignmentSpan.Standard(
                        alignment
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            "indent" -> {
                builder.setSpan(
                    DoraIndentSpan(
                        item.optInt(
                            "level",
                            1
                        ),
                        dp(24)
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

    fun setContent(value: CharSequence?) {
        editor.setText(value)
    }

    fun getContent(): CharSequence {
        return editor.text ?: ""
    }

    fun getEditable(): Editable? {
        return editor.text
    }

    fun getPlainText(): String {
        return editor.text?.toString() ?: ""
    }

    fun setHintText(value: String) {
        hintText = value
        editor.hint = value
    }

    fun setEditorTextColor(@ColorInt color: Int) {
        editorTextColor = color
        editor.setTextColor(color)
    }

    fun setEditorTextSize(size: Float) {
        editorTextSize = size
        editor.textSize = size
    }

    fun setToolbarTextColor(@ColorInt color: Int) {
        toolbarTextColor = color
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is ImageButton) {
                child.imageTintList =
                    ColorStateList.valueOf(color)
            }
        }
    }

    fun setToolbarColor(@ColorInt color: Int) {
        toolbarColor = color
        toolbarContainer.setBackgroundColor(color)
        toolbarScrollView.setBackgroundColor(color)
        toolbar.setBackgroundColor(color)
    }

    @ColorInt
    fun getToolbarColor(): Int {
        return toolbarColor
    }

    fun setToolbarVisible(visible: Boolean) {
        toolbarVisible = visible
        toolbarScrollView.visibility =
            if (visible) {
                VISIBLE
            } else {
                GONE
            }
    }

    fun isToolbarVisible(): Boolean {
        return toolbarVisible
    }

    fun setToolbarHeight(heightDp: Int) {
        toolbarHeight = dp(heightDp)
        val params = toolbarScrollView.layoutParams
        params.height = toolbarHeight
        toolbarScrollView.layoutParams = params
    }

    fun getToolbarHeight(): Int {
        return (toolbarHeight / resources.displayMetrics.density).toInt()
    }

    fun setDividerColor(@ColorInt color: Int) {
        dividerColor = color
        toolbarDivider.setBackgroundColor(color)
    }

    @ColorInt
    fun getDividerColor(): Int {
        return dividerColor
    }

    fun getEditText(): EditText {
        return editor
    }

    fun setSelection(start: Int, end: Int) {
        val safeStart =
            start.coerceIn(
                0,
                editor.length()
            )
        val safeEnd =
            end.coerceIn(
                0,
                editor.length()
            )
        editor.setSelection(
            safeStart,
            safeEnd
        )
    }

    fun clear() {
        editor.text?.clear()
        justifyEnabled = false
        applyJustifyMode()
    }

    // ============================================================
    // Span Classes
    // ============================================================

    private class OrderedListSpan(val number: Int) : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 50
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
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

    private class DoraIndentSpan(
        val level: Int,
        private val width: Int
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return width * level
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
        }
    }

    private class TaskListSpan(
        val checked: Boolean
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 48
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (!first) {
                return
            }
            val oldStyle = p.style
            val oldStroke = p.strokeWidth
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            c.drawRect(
                x.toFloat(),
                baseline - 18f,
                x + 20f,
                baseline + 2f,
                p
            )
            if (checked) {
                p.style = Paint.Style.FILL
                c.drawRect(
                    x.toFloat(),
                    baseline - 18f,
                    x + 20f,
                    baseline + 2f,
                    p
                )
            }
            p.style = oldStyle
            p.strokeWidth = oldStroke
        }
    }

    private class CheckListSpan(val checked: Boolean) : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 48
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (!first) {
                return
            }
            val oldStyle = p.style
            val oldStroke = p.strokeWidth
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            c.drawRect(
                x.toFloat(),
                baseline - 18f,
                x + 20f,
                baseline + 2f,
                p
            )
            if (checked) {
                c.drawLine(
                    x + 3f,
                    baseline - 8f,
                    x + 8f,
                    baseline - 3f,
                    p
                )
                c.drawLine(
                    x + 8f,
                    baseline - 3f,
                    x + 17f,
                    baseline - 14f,
                    p
                )
            }
            p.style = oldStyle
            p.strokeWidth = oldStroke
        }
    }

    private class StarListSpan : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 36
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (!first) {
                return
            }
            c.drawText(
                "★",
                x.toFloat(),
                baseline.toFloat(),
                p
            )
        }
    }

    private class QuoteSpan : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 36
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (!first) {
                return
            }
            c.drawText(
                "“",
                x.toFloat(),
                baseline.toFloat(),
                p
            )
        }
    }

    private class BlockQuoteSpan(
        val right: Boolean
    ) : LeadingMarginSpan, android.text.style.LineBackgroundSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            // 只有左引用需要给正文预留左边距
            return if (right) {
                0
            } else {
                20
            }
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (right) {
                return
            }
            val oldColor = p.color
            p.color = DEFAULT_QUOTE_COLOR
            c.drawRect(
                x.toFloat(),
                top.toFloat(),
                x + 6f,
                bottom.toFloat(),
                p
            )
            p.color = oldColor
        }

        override fun drawBackground(
            c: Canvas,
            p: Paint,
            left: Int,
            rightEdge: Int,
            top: Int,
            baseline: Int,
            bottom: Int,
            text: CharSequence,
            start: Int,
            end: Int,
            lineNumber: Int
        ) {
            if (!right) {
                return
            }
            val oldColor = p.color
            p.color = DEFAULT_QUOTE_COLOR
            c.drawRect(
                (rightEdge - 6).toFloat(),
                top.toFloat(),
                rightEdge.toFloat(),
                bottom.toFloat(),
                p
            )
            p.color = oldColor
        }
    }

    private class CodeSpan : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 20
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
        }
    }

    private class CodeBlockSpan : LeadingMarginSpan {

        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 24
        }

        override fun drawLeadingMargin(
            c: Canvas,
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
            layout: Layout
        ) {
            if (!first) {
                return
            }
            val oldColor = p.color
            p.color = DEFAULT_CODE_BACKGROUND
            c.drawRect(
                x.toFloat(),
                top.toFloat(),
                (x + layout.width).toFloat(),
                bottom.toFloat(),
                p
            )
            p.color = oldColor
        }
    }

    // ============================================================
    // Utils
    // ============================================================

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun spToPx(value: Float): Float {
        return value * resources.displayMetrics.scaledDensity
    }
}