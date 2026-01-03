/*
 * Swing application that runs all *Example.groovy scripts and displays
 * the generated SVG charts in a tabbed interface.
 */
@GrabConfig(systemClassLoader=true)
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel

import javax.swing.*
import java.awt.*

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

// Load SVG files and create Swing application
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

SwingUtilities.invokeLater {
    JFrame frame = new JFrame("Chart Examples Viewer")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(900, 700)

    JTabbedPane tabbedPane = new JTabbedPane()

    svgFiles.each { File svgFile ->
        String svgContent = svgFile.text
        String tabName = svgFile.name.replace('.svg', '')

        try {
            SvgPanel svgPanel = ChartToSwing.export(svgContent)

            // Wrap in a scroll pane for large charts
            JScrollPane scrollPane = new JScrollPane(svgPanel)
            scrollPane.border = BorderFactory.createEmptyBorder()

            tabbedPane.addTab(tabName, scrollPane)
            println "  Loaded: ${svgFile.name}"
        } catch (Exception e) {
            // Create an error panel for failed SVGs
            JPanel errorPanel = new JPanel(new BorderLayout())
            JLabel errorLabel = new JLabel("<html><center>Failed to load SVG:<br>${e.message}</center></html>")
            errorLabel.horizontalAlignment = SwingConstants.CENTER
            errorPanel.add(errorLabel, BorderLayout.CENTER)
            tabbedPane.addTab(tabName + " (error)", errorPanel)
            println "  Failed to load: ${svgFile.name} - ${e.message}"
        }
    }

    frame.contentPane.add(tabbedPane, BorderLayout.CENTER)
    frame.locationRelativeTo = null  // Center on screen
    frame.visible = true
}
