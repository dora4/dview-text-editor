dview-text-editor ![Release](https://jitpack.io/v/dora4/dview-text-editor.svg)
--------------------------------

#### Gradle依赖配置

```groovy
// 添加以下代码到项目根目录下的build.gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
// 添加以下代码到app模块的build.gradle
dependencies {
    implementation 'com.github.dora4:dview-text-editor:1.2'
}
```

#### 控件使用

```xml
<dora.widget.DoraTextEditor
    android:id="@+id/textEditor"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

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
