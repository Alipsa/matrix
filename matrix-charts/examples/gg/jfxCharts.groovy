/*
 * JavaFX application that runs all *Example.groovy scripts and displays
 * the generated SVG charts in a tabbed interface.
 */
@GrabConfig(systemClassLoader=true)
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')
@Grab('org.openjfx:javafx-base:23.0.2')
@Grab('org.openjfx:javafx-graphics:23.0.2')
@Grab('org.openjfx:javafx-controls:23.0.2')

import groovy.transform.SourceURI
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.girod.javafx.svgimage.SVGImage
import se.alipsa.matrix.chartexport.ChartToJfx

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

        // FlowPane for wrapping tab buttons (multi-row like Swing)
        FlowPane tabBar = new FlowPane()
        tabBar.hgap = 4
        tabBar.vgap = 4
        tabBar.padding = new Insets(8)
        tabBar.style = '-fx-background-color: #e0e0e0;'

        ToggleGroup toggleGroup = new ToggleGroup()

        // Content area that switches based on selected tab
        StackPane contentArea = new StackPane()

        // Map to store content for each tab
        Map<ToggleButton, Node> tabContents = [:]

        svgFiles.each { File svgFile ->
            String svgContent = svgFile.text
            String tabName = svgFile.name.replace('.svg', '')

            ToggleButton tabButton = new ToggleButton(tabName)
            tabButton.toggleGroup = toggleGroup
            tabButton.style = '''
                -fx-background-radius: 4 4 0 0;
                -fx-padding: 6 12;
                -fx-font-size: 12px;
            '''

            Node content
            try {
                SVGImage svgImage = ChartToJfx.export(svgContent)

                // Wrap in a StackPane for centering
                StackPane container = new StackPane(svgImage)

                // Wrap in ScrollPane for large charts
                ScrollPane scrollPane = new ScrollPane(container)
                scrollPane.fitToWidth = true
                scrollPane.fitToHeight = true

                content = scrollPane
                println "  Loaded: ${svgFile.name}"
            } catch (Exception e) {
                // Create an error label for failed SVGs
                Label errorLabel = new Label("Failed to load SVG:\n${e.message}")
                errorLabel.style = '-fx-text-fill: red; -fx-font-size: 14px;'
                content = new StackPane(errorLabel)
                tabButton.text = tabName + " (error)"
                println "  Failed to load: ${svgFile.name} - ${e.message}"
            }

            tabContents[tabButton] = content
            tabBar.children.add(tabButton)
        }

        // Handle tab selection
        toggleGroup.selectedToggleProperty().addListener { obs, oldVal, newVal ->
            if (newVal != null) {
                contentArea.children.clear()
                contentArea.children.add(tabContents[newVal])
            }
        }

        // Select first tab (the listener will handle adding the content)
        if (!tabBar.children.isEmpty()) {
            ToggleButton firstTab = tabBar.children[0] as ToggleButton
            firstTab.selected = true
        }

        BorderPane root = new BorderPane()
        root.top = tabBar
        root.center = contentArea

        Scene scene = new Scene(root, 900, 700)
        primaryStage.title = "Chart Examples Viewer (JavaFX)"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

Application.launch(ChartViewerApp)
