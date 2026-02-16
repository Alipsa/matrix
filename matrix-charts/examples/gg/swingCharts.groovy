/*
 * Swing application that runs all *Example.groovy scripts and displays
 * the generated SVG charts in a list/detail viewer.
 */
@GrabConfig(systemClassLoader=true)
@Grab('se.alipsa.matrix:matrix-core:3.7.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.5.0-SNAPSHOT')
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
    JFrame frame = new JFrame("Chart Examples Viewer (Swing)")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    int listWidth = 200

    DefaultListModel<File> listModel = new DefaultListModel<>()
    svgFiles.each { File svgFile ->
        listModel.addElement(svgFile)
    }

    JList<File> fileList = new JList<>(listModel)
    fileList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    fileList.cellRenderer = new DefaultListCellRenderer() {
        @Override
        Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value instanceof File) {
                String name = ((File) value).name.replace('.svg', '')
                setText(name)
            }
            return this
        }
    }

    JScrollPane listScrollPane = new JScrollPane(fileList)
    listScrollPane.preferredSize = new Dimension(listWidth, 0)

    JPanel displayPanel = new JPanel(new BorderLayout())
    File currentSelection = null

    def showSvg = { File svgFile ->
        displayPanel.removeAll()
        try {
            String svgContent = svgFile.text
            SvgPanel svgPanel = ChartToSwing.export(svgContent)
            Dimension svgSize = svgPanel.preferredSize

            // Wrap in a scroll pane for large charts
            JScrollPane scrollPane = new JScrollPane(svgPanel)
            scrollPane.border = BorderFactory.createEmptyBorder()
            displayPanel.add(scrollPane, BorderLayout.CENTER)
            int svgWidth = svgSize.width as int
            int svgHeight = svgSize.height as int
            displayPanel.preferredSize = new Dimension(svgWidth, svgHeight)
            listScrollPane.preferredSize = new Dimension(listWidth, svgHeight)
            println "  Loaded: ${svgFile.name}"
        } catch (Exception e) {
            // Create an error panel for failed SVGs
            JPanel errorPanel = new JPanel(new BorderLayout())
            JLabel errorLabel = new JLabel("<html><center>Failed to load SVG:<br>${e.message}</center></html>")
            errorLabel.horizontalAlignment = SwingConstants.CENTER
            errorPanel.add(errorLabel, BorderLayout.CENTER)
            displayPanel.add(errorPanel, BorderLayout.CENTER)
            println "  Failed to load: ${svgFile.name} - ${e.message}"
        }
        displayPanel.revalidate()
        displayPanel.repaint()
        currentSelection = svgFile
        frame.pack()
    }

    fileList.addListSelectionListener { event ->
        if (!event.valueIsAdjusting) {
            File selected = fileList.selectedValue
            if (selected != null && selected != currentSelection) {
                showSvg(selected)
            }
        }
    }

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, displayPanel)
    splitPane.dividerLocation = listWidth
    frame.contentPane.add(splitPane, BorderLayout.CENTER)
    if (listModel.size() > 0) {
        File firstFile = listModel.getElementAt(0)
        showSvg(firstFile)
        fileList.selectedIndex = 0
    }
    frame.locationRelativeTo = null  // Center on screen
    frame.visible = true
}
