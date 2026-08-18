package com.lorecanvas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lorecanvas.app.ui.LoreCanvasApp
import com.lorecanvas.app.ui.theme.LoreCanvasTheme
import java.io.File

/**
 * Entry point. Projects live under the app's private files directory
 * (`filesDir/Projects/<sanitized name>/`) — simple, works with no extra
 * permissions, and matches "the software should become invisible"
 * (LCD-002). Exported project bundles live alongside it in `Exports/`,
 * used by the Import/Export screens (LCD-009 Ch.15). A user-chosen
 * location via the Storage Access Framework is a reasonable future
 * enhancement, not required for this to be a real, working feature.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectsRoot = File(filesDir, "Projects")
        val exportsRoot = File(filesDir, "Exports")

        setContent {
            LoreCanvasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoreCanvasApp(projectsRootDirectory = projectsRoot, exportsRootDirectory = exportsRoot)
                }
            }
        }
    }
}
