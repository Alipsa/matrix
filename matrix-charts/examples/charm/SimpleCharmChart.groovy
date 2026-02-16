import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.chartexport.ChartToPng

def data = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    sales: [120, 150, 180, 160, 200, 220],
    target: [130, 140, 170, 175, 190, 210]
).types(String, Integer, Integer).build()

def chart = plot(data) {
  aes {
    x = col.month
    y = col.sales
  }
  line {}
  points { size = 3 }
  labels {
    title = 'Monthly Sales'
    x = 'Month'
    y = 'Sales (units)'
  }
  theme {
    legend { position = 'none' }
  }
}.build()

// Write to SVG
chart.writeTo('sales_chart.svg')
println "Wrote sales_chart.svg"

// Export to PNG
ChartToPng.export(chart, new File('sales_chart.png'))
println "Wrote sales_chart.png"
