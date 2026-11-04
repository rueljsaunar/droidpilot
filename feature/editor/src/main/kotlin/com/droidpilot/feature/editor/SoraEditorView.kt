package com.droidpilot.feature.editor

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

@Composable
public fun SoraEditorView(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val editor = remember {
        CodeEditor(context).apply {
            typefaceText = Typeface.MONOSPACE
            setTextSize(14f)
            colorScheme = SchemeDarcula()
        }
    }

    // Set up TextMate Python grammar once; silently falls back if assets are missing
    LaunchedEffect(Unit) {
        runCatching {
            FileProviderRegistry.getInstance().addFileProvider(
                AssetsFileResolver(context.assets)
            )
            GrammarRegistry.getInstance().loadGrammars(languages {
                language("source.python") {
                    grammar = "textmate/python/python.tmLanguage.json"
                }
            })
            val language = TextMateLanguage.create("source.python", true)
            editor.setEditorLanguage(language)
        }
    }

    // Track whether we're in a programmatic setText to suppress spurious callbacks
    val isProgrammatic = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // Push content from ViewModel → editor when a new file is opened
    LaunchedEffect(content) {
        if (editor.text.toString() != content) {
            isProgrammatic.set(true)
            editor.setText(content)
            isProgrammatic.set(false)
        }
    }

    // Subscribe to content-change events for the autosave debounce
    DisposableEffect(editor) {
        val subscription = editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            if (!isProgrammatic.get()) {
                onContentChanged(editor.text.toString())
            }
        }
        onDispose {
            subscription.unsubscribe()
            editor.release()
        }
    }

    AndroidView(factory = { editor }, modifier = modifier)
}
