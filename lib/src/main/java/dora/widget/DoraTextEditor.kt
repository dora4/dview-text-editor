package dora.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.text.LineBreaker
import android.net.Uri
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
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Dora 富文本编辑器。
 *
 * `DoraTextEditor` 是 Dora View 文本编辑组件，
 * 基于 Android [Spannable] / [Editable] 实现富文本编辑。
 *
 * 支持的主要功能包括：
 *
 * - H1 / H2 / H3 标题
 * - 粗体
 * - 斜体
 * - 下划线
 * - 删除线
 * - 字体颜色
 * - 字号
 * - 左对齐
 * - 居中
 * - 右对齐
 * - 两端对齐
 * - 增加缩进
 * - 减少缩进
 * - 无序列表
 * - 有序列表
 * - Task List
 * - Check List
 * - Star List
 * - Quote
 * - BlockQuote
 * - Code
 * - Code Block
 * - 上标
 * - 下标
 * - 水平线
 * - 图片
 * - 拼写检查
 * - 自动换行
 * - DTXT 保存
 * - DTXT 加载
 *
 * 编辑器内部实际保存的是 Android `Spannable`，
 * DTXT 则负责将文字内容和 Span 信息序列化为 JSON。
 *
 * 示例：
 *
 * ```xml
 * <dora.widget.DoraTextEditor
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:dview_te_toolbarVisible="true"
 *     app:dview_te_toolbarColor="#F5F5F5"
 *     app:dview_te_textColor="#333333"
 *     app:dview_te_textSize="16sp" />
 * ```
 *
 * DTXT 文件结构示例：
 *
 * ```json
 * {
 *   "format": "dtxt",
 *   "version": 1,
 *   "text": "Hello Dora",
 *   "justify": false,
 *   "spans": [],
 *   "images": []
 * }
 * ```
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
         * 当前 DTXT 数据版本。
         *
         * 修改 DTXT 数据结构时应增加版本号，
         * 以便后续实现向后兼容。
         */
        private const val DTXT_VERSION = 1

        /**
         * 编辑器默认字号，单位 sp。
         */
        private const val DEFAULT_TEXT_SIZE = 16f

        /**
         * Toolbar 默认高度，单位 dp。
         */
        private const val DEFAULT_TOOLBAR_HEIGHT = 48

        /**
         * 编辑器默认文字颜色。
         */
        private const val DEFAULT_TEXT_COLOR = 0xFF333333.toInt()

        /**
         * 编辑器默认 Hint 颜色。
         */
        private const val DEFAULT_HINT_COLOR = 0xFF999999.toInt()

        /**
         * Toolbar 默认背景色。
         */
        private const val DEFAULT_TOOLBAR_COLOR = 0xFFF5F5F5.toInt()

        /**
         * Toolbar 默认分割线颜色。
         */
        private const val DEFAULT_DIVIDER_COLOR = 0xFFE5E5E5.toInt()

        /**
         * Code Block 默认背景色。
         */
        private const val DEFAULT_CODE_BACKGROUND = 0xFFF5F5F5.toInt()

        /**
         * Quote / BlockQuote 默认颜色。
         */
        private const val DEFAULT_QUOTE_COLOR = 0xFF888888.toInt()
    }

    // ============================================================
    // View
    // ============================================================

    /**
     * Toolbar 横向滚动容器。
     *
     * 当 Toolbar 中的按钮超过屏幕宽度时，
     * 用户可以水平滑动查看剩余按钮。
     */
    private val toolbarScrollView = HorizontalScrollView(context)

    /**
     * Toolbar 外层容器。
     *
     * 用于垂直排列 Toolbar 和底部分割线。
     */
    private val toolbarContainer = LinearLayout(context)

    /**
     * 实际承载所有 Toolbar 按钮的横向 LinearLayout。
     */
    private val toolbar = LinearLayout(context)

    /**
     * Toolbar 底部分割线。
     */
    private val toolbarDivider = View(context)

    /**
     * Toolbar显示在顶部还是底部，默认0-顶部。
     */
    private var toolbarPosition = 0

    /**
     * 实际执行文本编辑的 EditText。
     */
    private val editor = AppCompatEditText(context)

    // ============================================================
    // Attributes
    // ============================================================

    /**
     * Toolbar 是否显示。
     */
    private var toolbarVisible = true

    /**
     * Toolbar 高度，单位 px。
     */
    private var toolbarHeight = dp(DEFAULT_TOOLBAR_HEIGHT)

    /**
     * 编辑器文字颜色。
     */
    @ColorInt
    private var editorTextColor = DEFAULT_TEXT_COLOR

    /**
     * 编辑器 Hint 颜色。
     */
    @ColorInt
    private var editorHintColor = DEFAULT_HINT_COLOR

    /**
     * Toolbar 图标颜色。
     */
    @ColorInt
    private var toolbarTextColor = DEFAULT_TEXT_COLOR

    /**
     * Toolbar 背景颜色。
     */
    @ColorInt
    private var toolbarColor = DEFAULT_TOOLBAR_COLOR

    /**
     * Toolbar 分割线颜色。
     */
    @ColorInt
    private var dividerColor = DEFAULT_DIVIDER_COLOR

    /**
     * 编辑器默认字号，单位 sp。
     */
    private var editorTextSize = DEFAULT_TEXT_SIZE

    /**
     * 编辑器默认提示文字。
     */
    private var hintText = "请输入内容"

    /**
     * 当前没有选中文字时，
     * 后续输入文字所使用的颜色。
     */
    private var currentTextColor: Int? = null

    /**
     * 当前没有选中文字时，
     * 后续输入文字所使用的字号。
     */
    private var currentTextSize: Float? = null

    /**
     * 当前输入状态是否为粗体。
     */
    private var currentBold = false

    /**
     * 当前输入状态是否为斜体。
     */
    private var currentItalic = false

    /**
     * 当前输入状态是否带下划线。
     */
    private var currentUnderline = false

    /**
     * 当前输入状态是否带删除线。
     */
    private var currentStrikeThrough = false

    /**
     * 是否启用两端对齐。
     *
     * Android 没有 `Layout.Alignment.ALIGN_JUSTIFY`，
     * 因此 Android O 及以上通过 `EditText.justificationMode`
     * 实现两端对齐。
     */
    private var justifyEnabled = false

    /**
     * 是否启用拼写检查。
     */
    private var spellCheckEnabled = true

    /**
     * 是否启用自动换行。
     */
    private var wordWrapEnabled = true

    /**
     * 是否暂时禁止文本监听逻辑。
     *
     * 在加载 DTXT 时可以避免触发外部监听逻辑。
     */
    private var suppressTextWatcher = false

    /**
     * 是否支持插入图片，如果为true，需要设置onImageClickListener。
     */
    var insertImageEnabled: Boolean = false

    /**
     * 图片按钮点击回调。
     *
     * 由外部负责真正的图片选择逻辑，
     * 编辑器本身只负责接收最终的 Bitmap / Uri / File。
     */
    private var onImageClickListener: (() -> Unit)? = null

    // ============================================================
    // Init
    // ============================================================

    /**
     * 初始化控件。
     *
     * 初始化顺序：
     *
     * 1. 读取 XML 属性
     * 2. 初始化 Toolbar
     * 3. 初始化 EditText
     * 4. 添加 Toolbar
     * 5. 添加编辑器
     */
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

        val toolbarParams =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )

        val editorParams =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )

        if (toolbarPosition == 1) {
            addView(
                editor,
                editorParams
            )

            addView(
                toolbarContainer,
                toolbarParams
            )
        } else {
            addView(
                toolbarContainer,
                toolbarParams
            )

            addView(
                editor,
                editorParams
            )
        }

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

    /**
     * 从 XML 属性中读取控件配置。
     *
     * @param attrs XML 属性集合。
     */
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
            /**
             * 是否支持插入图片。
             */
            insertImageEnabled = typedArray.getBoolean(
                R.styleable.DoraTextEditor_dview_te_insertImageEnabled,
                true
            )
            /**
             * 是否显示 Toolbar。
             */
            toolbarVisible =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_toolbarVisible,
                    true
                )

            /**
             * Toolbar 高度。
             */
            toolbarHeight =
                typedArray.getDimensionPixelSize(
                    R.styleable.DoraTextEditor_dview_te_toolbarHeight,
                    dp(DEFAULT_TOOLBAR_HEIGHT)
                )

            /**
             * Toolbar 背景颜色。
             */
            toolbarColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_toolbarColor,
                    DEFAULT_TOOLBAR_COLOR
                )

            /**
             * Toolbar 位置。
             */
            toolbarPosition =
                typedArray.getInt(
                    R.styleable.DoraTextEditor_dview_te_toolbarPosition,
                    0
                )

            /**
             * 编辑器文字颜色。
             */
            editorTextColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_textColor,
                    DEFAULT_TEXT_COLOR
                )

            /**
             * 编辑器 Hint 颜色。
             */
            editorHintColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_hintColor,
                    DEFAULT_HINT_COLOR
                )

            /**
             * 编辑器 Hint。
             */
            hintText =
                typedArray.getString(
                    R.styleable.DoraTextEditor_dview_te_hint
                ) ?: "请输入内容"

            /**
             * XML 中的 textSize 使用 px 获取，
             * 因此需要转换回 sp 保存。
             */
            editorTextSize =
                typedArray.getDimension(
                    R.styleable.DoraTextEditor_dview_te_textSize,
                    spToPx(DEFAULT_TEXT_SIZE)
                ) / resources.displayMetrics.scaledDensity

            /**
             * Toolbar 分割线颜色。
             */
            dividerColor =
                typedArray.getColor(
                    R.styleable.DoraTextEditor_dview_te_dividerColor,
                    DEFAULT_DIVIDER_COLOR
                )

            /**
             * 是否启用拼写检查。
             */
            spellCheckEnabled =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_spellCheck,
                    true
                )

            /**
             * 是否自动换行。
             */
            wordWrapEnabled =
                typedArray.getBoolean(
                    R.styleable.DoraTextEditor_dview_te_wordWrap,
                    true
                )

            /**
             * 是否两端对齐。
             */
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

    /**
     * 创建 Toolbar。
     *
     * Toolbar 使用：
     *
     * `HorizontalScrollView -> LinearLayout -> ImageButton`
     *
     * 的结构，因此可以支持水平滚动。
     */
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

        // H1
        addIconButton(
            R.drawable.ic_dview_editor_type_h1,
            "H1"
        ) {
            setHeading(1)
        }

        // H2
        addIconButton(
            R.drawable.ic_dview_editor_type_h2,
            "H2"
        ) {
            setHeading(2)
        }

        // H3
        addIconButton(
            R.drawable.ic_dview_editor_type_h3,
            "H3"
        ) {
            setHeading(3)
        }

        addDivider()

        // ========================================================
        // 基础文字格式
        // ========================================================

        // 粗体
        addIconButton(
            R.drawable.ic_dview_editor_type_bold,
            "粗体"
        ) {
            toggleBold()
        }

        // 斜体
        addIconButton(
            R.drawable.ic_dview_editor_type_italic,
            "斜体"
        ) {
            toggleItalic()
        }

        // 下划线
        addIconButton(
            R.drawable.ic_dview_editor_type_underline,
            "下划线"
        ) {
            toggleUnderline()
        }

        // 删除线
        addIconButton(
            R.drawable.ic_dview_editor_type_strikethrough,
            "删除线"
        ) {
            toggleStrikeThrough()
        }

        addDivider()

        // ========================================================
        // 图片 / 字体 / 颜色
        // ========================================================
        if (insertImageEnabled) {
            addIconButton(
                R.drawable.ic_dview_editor_image,
                "图片"
            ) {
                showImagePicker()
            }
        }

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

        // ========================================================
        // 对齐
        // ========================================================

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

        addDivider()

        // ========================================================
        // 缩进
        // ========================================================

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

        // ========================================================
        // 列表
        // ========================================================

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

        addIconButton(
            R.drawable.ic_dview_editor_list_stars,
            "星标列表"
        ) {
            toggleStarList()
        }

        addDivider()

        // ========================================================
        // 引用
        // ========================================================

        addIconButton(
            R.drawable.ic_dview_editor_quote,
            "引用"
        ) {
            toggleBlockQuoteLeft()
        }

        // ========================================================
        // 水平线
        // ========================================================

        addIconButton(
            R.drawable.ic_dview_editor_hr,
            "水平线"
        ) {
            insertHorizontalRule()
        }

        addDivider()

        // ========================================================
        // 上标 / 下标
        // ========================================================

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

        // ========================================================
        // 特殊字符
        // ========================================================

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
    }

    /**
     * 向 Toolbar 添加一个图标按钮。
     *
     * @param iconRes 图标资源 ID。
     * @param description 无障碍描述。
     * @param onClick 点击回调。
     */
    private fun addIconButton(
        iconRes: Int,
        description: String,
        onClick: (View) -> Unit
    ) {
        val button = ImageButton(context)

        button.setImageResource(iconRes)

        button.imageTintList =
            ColorStateList.valueOf(toolbarTextColor)

        button.contentDescription = description

        button.scaleType =
            ImageView.ScaleType.CENTER

        button.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        button.setBackgroundColor(
            Color.TRANSPARENT
        )

        button.setOnClickListener(onClick)

        toolbar.addView(
            button,
            LayoutParams(
                dp(44),
                LayoutParams.MATCH_PARENT
            )
        )
    }

    /**
     * 向 Toolbar 添加一个垂直分割线。
     */
    private fun addDivider() {
        val divider = View(context)

        divider.setBackgroundColor(dividerColor)

        toolbar.addView(
            divider,
            LayoutParams(
                dp(1),
                dp(28)
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )
    }

    /**
     * 设置图片按钮点击回调。
     *
     * 外部可以在这里打开系统图片选择器。
     *
     * @param listener 图片按钮点击回调。
     */
    fun setOnImageClickListener(
        listener: (() -> Unit)?
    ) {
        onImageClickListener = listener
    }

    // ============================================================
    // Editor
    // ============================================================

    /**
     * 初始化内部 EditText。
     */
    private fun setupEditor() {
        editor.setTextColor(editorTextColor)

        editor.setHintTextColor(editorHintColor)

        editor.textSize = editorTextSize

        editor.hint = hintText

        editor.gravity =
            Gravity.TOP or Gravity.START

        editor.setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(16)
        )

        editor.setBackgroundColor(
            Color.TRANSPARENT
        )

        editor.isSingleLine = false

        editor.inputType = createInputType()

        editor.setHorizontallyScrolling(
            !wordWrapEnabled
        )

        applyJustifyMode()

        editor.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateCurrentStyleFromSelection()
            }
        }

        /**
         * 删除图片时，需要一次性删除 ImageSpan
         * 对应的占位字符，而不是只删除字符。
         */
        editor.setOnKeyListener { _, keyCode, event ->
            if (
                keyCode ==
                android.view.KeyEvent.KEYCODE_DEL &&
                event.action ==
                android.view.KeyEvent.ACTION_DOWN
            ) {
                val position = editor.selectionStart

                if (position > 0) {
                    val editable = editor.text

                    val spans = editable?.getSpans(
                        position - 1,
                        position,
                        ImageSpan::class.java
                    )

                    if (!spans.isNullOrEmpty()) {
                        spans.forEach { span ->
                            val start =
                                editable.getSpanStart(span)

                            val end =
                                editable.getSpanEnd(span)

                            editable.removeSpan(span)

                            editable.delete(
                                start,
                                end
                            )
                        }

                        return@setOnKeyListener true
                    }
                }
            }

            false
        }
    }

    /**
     * 创建 EditText 输入类型。
     *
     * @return Android InputType。
     */
    private fun createInputType(): Int {
        var type =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        if (spellCheckEnabled) {
            type =
                type or
                        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        }

        return type
    }

    // ============================================================
    // Bold
    // ============================================================

    /**
     * 切换当前选中文字的粗体状态。
     *
     * 如果没有选中文字，则只切换当前输入状态。
     */
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
                hasBold = true
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
                StyleSpan(Typeface.BOLD),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 移除指定范围内的粗体。
     *
     * 对于 `BOLD_ITALIC`，只移除粗体，
     * 保留斜体属性。
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
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)

                    editable.removeSpan(span)

                    editable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        maxOf(start, s),
                        minOf(end, e),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    // ============================================================
    // Italic
    // ============================================================

    /**
     * 切换当前选中文字的斜体状态。
     */
    fun toggleItalic() {
        val range = getSelectionRange()

        if (range == null) {
            currentItalic = !currentItalic
            return
        }

        val editable = editor.text
            ?: return

        val spans = editable.getSpans(
            range.first,
            range.second,
            StyleSpan::class.java
        )

        var hasItalic = false

        spans.forEach {
            if (
                it.style == Typeface.ITALIC ||
                it.style == Typeface.BOLD_ITALIC
            ) {
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
                StyleSpan(Typeface.ITALIC),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 移除指定范围内的斜体。
     *
     * 对于 `BOLD_ITALIC`，只移除斜体，
     * 保留粗体属性。
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
                    val s = editable.getSpanStart(span)
                    val e = editable.getSpanEnd(span)

                    editable.removeSpan(span)

                    editable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        maxOf(start, s),
                        minOf(end, e),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    // ============================================================
    // Underline
    // ============================================================

    /**
     * 切换当前选中文字的下划线。
     */
    fun toggleUnderline() {
        val range = getSelectionRange()
            ?: return

        val editable = editor.text
            ?: return

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

    /**
     * 切换当前选中文字的删除线。
     */
    fun toggleStrikeThrough() {
        val range = getSelectionRange()
            ?: return

        val editable = editor.text
            ?: return

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

    /**
     * 设置当前选中文字颜色。
     *
     * 如果当前没有选中文字，则保存为后续输入状态。
     *
     * @param color ARGB 颜色值。
     */
    fun setSelectionTextColor(
        @ColorInt color: Int
    ) {
        val range = getSelectionRange()

        if (range == null) {
            currentTextColor = color
            return
        }

        editor.text?.setSpan(
            ForegroundColorSpan(color),
            range.first,
            range.second,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 显示文字颜色选择菜单。
     */
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

        val views =
            colors.map {
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

    /**
     * 颜色菜单项。
     *
     * @property color 颜色值。
     */
    private class ColorMenuItem(
        @ColorInt val color: Int
    ) {
        /**
         * 对应的菜单 View。
         */
        var view: View? = null
    }

    // ============================================================
    // Text Size
    // ============================================================

    /**
     * 设置当前选中文字字号。
     *
     * @param size 字号，单位 sp。
     */
    fun setSelectionTextSize(
        size: Float
    ) {
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

    /**
     * 显示字号选择菜单。
     */
    private fun showTextSizeMenu(anchor: View) {
        val sizes =
            arrayOf(
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
            anchor,
            sizes.map {
                it.toString()
            }
        ) { value ->
            setSelectionTextSize(
                value.toFloat()
            )
        }
    }

    // ============================================================
    // Heading
    // ============================================================

    /**
     * 将当前段落设置为标题。
     *
     * 标题字号：
     *
     * - H1 = 32sp
     * - H2 = 26sp
     * - H3 = 22sp
     *
     * 标题同时使用粗体。
     *
     * @param level 标题级别，只支持 1、2、3。
     */
    fun setHeading(level: Int) {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        removeParagraphFormatting(
            editable,
            range.first,
            range.second
        )

        val size =
            when (level) {
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

    /**
     * 将当前段落恢复为正文。
     */
    fun setBodyText() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

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

    /**
     * 删除段落级格式。
     *
     * 当前删除：
     *
     * - AbsoluteSizeSpan
     * - AlignmentSpan
     *
     * @param editable 编辑内容。
     * @param start 起始位置。
     * @param end 结束位置。
     */
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

    /**
     * 设置当前段落对齐方式。
     *
     * @param alignment Android Layout.Alignment。
     */
    fun setAlignment(
        alignment: Layout.Alignment
    ) {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

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

    /**
     * 切换两端对齐。
     */
    fun setJustify() {
        justifyEnabled = !justifyEnabled
        applyJustifyMode()
    }

    /**
     * 设置两端对齐状态。
     *
     * @param justify 是否启用两端对齐。
     */
    fun setJustify(
        justify: Boolean
    ) {
        justifyEnabled = justify
        applyJustifyMode()
    }

    /**
     * 获取当前是否启用两端对齐。
     *
     * @return `true` 表示启用。
     */
    fun isJustifyEnabled(): Boolean {
        return justifyEnabled
    }

    /**
     * 将当前两端对齐状态应用到 EditText。
     *
     * Android O 以下无法使用 justificationMode，
     * 因此低版本保持系统默认行为。
     */
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

    /**
     * 增加当前段落缩进。
     *
     * 每一级缩进默认为 24dp。
     */
    fun increaseIndent() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val old =
            editable.getSpans(
                range.first,
                range.second,
                DoraIndentSpan::class.java
            )

        val current =
            old.firstOrNull()?.level
                ?: 0

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

    /**
     * 减少当前段落缩进。
     *
     * 缩进等级最低为 0。
     */
    fun decreaseIndent() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val old =
            editable.getSpans(
                range.first,
                range.second,
                DoraIndentSpan::class.java
            )

        if (old.isEmpty()) {
            return
        }

        val current =
            old.first().level

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

    /**
     * 切换无序列表。
     *
     * 使用 Android 原生 [BulletSpan] 绘制项目符号。
     */
    fun toggleUnorderedList() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换有序列表。
     *
     * 默认从数字 1 开始。
     */
    fun toggleOrderedList() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换任务列表。
     *
     * 默认创建未选中的任务项。
     */
    fun toggleTaskList() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换检查列表。
     *
     * 默认创建未选中的 Check List。
     */
    fun toggleCheckList() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换星标列表。
     *
     * 星标使用 `★` 字符绘制。
     */
    fun toggleStarList() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换普通引用。
     *
     * 普通引用使用左侧 `“` 符号。
     */
    fun toggleQuote() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换左侧 BlockQuote。
     *
     * 左侧引用会在段落左边绘制一条竖线。
     */
    fun toggleBlockQuoteLeft() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 设置右侧 BlockQuote。
     *
     * @note 当前实现会直接覆盖当前段落已有的 BlockQuote。
     */
    fun toggleBlockQuoteRight() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

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

    /**
     * 切换行内代码格式。
     */
    fun toggleCode() {
        val range =
            getSelectionRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换代码块。
     *
     * 代码块作用于当前段落。
     */
    fun toggleCodeBlock() {
        val range =
            getCurrentParagraphRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val existing =
            editable.getSpans(
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

    /**
     * 切换上标。
     */
    fun toggleSuperscript() {
        val range =
            getSelectionRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val spans =
            editable.getSpans(
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

    /**
     * 切换下标。
     */
    fun toggleSubscript() {
        val range =
            getSelectionRange()
                ?: return

        val editable =
            editor.text
                ?: return

        val spans =
            editable.getSpans(
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

    /**
     * 在当前光标位置插入水平线。
     *
     * 当前水平线通过字符模拟：
     *
     * `────────────────────`
     *
     * 因此它属于文本内容的一部分，而不是独立 Drawable。
     */
    fun insertHorizontalRule() {
        val position =
            editor.selectionStart

        if (position < 0) {
            return
        }

        val editable =
            editor.text
                ?: return

        val text =
            "\n────────────────────\n"

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

    /**
     * 在当前光标位置插入一个空行。
     */
    fun insertParagraphBreak() {
        val position =
            editor.selectionStart

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

    /**
     * 在当前选择区域插入文本。
     *
     * 如果存在选区，则替换选中的内容；
     * 如果没有选区，则直接在光标位置插入。
     *
     * @param value 要插入的文本。
     */
    fun insertText(
        value: String
    ) {
        val start =
            editor.selectionStart

        val end =
            editor.selectionEnd

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
    // Insert Image
    // ============================================================

    /**
     * 插入 Bitmap 图片。
     *
     * 图片在文本中使用一个 Unicode Object Replacement Character：
     *
     * `\uFFFC`
     *
     * 作为占位字符，并通过 [ImageSpan] 显示实际图片。
     *
     * 当图片宽度超过编辑器可用宽度时，
     * 会按照原始宽高比例自动缩小。
     *
     * @param bitmap 要插入的 Bitmap。
     * @param mimeType 图片 MIME 类型。
     */
    fun insertImage(
        bitmap: Bitmap,
        mimeType: String
    ) {
        val start =
            editor.selectionStart

        val end =
            editor.selectionEnd

        if (start < 0 || end < 0) {
            return
        }

        val drawable =
            BitmapDrawable(
                resources,
                bitmap
            )

        val maxWidth =
            editor.width -
                    editor.paddingLeft -
                    editor.paddingRight

        var width =
            bitmap.width

        var height =
            bitmap.height

        if (
            maxWidth > 0 &&
            width > maxWidth
        ) {
            val scale =
                maxWidth.toFloat() /
                        width

            width =
                maxWidth

            height =
                (height * scale).toInt()
        }

        drawable.setBounds(
            0,
            0,
            width,
            height
        )

        val imageSpan =
            ImageSpan(
                drawable,
                ImageSpan.ALIGN_BASELINE
            )

        val editable =
            editor.text
                ?: return

        val imagePosition =
            minOf(start, end)

        editable.replace(
            imagePosition,
            maxOf(start, end),
            "\uFFFC"
        )

        editable.setSpan(
            imageSpan,
            imagePosition,
            imagePosition + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editor.setSelection(
            (imagePosition + 1)
                .coerceAtMost(
                    editor.length()
                )
        )
    }

    /**
     * 插入默认 PNG 类型的 Bitmap。
     *
     * @param bitmap 要插入的图片。
     */
    fun insertImage(
        bitmap: Bitmap
    ) {
        insertImage(
            bitmap,
            "image/png"
        )
    }

    /**
     * 从 Uri 插入图片。
     *
     * @param uri 图片 Uri。
     * @param mimeType 图片 MIME 类型。
     * @throws Exception 无法读取或解析图片时抛出异常。
     */
    @Throws(Exception::class)
    fun insertImage(
        uri: Uri,
        mimeType: String? = null
    ) {
        val inputStream =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalArgumentException(
                    "无法打开图片：$uri"
                )

        inputStream.use {
            val bitmap =
                BitmapFactory.decodeStream(it)
                    ?: throw IllegalArgumentException(
                        "无法解析图片：$uri"
                    )

            insertImage(
                bitmap,
                mimeType
                    ?: context.contentResolver
                        .getType(uri)
                    ?: "image/png"
            )
        }
    }

    /**
     * 从文件插入图片。
     *
     * @param file 图片文件。
     * @param mimeType 图片 MIME 类型。
     * @throws Exception 图片不存在或无法解析时抛出异常。
     */
    @Throws(Exception::class)
    fun insertImage(
        file: File,
        mimeType: String = "image/png"
    ) {
        val bitmap =
            BitmapFactory.decodeFile(
                file.absolutePath
            )
                ?: throw IllegalArgumentException(
                    "无法解析图片：${file.absolutePath}"
                )

        insertImage(
            bitmap,
            mimeType
        )
    }

    // ============================================================
    // Spell Check
    // ============================================================

    /**
     * 切换拼写检查。
     */
    fun toggleSpellCheck() {
        spellCheckEnabled =
            !spellCheckEnabled

        editor.inputType =
            createInputType()

        val selection =
            editor.selectionStart
                .coerceAtLeast(0)
                .coerceAtMost(
                    editor.length()
                )

        editor.setSelection(selection)
    }

    /**
     * 设置拼写检查。
     *
     * @param enabled 是否启用。
     */
    fun setSpellCheckEnabled(
        enabled: Boolean
    ) {
        spellCheckEnabled =
            enabled

        editor.inputType =
            createInputType()
    }

    /**
     * 获取拼写检查状态。
     *
     * @return `true` 表示启用。
     */
    fun isSpellCheckEnabled(): Boolean {
        return spellCheckEnabled
    }

    // ============================================================
    // Word Wrap
    // ============================================================

    /**
     * 切换自动换行。
     */
    fun toggleWordWrap() {
        wordWrapEnabled =
            !wordWrapEnabled

        editor.setHorizontallyScrolling(
            !wordWrapEnabled
        )
    }

    /**
     * 设置自动换行。
     *
     * @param enabled 是否启用。
     */
    fun setWordWrapEnabled(
        enabled: Boolean
    ) {
        wordWrapEnabled =
            enabled

        editor.setHorizontallyScrolling(
            !enabled
        )
    }

    /**
     * 获取自动换行状态。
     *
     * @return `true` 表示启用。
     */
    fun isWordWrapEnabled(): Boolean {
        return wordWrapEnabled
    }

    // ============================================================
    // Clear Formatting
    // ============================================================

    /**
     * 清除当前选中文字的字符格式。
     *
     * 清除的格式包括：
     *
     * - 粗体
     * - 斜体
     * - 文字颜色
     * - 背景颜色
     * - 字号
     * - 下划线
     * - 删除线
     * - 上标
     * - 下标
     * - Typeface
     * - 对齐
     * - 缩进
     *
     * @note 列表、Quote、Code 等特殊段落 Span
     * 当前不会被这里清除。
     */
    fun clearSelectionFormatting() {
        val range =
            getSelectionRange()
                ?: return

        val editable =
            editor.text
                ?: return

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
                    editable.removeSpan(span)
                }
            }
        }
    }

    // ============================================================
    // Popup
    // ============================================================

    /**
     * 显示横向文本菜单。
     *
     * Popup 的宽度与 Toolbar 可视宽度一致，
     * 菜单内容本身可以水平滚动。
     *
     * @param anchor Popup 锚点。
     * @param values 菜单文字。
     * @param callback 菜单点击回调。
     */
    private fun showTextPopup(
        anchor: View,
        values: List<String>,
        callback: (String) -> Unit
    ) {
        val scrollView =
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode =
                    OVER_SCROLL_NEVER
                setBackgroundColor(Color.WHITE)
            }

        val container =
            LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL

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

        val popupWidth =
            toolbarScrollView.width

        val popup =
            PopupWindow(
                scrollView,
                popupWidth,
                toolbarHeight,
                true
            ).apply {
                setBackgroundDrawable(
                    Color.WHITE.toDrawable()
                )

                elevation =
                    dp(6).toFloat()

                isOutsideTouchable = true
                isFocusable = true
            }

        values.forEach { value ->
            val item =
                TextView(context).apply {
                    text = value
                    textSize = 14f
                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        toolbarTextColor
                    )

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

    /**
     * 显示颜色选择 Popup。
     *
     * @param anchor Popup 锚点。
     * @param items 颜色菜单项。
     * @param callback 点击回调。
     */
    private fun showPopup(
        anchor: View,
        items: List<ColorMenuItem>,
        callback: (ColorMenuItem) -> Unit
    ) {
        val scrollView =
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode =
                    OVER_SCROLL_NEVER
                setBackgroundColor(Color.WHITE)
            }

        val container =
            LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL

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

        val popupWidth =
            toolbarScrollView.width

        val popup =
            PopupWindow(
                scrollView,
                popupWidth,
                toolbarHeight,
                true
            ).apply {
                setBackgroundDrawable(
                    Color.WHITE.toDrawable()
                )

                elevation =
                    dp(6).toFloat()

                isOutsideTouchable = true
                isFocusable = true
            }

        items.forEach { item ->
            val view =
                View(context).apply {
                    setBackgroundColor(
                        item.color
                    )
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

    /**
     * 获取当前选择范围。
     *
     * @return 起始位置和结束位置，
     *         没有有效选择时返回 null。
     */
    private fun getSelectionRange(): Pair<Int, Int>? {
        val start =
            editor.selectionStart

        val end =
            editor.selectionEnd

        if (start < 0 || end < 0) {
            return null
        }

        val s =
            minOf(start, end)

        val e =
            maxOf(start, end)

        if (s == e) {
            return null
        }

        return s to e
    }

    /**
     * 获取当前光标所在段落的范围。
     *
     * 段落以 `\n` 为边界。
     *
     * @return 当前段落起始和结束位置。
     */
    private fun getCurrentParagraphRange():
            Pair<Int, Int>? {
        val editable =
            editor.text
                ?: return null

        if (editable.isEmpty()) {
            return null
        }

        val position =
            editor.selectionStart
                .coerceAtLeast(0)
                .coerceAtMost(
                    editable.length
                )

        var start =
            position

        while (
            start > 0 &&
            editable[start - 1] != '\n'
        ) {
            start--
        }

        var end =
            position

        while (
            end < editable.length &&
            editable[end] != '\n'
        ) {
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

    /**
     * 根据当前光标位置更新当前文字样式状态。
     *
     * 该状态主要用于记录光标所在位置的：
     *
     * - 粗体
     * - 斜体
     * - 下划线
     * - 删除线
     */
    private fun updateCurrentStyleFromSelection() {
        val editable =
            editor.text
                ?: return

        val position =
            editor.selectionStart

        if (
            position < 0 ||
            position >= editable.length
        ) {
            return
        }

        val styleSpans =
            editable.getSpans(
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

        currentUnderline =
            editable.getSpans(
                position,
                position + 1,
                UnderlineSpan::class.java
            ).isNotEmpty()

        currentStrikeThrough =
            editable.getSpans(
                position,
                position + 1,
                StrikethroughSpan::class.java
            ).isNotEmpty()
    }

    // ============================================================
    // DTXT Save
    // ============================================================

    /**
     * 将当前编辑内容保存为 DTXT 文件。
     *
     * @param file 目标 DTXT 文件。
     * @throws Exception 文件写入失败时抛出异常。
     */
    @Throws(Exception::class)
    fun saveDtxt(
        file: File
    ) {
        val root =
            buildDtxtJson()

        file.parentFile?.mkdirs()

        file.writeText(
            root.toString(),
            StandardCharsets.UTF_8
        )
    }

    /**
     * 获取当前编辑内容对应的 DTXT JSON 字符串。
     *
     * @return DTXT JSON。
     */
    @Throws(Exception::class)
    fun getDtxtContent(): String {
        return buildDtxtJson().toString()
    }

    /**
     * 创建 DTXT JSON。
     *
     * DTXT 分为：
     *
     * - format
     * - version
     * - text
     * - justify
     * - spans
     * - images
     */
    private fun buildDtxtJson(): JSONObject {
        val editable =
            editor.text
                ?: SpannableStringBuilder()

        val root =
            JSONObject()

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

        root.put(
            "justify",
            justifyEnabled
        )

        val spans =
            JSONArray()

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

        saveUnderlineSpans(
            editable,
            spans
        )

        saveStrikeSpans(
            editable,
            spans
        )

        saveSuperSubSpans(
            editable,
            spans
        )

        saveBulletSpans(
            editable,
            spans
        )

        saveOrderedSpans(
            editable,
            spans
        )

        saveTaskSpans(
            editable,
            spans
        )

        saveCheckSpans(
            editable,
            spans
        )

        saveStarSpans(
            editable,
            spans
        )

        saveQuoteSpans(
            editable,
            spans
        )

        saveCodeSpans(
            editable,
            spans
        )

        saveAlignmentSpans(
            editable,
            spans
        )

        saveIndentSpans(
            editable,
            spans
        )

        root.put(
            "spans",
            spans
        )

        val images =
            JSONArray()

        saveImageSpans(
            editable,
            images
        )

        root.put(
            "images",
            images
        )

        return root
    }

    /**
     * 将 DTXT 中的图片 JSON 恢复成 ImageSpan。
     *
     * 图片数据使用 Base64 编码。
     *
     * @param builder 正在构建的 Spannable。
     * @param item 图片 JSON。
     */
    private fun applyDtxtImage(
        builder: SpannableStringBuilder,
        item: JSONObject
    ) {
        val start =
            item.optInt(
                "start",
                -1
            ).coerceIn(
                0,
                builder.length
            )

        val end =
            item.optInt(
                "end",
                -1
            ).coerceIn(
                0,
                builder.length
            )

        if (start >= end) {
            return
        }

        val data =
            item.optString(
                "data",
                ""
            )

        if (data.isEmpty()) {
            return
        }

        try {
            val bytes =
                Base64.decode(
                    data,
                    Base64.NO_WRAP
                )

            val bitmap =
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )
                    ?: return

            val drawable =
                BitmapDrawable(
                    resources,
                    bitmap
                )

            val width =
                item.optInt(
                    "width",
                    bitmap.width
                )

            val height =
                item.optInt(
                    "height",
                    bitmap.height
                )

            drawable.setBounds(
                0,
                0,
                width,
                height
            )

            builder.setSpan(
                ImageSpan(
                    drawable,
                    ImageSpan.ALIGN_BASELINE
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } catch (_: Exception) {
            /**
             * 图片损坏时忽略该图片，
             * 保证整个 DTXT 文档仍然可以加载。
             */
        }
    }

    // ============================================================
    // Save Span
    // ============================================================

    /**
     * 保存 StyleSpan。
     */
    private fun saveStyleSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            StyleSpan::class.java
        ).forEach { span ->

            val start =
                editable.getSpanStart(span)

            val end =
                editable.getSpanEnd(span)

            if (start >= end) {
                return@forEach
            }

            val type =
                when (span.style) {
                    Typeface.BOLD ->
                        "bold"

                    Typeface.ITALIC ->
                        "italic"

                    Typeface.BOLD_ITALIC ->
                        "bold_italic"

                    else ->
                        return@forEach
                }

            spans.put(
                JSONObject()
                    .put("type", type)
                    .put("start", start)
                    .put("end", end)
            )
        }
    }

    /**
     * 保存文字颜色 Span。
     */
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

    /**
     * 保存字号 Span。
     */
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

    /**
     * 保存下划线 Span。
     */
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

    /**
     * 保存删除线 Span。
     */
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

    /**
     * 保存上标和下标 Span。
     */
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

    /**
     * 保存无序列表 Span。
     */
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

    /**
     * 保存有序列表 Span。
     */
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

    /**
     * 保存 Task List。
     */
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

    /**
     * 保存 Check List。
     */
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

    /**
     * 保存 Star List。
     */
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

    /**
     * 保存 Quote 和 BlockQuote。
     */
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

    /**
     * 保存 Code 和 Code Block。
     */
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

    /**
     * 保存段落对齐方式。
     */
    private fun saveAlignmentSpans(
        editable: Editable,
        spans: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            AlignmentSpan::class.java
        ).forEach {
            val alignment =
                when (it.alignment) {
                    Layout.Alignment.ALIGN_CENTER ->
                        "center"

                    Layout.Alignment.ALIGN_OPPOSITE ->
                        "right"

                    else ->
                        "left"
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

    /**
     * 保存缩进 Span。
     */
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

    /**
     * 保存图片 Span。
     *
     * 图片会：
     *
     * 1. 转换成 Bitmap
     * 2. 使用 PNG 编码
     * 3. 使用 Base64 编码
     * 4. 保存到 images 数组
     */
    private fun saveImageSpans(
        editable: Editable,
        images: JSONArray
    ) {
        editable.getSpans(
            0,
            editable.length,
            ImageSpan::class.java
        ).forEach { span ->

            val start =
                editable.getSpanStart(span)

            val end =
                editable.getSpanEnd(span)

            if (start !in 0..<end) {
                return@forEach
            }

            val drawable =
                span.drawable

            val bitmap =
                drawableToBitmap(
                    drawable
                )
                    ?: return@forEach

            val data =
                bitmapToBase64(
                    bitmap
                )

            val item =
                JSONObject()
                    .put(
                        "type",
                        "image"
                    )
                    .put(
                        "start",
                        start
                    )
                    .put(
                        "end",
                        end
                    )
                    .put(
                        "width",
                        drawable.bounds.width()
                    )
                    .put(
                        "height",
                        drawable.bounds.height()
                    )
                    .put(
                        "mime",
                        "image/png"
                    )
                    .put(
                        "data",
                        data
                    )

            images.put(item)
        }
    }

    /**
     * 调用外部图片选择器。
     */
    private fun showImagePicker() {
        onImageClickListener?.invoke()
    }

    /**
     * 将 Drawable 转换成 Bitmap。
     *
     * @param drawable 原始 Drawable。
     * @return Bitmap，无法转换时返回 null。
     */
    private fun drawableToBitmap(
        drawable: Drawable
    ): Bitmap? {
        val width =
            drawable.intrinsicWidth

        val height =
            drawable.intrinsicHeight

        if (width <= 0 || height <= 0) {
            return null
        }

        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            width,
            height
        )

        drawable.draw(canvas)

        return bitmap
    }

    /**
     * 将 Bitmap 编码成 Base64 PNG 字符串。
     *
     * @param bitmap 要编码的 Bitmap。
     * @return Base64 字符串。
     */
    private fun bitmapToBase64(
        bitmap: Bitmap
    ): String {
        val output =
            ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )

        return Base64.encodeToString(
            output.toByteArray(),
            Base64.NO_WRAP
        )
    }

    /**
     * 保存一个普通 Span 的范围信息。
     *
     * @param editable 编辑内容。
     * @param span 要保存的 Span。
     * @param type DTXT Span 类型。
     * @param spans DTXT spans 数组。
     * @return 创建的 JSON 对象。
     */
    private fun saveRange(
        editable: Editable,
        span: Any,
        type: String,
        spans: JSONArray
    ): JSONObject? {
        val start =
            editable.getSpanStart(span)

        val end =
            editable.getSpanEnd(span)

        if (start !in 0..<end) {
            return null
        }

        val item =
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

        spans.put(item)

        return item
    }

    // ============================================================
    // DTXT Load
    // ============================================================

    /**
     * 从 DTXT 文件加载编辑内容。
     *
     * @param file DTXT 文件。
     * @throws Exception 文件不存在、格式错误或读取失败时抛出异常。
     */
    @Throws(Exception::class)
    fun loadDtxt(
        file: File
    ) {
        if (!file.exists()) {
            throw IllegalArgumentException(
                "DTXT 文件不存在：${file.absolutePath}"
            )
        }

        loadDtxtContent(
            file.readText(
                StandardCharsets.UTF_8
            )
        )
    }

    /**
     * 从 DTXT JSON 字符串加载编辑内容。
     *
     * @param json DTXT JSON 字符串。
     * @throws Exception JSON 格式错误或版本不支持时抛出异常。
     */
    @Throws(Exception::class)
    fun loadDtxtContent(
        json: String
    ) {
        val root =
            JSONObject(json)

        if (
            root.optString("format") !=
            DTXT_FORMAT
        ) {
            throw IllegalArgumentException(
                "不是有效的 DTXT 文件"
            )
        }

        val version =
            root.optInt(
                "version",
                1
            )

        if (
            version !in 1..DTXT_VERSION
        ) {
            throw IllegalArgumentException(
                "不支持的 DTXT 版本：$version"
            )
        }

        val text =
            root.optString(
                "text",
                ""
            )

        justifyEnabled =
            root.optBoolean(
                "justify",
                false
            )

        val builder =
            SpannableStringBuilder(text)

        val spans =
            root.optJSONArray(
                "spans"
            )
                ?: JSONArray()

        for (
        i in 0 until spans.length()
        ) {
            val item =
                spans.optJSONObject(i)
                    ?: continue

            applyDtxtSpan(
                builder,
                item
            )
        }

        val images =
            root.optJSONArray(
                "images"
            )
                ?: JSONArray()

        for (
        i in 0 until images.length()
        ) {
            val item =
                images.optJSONObject(i)
                    ?: continue

            applyDtxtImage(
                builder,
                item
            )
        }

        suppressTextWatcher = true

        try {
            editor.setText(builder)

            editor.setSelection(
                builder.length
            )

            applyJustifyMode()
        } finally {
            suppressTextWatcher = false
        }

        updateCurrentStyleFromSelection()
    }

    /**
     * 将一个 DTXT Span JSON 对象恢复到 Spannable。
     *
     * @param builder 目标 Spannable。
     * @param item Span JSON。
     */
    private fun applyDtxtSpan(
        builder: SpannableStringBuilder,
        item: JSONObject
    ) {
        val type =
            item.optString("type")

        val start =
            item.optInt(
                "start",
                -1
            ).coerceIn(
                0,
                builder.length
            )

        val end =
            item.optInt(
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
                    when (
                        item.optString(
                            "value"
                        )
                    ) {
                        "center" ->
                            Layout.Alignment.ALIGN_CENTER

                        "right" ->
                            Layout.Alignment.ALIGN_OPPOSITE

                        else ->
                            Layout.Alignment.ALIGN_NORMAL
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

    /**
     * 设置编辑器内容。
     *
     * 如果传入的是 Spannable，
     * 其 Span 信息会被保留。
     *
     * @param value 新内容。
     */
    fun setContent(
        value: CharSequence?
    ) {
        editor.setText(value)
    }

    /**
     * 获取当前富文本内容。
     *
     * @return 当前 CharSequence。
     */
    fun getContent(): CharSequence {
        return editor.text ?: ""
    }

    /**
     * 获取当前 Editable。
     *
     * @return 内部 Editable。
     */
    fun getEditable(): Editable? {
        return editor.text
    }

    /**
     * 获取当前纯文本。
     *
     * 所有 Span 信息都会被忽略。
     *
     * @return 纯文本字符串。
     */
    fun getPlainText(): String {
        return editor.text?.toString()
            ?: ""
    }

    /**
     * 设置编辑器 Hint。
     *
     * @param value Hint 文本。
     */
    fun setHintText(
        value: String
    ) {
        hintText = value
        editor.hint = value
    }

    /**
     * 设置编辑器文字颜色。
     *
     * @param color ARGB 颜色。
     */
    fun setEditorTextColor(
        @ColorInt color: Int
    ) {
        editorTextColor = color
        editor.setTextColor(color)
    }

    /**
     * 设置编辑器默认字号。
     *
     * @param size 字号，单位 sp。
     */
    fun setEditorTextSize(
        size: Float
    ) {
        editorTextSize = size
        editor.textSize = size
    }

    /**
     * 设置 Toolbar 图标颜色。
     *
     * 会同时更新 Toolbar 中所有 ImageButton。
     *
     * @param color ARGB 颜色。
     */
    fun setToolbarTextColor(
        @ColorInt color: Int
    ) {
        toolbarTextColor = color

        for (
        i in 0 until toolbar.childCount
        ) {
            val child =
                toolbar.getChildAt(i)

            if (
                child is ImageButton
            ) {
                child.imageTintList =
                    ColorStateList.valueOf(
                        color
                    )
            }
        }
    }

    /**
     * 设置 Toolbar 背景颜色。
     *
     * @param color ARGB 颜色。
     */
    fun setToolbarColor(
        @ColorInt color: Int
    ) {
        toolbarColor = color

        toolbarContainer
            .setBackgroundColor(color)

        toolbarScrollView
            .setBackgroundColor(color)

        toolbar
            .setBackgroundColor(color)
    }

    /**
     * 获取 Toolbar 背景颜色。
     *
     * @return ARGB 颜色值。
     */
    @ColorInt
    fun getToolbarColor(): Int {
        return toolbarColor
    }

    /**
     * 设置 Toolbar 是否可见。
     *
     * @param visible 是否显示 Toolbar。
     */
    fun setToolbarVisible(
        visible: Boolean
    ) {
        toolbarVisible = visible

        toolbarContainer.visibility =
            if (visible) {
                VISIBLE
            } else {
                GONE
            }
    }

    /**
     * 获取 Toolbar 是否可见。
     *
     * @return `true` 表示显示。
     */
    fun isToolbarVisible(): Boolean {
        return toolbarVisible
    }

    /**
     * 设置 Toolbar 高度。
     *
     * @param heightDp 高度，单位 dp。
     */
    fun setToolbarHeight(
        heightDp: Int
    ) {
        toolbarHeight =
            dp(heightDp)

        val params =
            toolbarScrollView.layoutParams

        params.height =
            toolbarHeight

        toolbarScrollView.layoutParams =
            params
    }

    /**
     * 获取 Toolbar 高度。
     *
     * @return 高度，单位 dp。
     */
    fun getToolbarHeight(): Int {
        return (
                toolbarHeight /
                        resources.displayMetrics.density
                ).toInt()
    }

    /**
     * 设置 Toolbar 分割线颜色。
     *
     * @param color ARGB 颜色。
     */
    fun setDividerColor(
        @ColorInt color: Int
    ) {
        dividerColor = color

        toolbarDivider
            .setBackgroundColor(color)
    }

    /**
     * 获取 Toolbar 分割线颜色。
     *
     * @return ARGB 颜色。
     */
    @ColorInt
    fun getDividerColor(): Int {
        return dividerColor
    }

    /**
     * 获取内部 EditText。
     *
     * 适用于需要进一步配置 Android EditText
     * 原生属性的场景。
     *
     * @return 内部 EditText。
     */
    fun getEditText(): EditText {
        return editor
    }

    /**
     * 设置当前文本选择范围。
     *
     * 参数会自动限制在合法范围内。
     *
     * @param start 起始位置。
     * @param end 结束位置。
     */
    fun setSelection(
        start: Int,
        end: Int
    ) {
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

    /**
     * 清空编辑器。
     *
     * 同时关闭两端对齐状态。
     */
    fun clear() {
        editor.text?.clear()

        justifyEnabled = false

        applyJustifyMode()
    }

    // ============================================================
    // Span Classes
    // ============================================================

    /**
     * 有序列表 Span。
     *
     * 在段落左侧绘制：
     *
     * `1.`
     *
     * `2.`
     *
     * 等编号。
     *
     * @property number 当前列表编号。
     */
    private class OrderedListSpan(
        val number: Int
    ) : LeadingMarginSpan {

        /**
         * 列表左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 50
        }

        /**
         * 绘制列表编号。
         */
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

    /**
     * Dora 段落缩进 Span。
     *
     * @property level 缩进等级。
     * @property width 每一级缩进宽度。
     */
    private class DoraIndentSpan(
        val level: Int,
        private val width: Int
    ) : LeadingMarginSpan {

        /**
         * 根据缩进等级计算左边距。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return width * level
        }

        /**
         * 缩进本身不需要绘制任何内容。
         */
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

    /**
     * Task List Span。
     *
     * 使用方框表示任务状态。
     *
     * @property checked 是否已经完成。
     */
    private class TaskListSpan(
        val checked: Boolean
    ) : LeadingMarginSpan {

        /**
         * Task List 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 48
        }

        /**
         * 绘制任务框。
         */
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

            val oldStyle =
                p.style

            val oldStroke =
                p.strokeWidth

            p.style =
                Paint.Style.STROKE

            p.strokeWidth = 2f

            c.drawRect(
                x.toFloat(),
                baseline - 18f,
                x + 20f,
                baseline + 2f,
                p
            )

            if (checked) {
                p.style =
                    Paint.Style.FILL

                c.drawRect(
                    x.toFloat(),
                    baseline - 18f,
                    x + 20f,
                    baseline + 2f,
                    p
                )
            }

            p.style =
                oldStyle

            p.strokeWidth =
                oldStroke
        }
    }

    /**
     * Check List Span。
     *
     * 与 Task List 类似，但完成状态使用勾号表示。
     *
     * @property checked 是否已经完成。
     */
    private class CheckListSpan(
        val checked: Boolean
    ) : LeadingMarginSpan {

        /**
         * Check List 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 48
        }

        /**
         * 绘制 Check List。
         */
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

            val oldStyle =
                p.style

            val oldStroke =
                p.strokeWidth

            p.style =
                Paint.Style.STROKE

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

            p.style =
                oldStyle

            p.strokeWidth =
                oldStroke
        }
    }

    /**
     * Star List Span。
     *
     * 使用 `★` 作为列表符号。
     */
    private class StarListSpan :
        LeadingMarginSpan {

        /**
         * Star List 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 36
        }

        /**
         * 绘制星标。
         */
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

    /**
     * 普通 Quote Span。
     *
     * 在文本左侧显示一个引号。
     */
    private class QuoteSpan :
        LeadingMarginSpan {

        /**
         * Quote 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 36
        }

        /**
         * 绘制引号。
         */
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

    /**
     * BlockQuote Span。
     *
     * 可以在左侧或右侧绘制引用竖线。
     *
     * @property right 是否绘制在右侧。
     */
    private class BlockQuoteSpan(
        val right: Boolean
    ) : LeadingMarginSpan,
        android.text.style.LineBackgroundSpan {

        /**
         * 计算引用的左边距。
         *
         * 右侧引用不需要额外左边距。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return if (right) {
                0
            } else {
                20
            }
        }

        /**
         * 绘制左侧引用线。
         */
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

            val oldColor =
                p.color

            p.color =
                DEFAULT_QUOTE_COLOR

            c.drawRect(
                x.toFloat(),
                top.toFloat(),
                x + 6f,
                bottom.toFloat(),
                p
            )

            p.color =
                oldColor
        }

        /**
         * 绘制右侧引用线。
         */
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

            val oldColor =
                p.color

            p.color =
                DEFAULT_QUOTE_COLOR

            c.drawRect(
                (rightEdge - 6).toFloat(),
                top.toFloat(),
                rightEdge.toFloat(),
                bottom.toFloat(),
                p
            )

            p.color =
                oldColor
        }
    }

    /**
     * 行内 Code Span。
     *
     * 当前实现通过 LeadingMarginSpan
     * 保留代码文本的独立 Span 类型，
     * 后续可以继续扩展字体和背景绘制。
     */
    private class CodeSpan :
        LeadingMarginSpan {

        /**
         * Code 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 20
        }

        /**
         * 当前不额外绘制内容。
         */
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

    /**
     * Code Block Span。
     *
     * 当前实现为代码块左侧绘制背景区域。
     */
    private class CodeBlockSpan :
        LeadingMarginSpan {

        /**
         * Code Block 左侧预留空间。
         */
        override fun getLeadingMargin(
            first: Boolean
        ): Int {
            return 24
        }

        /**
         * 绘制 Code Block 背景。
         */
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

            val oldColor =
                p.color

            p.color =
                DEFAULT_CODE_BACKGROUND

            c.drawRect(
                x.toFloat(),
                top.toFloat(),
                (x + layout.width).toFloat(),
                bottom.toFloat(),
                p
            )

            p.color =
                oldColor
        }
    }

    // ============================================================
    // Utils
    // ============================================================

    /**
     * dp 转 px。
     *
     * @param value dp 数值。
     * @return 对应的 px 数值。
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
     * sp 转 px。
     *
     * @param value sp 数值。
     * @return 对应的 px 数值。
     */
    private fun spToPx(
        value: Float
    ): Float {
        return value *
                resources.displayMetrics.scaledDensity
    }
}