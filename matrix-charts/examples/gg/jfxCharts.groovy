/*
 * JavaFX application that runs all *Example.groovy scripts and displays
 * the generated SVG charts in a list/detail viewer.
 * Run it with: JAVA_OPTS=--enable-native-access=ALL-UNNAMED && groovy jfxCharts.groovy
 * Note: This script requires a full jdk (including javafx) distribution.
 * If you have a "normal" jdk, run the jfxChartsLinux or jfxChartsMac script as appropriate instead.
 */
@GrabConfig(systemClassLoader=true)
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-groovy-ext:0.1.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.girod.javafx.svgimage.SVGImage
import se.alipsa.matrix.chartexport.ChartToJfx
import javafx.collections.FXCollections

@SourceURI
URI sourceUri

File scriptDir = new File(sourceUri).parentFile
File subProjectDir = scriptDir.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')

// Run all *Example.groovy scripts
println "Running all example scripts..."
println "=" * 50

def binding = new Binding()
def shell = new GroovyShell(binding)

int successCount = 0
int failCount = 0

scriptDir.listFiles({ File f -> f.name.endsWith('Example.groovy') } as FileFilter)
    .sort { it.name }
    .each { File script ->
        println "Running: ${script.name}"
        try {
            shell.evaluate(script)
            successCount++
            println "  -> Success"
        } catch (Exception e) {
            failCount++
            println "  -> Failed: ${e.message}"
        }
    }

println "=" * 50
println "Completed: ${successCount} succeeded, ${failCount} failed"
println ""

// Load SVG files and create JavaFX application
if (!targetDir.exists() || !targetDir.isDirectory()) {
    println "Error: Target directory does not exist: ${targetDir.absolutePath}"
    System.exit(1)
}

File[] svgFiles = targetDir.listFiles({ File f -> f.name.endsWith('.svg') } as FileFilter)
if (svgFiles == null || svgFiles.length == 0) {
    println "Error: No SVG files found in ${targetDir.absolutePath}"
    System.exit(1)
}

svgFiles = svgFiles.sort { it.name }

println "Found ${svgFiles.length} SVG file(s). Opening viewer..."

// Store svgFiles for use in the Application
System.setProperty('jfxCharts.svgFiles', svgFiles*.absolutePath.join(File.pathSeparator))

class ChartViewerApp extends Application {

    @Override
    void start(Stage primaryStage) {
        String svgFilesProp = System.getProperty('jfxCharts.svgFiles')
        List<File> svgFiles = svgFilesProp.split(File.pathSeparator).collect { new File(it) }

        // List of SVG files on the left
        double listWidth = 200d
        ListView<File> listView = new ListView<>(FXCollections.observableArrayList(svgFiles))
        listView.prefWidth = listWidth
        listView.cellFactory = { ListView<File> view ->
            new ListCell<File>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        setText(null)
                    } else {
                        setText(item.name.replace('.svg', ''))
                    }
                }
            }
        }

        BorderPane listPane = new BorderPane(listView)
        listPane.padding = new Insets(8)

        // Content area that switches based on selected file
        StackPane contentArea = new StackPane()

        def showSvg = { File svgFile ->
            Node content
            boolean resizeStage = false
            try {
                String svgContent = svgFile.text
                SVGImage svgImage = ChartToJfx.export(svgContent)

                // Wrap in a StackPane for centering
                StackPane container = new StackPane(svgImage)

                // Wrap in ScrollPane for large charts
                ScrollPane scrollPane = new ScrollPane(container)
                scrollPane.fitToWidth = true
                scrollPane.fitToHeight = true

                def bounds = svgImage.boundsInLocal
                double svgWidth = bounds?.width ?: 0d
                double svgHeight = bounds?.height ?: 0d
                if (svgWidth <= 0d || svgHeight <= 0d) {
                    svgWidth = 800d
                    svgHeight = 600d
                }
                scrollPane.prefViewportWidth = svgWidth
                scrollPane.prefViewportHeight = svgHeight
                listView.prefHeight = svgHeight

                content = scrollPane
                println "  Loaded: ${svgFile.name}"
                resizeStage = true
            } catch (Exception e) {
                // Create an error label for failed SVGs
                Label errorLabel = new Label("Failed to load SVG:\n${e.message}")
                errorLabel.style = '-fx-text-fill: red; -fx-font-size: 14px;'
                content = new StackPane(errorLabel)
                println "  Failed to load: ${svgFile.name} - ${e.message}"
            }

            contentArea.children.clear()
            contentArea.children.add(content)
            if (resizeStage) {
                Platform.runLater {
                    primaryStage.sizeToScene()
                }
            }
        }

        // Handle list selection
        listView.selectionModel.selectedItemProperty().addListener { obs, oldVal, newVal ->
            if (newVal != null) {
                showSvg(newVal)
            }
        }

        // Select first item (the listener will handle adding the content)
        if (!svgFiles.isEmpty()) {
            listView.selectionModel.select(0)
        }

        BorderPane root = new BorderPane()
        root.left = listPane
        root.center = contentArea

        Scene scene = new Scene(root)
        primaryStage.title = "Chart Examples Viewer (JavaFX)"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

Application.launch(ChartViewerApp)
