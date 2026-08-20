# dview-text-editor

[![Release](https://jitpack.io/v/dora4/dview-text-editor.svg)](https://jitpack.io/#dora4/dview-text-editor)

`dview-text-editor` 是 Dora View 系列的 Android 富文本编辑器控件，基于 `Spannable` 实现，支持文本样式、标题、段落对齐、缩进、列表、引用、代码、上下标、颜色、字号、自动换行、拼写检查以及 DTXT 富文本文件保存与加载。

## Gradle 依赖配置

### 1. 添加 JitPack 仓库

在项目根目录的 `build.gradle` 中添加：

```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

如果项目使用 `settings.gradle` 管理仓库，可以配置：

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

### 2. 添加依赖

在 app 模块的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.github.dora4:dview-text-editor:1.2'
}
```

---

## 基础使用

### XML

```xml
<dora.widget.DoraTextEditor
    android:id="@+id/textEditor"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Kotlin

```kotlin
val editor = findViewById<DoraTextEditor>(
    R.id.textEditor
)

editor.setHintText("请输入文章内容")

editor.setEditorTextSize(16f)

editor.setEditorTextColor(
    Color.parseColor("#333333")
)
```

---

## XML 属性

控件统一使用 `dview_` 前缀。

### `dview_te_toolbarVisible`

是否显示工具栏。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_toolbarVisible="true" />
```

默认值：

```text
true
```

---

### `dview_te_toolbarHeight`

工具栏高度，单位为 `dp`。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_toolbarHeight="48dp" />
```

默认值：

```text
48dp
```

---

### `dview_te_textColor`

编辑器默认文字颜色。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_textColor="#333333" />
```

默认值：

```text
#333333
```

---

### `dview_te_hintColor`

提示文字颜色。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_hintColor="#999999" />
```

默认值：

```text
#999999
```

---

### `dview_te_hint`

编辑器为空时显示的提示文字。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_hint="请输入文章内容" />
```

默认值：

```text
请输入内容
```

---

### `dview_te_textSize`

编辑器默认文字大小，单位为 `sp`。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_textSize="16sp" />
```

默认值：

```text
16sp
```

---

### `dview_te_dividerColor`

工具栏分隔线颜色。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_dividerColor="#E5E5E5" />
```

默认值：

```text
#E5E5E5
```

---

### `dview_te_spellCheck`

是否启用系统拼写检查。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_spellCheck="true" />
```

默认值：

```text
true
```

---

### `dview_te_wordWrap`

是否自动换行。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_wordWrap="true" />
```

默认值：

```text
true
```

---

### `dview_te_justify`

是否启用两端对齐。

```xml
<dora.widget.DoraTextEditor
    ...
    app:dview_te_justify="true" />
```

默认值：

```text
false
```

两端对齐使用 Android 8.0（API 26）及以上提供的 `justificationMode`。

---

## 完整 XML 示例

```xml
<dora.widget.DoraTextEditor
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/textEditor"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:dview_te_toolbarVisible="true"
    app:dview_te_toolbarHeight="48dp"
    app:dview_te_textColor="#333333"
    app:dview_te_hintColor="#999999"
    app:dview_te_hint="请输入文章内容"
    app:dview_te_textSize="16sp"
    app:dview_te_dividerColor="#E5E5E5"
    app:dview_te_spellCheck="true"
    app:dview_te_wordWrap="true"
    app:dview_te_justify="false" />
```

---

# 文本内容

## 设置内容

```kotlin
editor.setContent("Hello Dora")
```

支持 `CharSequence`：

```kotlin
editor.setContent(
    SpannableStringBuilder("Hello Dora")
)
```

---

## 获取内容

获取富文本内容：

```kotlin
val content: CharSequence = editor.getContent()
```

获取 `Editable`：

```kotlin
val editable: Editable? = editor.getEditable()
```

获取纯文本：

```kotlin
val text: String = editor.getPlainText()
```

其中：

* `getContent()`：保留 `Spannable` 样式。
* `getEditable()`：直接访问编辑器内部 `Editable`。
* `getPlainText()`：只返回纯文本，不包含 Span。

---

## 清空内容

```kotlin
editor.clear()
```

清空后同时关闭两端对齐状态。

---

# 文字格式

## 粗体

```kotlin
editor.toggleBold()
```

---

## 斜体

```kotlin
editor.toggleItalic()
```

---

## 下划线

```kotlin
editor.toggleUnderline()
```

---

## 删除线

```kotlin
editor.toggleStrikeThrough()
```

---

## 清除选中文本格式

```kotlin
editor.clearSelectionFormatting()
```

该方法会清除选中范围内的主要字符格式和段落格式，包括：

* 粗体
* 斜体
* 字体颜色
* 字号
* 下划线
* 删除线
* 上标
* 下标
* 字体
* 对齐
* 缩进等

---

# 字体颜色

设置选中文字颜色：

```kotlin
editor.setSelectionTextColor(
    Color.RED
)
```

例如：

```kotlin
editor.setSelectionTextColor(
    Color.parseColor("#2196F3")
)
```

如果当前没有选中文本，则颜色会作为当前编辑状态保存。

---

# 字号

设置选中文字大小：

```kotlin
editor.setSelectionTextSize(24f)
```

例如：

```kotlin
editor.setSelectionTextSize(18f)
```

支持：

```text
10
12
14
16
18
20
22
24
28
32
36
48
```

工具栏中的字体按钮会显示字号选择菜单。

---

# 标题

## H1

```kotlin
editor.setHeading(1)
```

默认字号：

```text
32sp
```

## H2

```kotlin
editor.setHeading(2)
```

默认字号：

```text
26sp
```

## H3

```kotlin
editor.setHeading(3)
```

默认字号：

```text
22sp
```

## 正文

```kotlin
editor.setBodyText()
```

正文默认字号：

```text
16sp
```

标题和正文格式作用于当前段落。

---

# 段落

插入新的段落：

```kotlin
editor.insertParagraphBreak()
```

等价于在当前光标位置插入：

```text
\n
```

---

# 对齐

## 左对齐

```kotlin
editor.setAlignment(
    Layout.Alignment.ALIGN_NORMAL
)
```

## 居中

```kotlin
editor.setAlignment(
    Layout.Alignment.ALIGN_CENTER
)
```

## 右对齐

```kotlin
editor.setAlignment(
    Layout.Alignment.ALIGN_OPPOSITE
)
```

## 两端对齐

切换两端对齐：

```kotlin
editor.setJustify()
```

直接指定：

```kotlin
editor.setJustify(true)
```

关闭：

```kotlin
editor.setJustify(false)
```

查询状态：

```kotlin
val enabled = editor.isJustifyEnabled()
```

---

# 缩进

增加当前段落缩进：

```kotlin
editor.increaseIndent()
```

减少当前段落缩进：

```kotlin
editor.decreaseIndent()
```

每级缩进默认：

```text
24dp
```

缩进可以重复增加，从而形成多级缩进。

---

# 列表

## 无序列表

```kotlin
editor.toggleUnorderedList()
```

显示为：

```text
• 第一项
• 第二项
• 第三项
```

---

## 有序列表

```kotlin
editor.toggleOrderedList()
```

显示为：

```text
1. 第一项
2. 第二项
3. 第三项
```

---

## 数字列表

工具栏中的数字列表按钮同样使用：

```kotlin
editor.toggleOrderedList()
```

---

## 任务列表

```kotlin
editor.toggleTaskList()
```

---

## 检查列表

```kotlin
editor.toggleCheckList()
```

---

## 星标列表

```kotlin
editor.toggleStarList()
```

显示为：

```text
★ 重要内容
```

---

## 嵌套列表

可以通过增加缩进实现嵌套效果：

```kotlin
editor.increaseIndent()
```

---

# 引用

## 普通引用

```kotlin
editor.toggleQuote()
```

---

## 左引用

```kotlin
editor.toggleBlockQuoteLeft()
```

---

## 右引用

```kotlin
editor.toggleBlockQuoteRight()
```

---

# 代码

## 行内代码

选中文字后：

```kotlin
editor.toggleCode()
```

---

## 代码块

当前段落：

```kotlin
editor.toggleCodeBlock()
```

---

## 插入 `{}`

```kotlin
editor.insertText("{}")
```

## 插入 `{*}`

```kotlin
editor.insertText("{*}")
```

---

# 上标与下标

## 上标

```kotlin
editor.toggleSuperscript()
```

例如：

```text
x²
```

## 下标

```kotlin
editor.toggleSubscript()
```

例如：

```text
H₂O
```

---

# 特殊字符

可以直接插入任意文本：

```kotlin
editor.insertText("#")
editor.insertText("%")
editor.insertText("∞")
editor.insertText("*")
editor.insertText("±")
```

也可以插入自定义字符串：

```kotlin
editor.insertText("Dora")
```

---

# 水平线

在当前光标位置插入水平线：

```kotlin
editor.insertHorizontalRule()
```

生成类似：

```text
────────────────────
```

---

# 自动换行

切换自动换行：

```kotlin
editor.toggleWordWrap()
```

设置：

```kotlin
editor.setWordWrapEnabled(true)
```

关闭：

```kotlin
editor.setWordWrapEnabled(false)
```

查询：

```kotlin
val enabled = editor.isWordWrapEnabled()
```

关闭自动换行后，编辑器允许横向滚动。

---

# 拼写检查

切换：

```kotlin
editor.toggleSpellCheck()
```

设置：

```kotlin
editor.setSpellCheckEnabled(true)
```

关闭：

```kotlin
editor.setSpellCheckEnabled(false)
```

查询：

```kotlin
val enabled = editor.isSpellCheckEnabled()
```

该功能通过 Android `EditText` 的 `TYPE_TEXT_FLAG_AUTO_CORRECT` 实现，具体拼写检查能力取决于系统输入法和设备。

---

# 工具栏

## 显示 / 隐藏工具栏

```kotlin
editor.setToolbarVisible(true)
```

隐藏：

```kotlin
editor.setToolbarVisible(false)
```

查询：

```kotlin
val visible = editor.isToolbarVisible()
```

---

## 修改工具栏高度

```kotlin
editor.setToolbarHeight(48)
```

单位为 `dp`。

获取当前工具栏高度：

```kotlin
val heightDp = editor.getToolbarHeight()
```

---

## 修改工具栏文字 / 图标颜色

```kotlin
editor.setToolbarTextColor(
    Color.parseColor("#666666")
)
```

工具栏按钮使用 `ImageButton`，该颜色会应用到工具栏图标的 `imageTintList`。

---

# 编辑器默认样式

## 修改默认文字颜色

```kotlin
editor.setEditorTextColor(
    Color.parseColor("#333333")
)
```

注意：该方法修改的是编辑器默认文字颜色，不会覆盖已经通过 `setSelectionTextColor()` 设置的局部文字颜色。

---

## 修改默认字号

```kotlin
editor.setEditorTextSize(16f)
```

单位为 `sp`。

---

## 修改提示文字

```kotlin
editor.setHintText("请输入文章内容")
```

---

# 光标选择

设置选中范围：

```kotlin
editor.setSelection(
    0,
    5
)
```

例如选择前 5 个字符：

```kotlin
editor.setSelection(0, 5)
```

也可以设置光标位置：

```kotlin
editor.setSelection(10, 10)
```

---

# 获取原始 EditText

如果需要直接使用 Android `EditText` API，可以获取内部编辑器：

```kotlin
val editText: EditText = editor.getEditText()
```

例如：

```kotlin
editor.getEditText().requestFocus()
```

---

# DTXT 文件

`DoraTextEditor` 提供专用的 `DTXT` 富文本文件格式，用于保存和恢复编辑器内容及格式。

DTXT 基于 JSON。

基本结构：

```json
{
  "format": "dtxt",
  "version": 1,
  "text": "Hello Dora",
  "justify": false,
  "spans": []
}
```

---

## 保存 DTXT 文件

```kotlin
val file = File(
    filesDir,
    "article.dtxt"
)

editor.saveDtxt(file)
```

如果父目录不存在，控件会自动创建。

---

## 获取 DTXT 内容

如果不需要直接保存文件，可以获取 JSON 字符串：

```kotlin
val json = editor.getDtxtContent()
```

例如：

```kotlin
val json = editor.getDtxtContent()

File(
    filesDir,
    "article.dtxt"
).writeText(json)
```

---

# 加载 DTXT 文件

```kotlin
val file = File(
    filesDir,
    "article.dtxt"
)

editor.loadDtxt(file)
```

加载后会恢复：

* 文本内容
* 粗体
* 斜体
* 粗斜体
* 文字颜色
* 字号
* 下划线
* 删除线
* 上标
* 下标
* 无序列表
* 有序列表
* Task List
* Check List
* Star List
* Quote
* BlockQuote
* Code
* Code Block
* 段落对齐
* 缩进
* 两端对齐状态

---

## 从 JSON 字符串加载

```kotlin
val json = file.readText(
    Charsets.UTF_8
)

editor.loadDtxtContent(json)
```

---

# DTXT 数据格式

DTXT 顶层字段：

| 字段        | 类型      | 说明          |
| --------- | ------- | ----------- |
| `format`  | String  | 固定为 `dtxt`  |
| `version` | Int     | DTXT 版本     |
| `text`    | String  | 纯文本内容       |
| `justify` | Boolean | 是否启用两端对齐    |
| `spans`   | Array   | 富文本 Span 数据 |

---

## Span 通用字段

```json
{
  "type": "bold",
  "start": 0,
  "end": 5
}
```

其中：

* `type`：格式类型
* `start`：开始位置
* `end`：结束位置

---

## 支持的 Span 类型

### 粗体

```json
{
  "type": "bold",
  "start": 0,
  "end": 5
}
```

### 斜体

```json
{
  "type": "italic",
  "start": 0,
  "end": 5
}
```

### 粗斜体

```json
{
  "type": "bold_italic",
  "start": 0,
  "end": 5
}
```

### 颜色

```json
{
  "type": "color",
  "start": 0,
  "end": 5,
  "value": -65536
}
```

### 字号

```json
{
  "type": "size",
  "start": 0,
  "end": 5,
  "value": 24,
  "dip": true
}
```

### 下划线

```json
{
  "type": "underline",
  "start": 0,
  "end": 5
}
```

### 删除线

```json
{
  "type": "strike",
  "start": 0,
  "end": 5
}
```

### 上标

```json
{
  "type": "superscript",
  "start": 0,
  "end": 5
}
```

### 下标

```json
{
  "type": "subscript",
  "start": 0,
  "end": 5
}
```

### 无序列表

```json
{
  "type": "bullet",
  "start": 0,
  "end": 10
}
```

### 有序列表

```json
{
  "type": "ordered",
  "start": 0,
  "end": 10,
  "number": 1
}
```

### Task List

```json
{
  "type": "task",
  "start": 0,
  "end": 10,
  "checked": false
}
```

### Check List

```json
{
  "type": "check",
  "start": 0,
  "end": 10,
  "checked": true
}
```

### Star List

```json
{
  "type": "star",
  "start": 0,
  "end": 10
}
```

### Quote

```json
{
  "type": "quote",
  "start": 0,
  "end": 10
}
```

### BlockQuote

```json
{
  "type": "blockquote",
  "start": 0,
  "end": 10,
  "right": false
}
```

### Code

```json
{
  "type": "code",
  "start": 0,
  "end": 10
}
```

### Code Block

```json
{
  "type": "code_block",
  "start": 0,
  "end": 20
}
```

### 对齐

```json
{
  "type": "alignment",
  "start": 0,
  "end": 20,
  "value": "center"
}
```

支持：

```text
left
center
right
```

### 缩进

```json
{
  "type": "indent",
  "start": 0,
  "end": 20,
  "level": 2
}
```

---

# 完整示例

```kotlin
class EditorActivity : AppCompatActivity() {

    private lateinit var editor: DoraTextEditor

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_editor)

        editor = findViewById(
            R.id.textEditor
        )

        editor.setHintText(
            "请输入文章内容"
        )

        editor.setEditorTextSize(
            16f
        )

        editor.setEditorTextColor(
            Color.parseColor("#333333")
        )

        editor.setToolbarTextColor(
            Color.parseColor("#333333")
        )
    }

    private fun saveArticle() {
        val file = File(
            filesDir,
            "article.dtxt"
        )

        editor.saveDtxt(file)
    }

    private fun loadArticle() {
        val file = File(
            filesDir,
            "article.dtxt"
        )

        if (file.exists()) {
            editor.loadDtxt(file)
        }
    }
}
```

---

# 功能列表

| 功能           | API                            |
| ------------ | ------------------------------ |
| 粗体           | `toggleBold()`                 |
| 斜体           | `toggleItalic()`               |
| 下划线          | `toggleUnderline()`            |
| 删除线          | `toggleStrikeThrough()`        |
| 文字颜色         | `setSelectionTextColor()`      |
| 字号           | `setSelectionTextSize()`       |
| H1           | `setHeading(1)`                |
| H2           | `setHeading(2)`                |
| H3           | `setHeading(3)`                |
| 正文           | `setBodyText()`                |
| 左对齐          | `setAlignment(ALIGN_NORMAL)`   |
| 居中           | `setAlignment(ALIGN_CENTER)`   |
| 右对齐          | `setAlignment(ALIGN_OPPOSITE)` |
| 两端对齐         | `setJustify()`                 |
| 增加缩进         | `increaseIndent()`             |
| 减少缩进         | `decreaseIndent()`             |
| 无序列表         | `toggleUnorderedList()`        |
| 有序列表         | `toggleOrderedList()`          |
| Task List    | `toggleTaskList()`             |
| Check List   | `toggleCheckList()`            |
| Star List    | `toggleStarList()`             |
| Quote        | `toggleQuote()`                |
| 左 BlockQuote | `toggleBlockQuoteLeft()`       |
| 右 BlockQuote | `toggleBlockQuoteRight()`      |
| Code         | `toggleCode()`                 |
| Code Block   | `toggleCodeBlock()`            |
| 上标           | `toggleSuperscript()`          |
| 下标           | `toggleSubscript()`            |
| 水平线          | `insertHorizontalRule()`       |
| 插入文本         | `insertText()`                 |
| 拼写检查         | `toggleSpellCheck()`           |
| 自动换行         | `toggleWordWrap()`             |
| 清除格式         | `clearSelectionFormatting()`   |
| 保存 DTXT      | `saveDtxt()`                   |
| 加载 DTXT      | `loadDtxt()`                   |
| 获取 DTXT      | `getDtxtContent()`             |

---

# API 一览

## 内容

```kotlin
setContent(value: CharSequence?)
getContent(): CharSequence
getEditable(): Editable?
getPlainText(): String
clear()
```

## 编辑器

```kotlin
setHintText(value: String)
setEditorTextColor(color: Int)
setEditorTextSize(size: Float)
setSelection(start: Int, end: Int)
getEditText(): EditText
```

## 工具栏

```kotlin
setToolbarVisible(visible: Boolean)
isToolbarVisible(): Boolean

setToolbarTextColor(color: Int)

setToolbarHeight(heightDp: Int)
getToolbarHeight(): Int
```

## 格式

```kotlin
toggleBold()
toggleItalic()
toggleUnderline()
toggleStrikeThrough()

setSelectionTextColor(color: Int)
setSelectionTextSize(size: Float)

setHeading(level: Int)
setBodyText()

setAlignment(alignment: Layout.Alignment)

setJustify()
setJustify(justify: Boolean)
isJustifyEnabled(): Boolean

increaseIndent()
decreaseIndent()

toggleUnorderedList()
toggleOrderedList()
toggleTaskList()
toggleCheckList()
toggleStarList()

toggleQuote()
toggleBlockQuoteLeft()
toggleBlockQuoteRight()

toggleCode()
toggleCodeBlock()

toggleSuperscript()
toggleSubscript()

insertHorizontalRule()
insertParagraphBreak()
insertText(value: String)

clearSelectionFormatting()
```

## 编辑功能

```kotlin
toggleSpellCheck()
setSpellCheckEnabled(enabled: Boolean)
isSpellCheckEnabled(): Boolean

toggleWordWrap()
setWordWrapEnabled(enabled: Boolean)
isWordWrapEnabled(): Boolean
```

## DTXT

```kotlin
saveDtxt(file: File)

getDtxtContent(): String

loadDtxt(file: File)

loadDtxtContent(json: String)
```

---

# DTXT 设计

DTXT 的设计目标是：

1. 保留原始文本内容。
2. 保留文本范围对应的富文本格式。
3. 支持多个 Span 同时作用于同一段文本。
4. 支持多段文本。
5. 支持文件保存和重新加载。
6. 通过 `version` 支持后续格式升级。

例如一段文本可以同时具有：

```text
粗体 + 红色 + 24sp + 下划线
```

DTXT 会分别记录这些 Span，因此不同格式之间不会互相覆盖。

---

# 注意事项

### 1. Span 范围使用字符索引

`start` 和 `end` 使用 Java/Kotlin `CharSequence` 的字符索引，而不是像素位置。

### 2. 标题作用于当前段落

```kotlin
editor.setHeading(1)
```

会对当前光标所在段落进行处理。

### 3. 列表作用于当前段落

列表相关 API 默认作用于当前段落：

```kotlin
toggleUnorderedList()
toggleOrderedList()
toggleTaskList()
toggleCheckList()
toggleStarList()
```

### 4. 文字格式需要选中文本

例如：

```kotlin
editor.toggleBold()
```

通常需要先选择文本。

### 5. 两端对齐依赖 Android 8.0+

两端对齐使用：

```kotlin
editor.justificationMode
```

在低于 API 26 的设备上不会启用 Android 原生两端对齐模式。

### 6. DTXT 不等同于 HTML

DTXT 是 `DoraTextEditor` 自己定义的富文本格式，主要用于保存和恢复 `Spannable` 数据，不是 HTML、Markdown 或 RTF。
