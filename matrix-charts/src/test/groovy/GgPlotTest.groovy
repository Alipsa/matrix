import se.alipsa.groovy.matrix.Stat

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import static se.alipsa.groovy.gg.GgPlot.*

class GgPlotTest {

    def iris = Dataset.iris()
    def mtcars = Dataset.mtcars()

    @Test
    void testAes() {
        def a = aes(x:"Sepal Length", y:"Petal Length", col:"Species")
        assertEquals('Aes(xCol=Sepal Length, yCol=Petal Length, colorCol=Species)', a.toString())
    }
    @Test
    void testPoint(){
        ggplot(iris,
                aes(x:"Sepal Length", y:"Petal Length", col:"Species")
        ) + geom_point()

        ggplot(iris, aes(x:'Sepal Length', y:'Petal Length', col:'Species')) \
            + geom_point() + geom_smooth()

        // same thing with colors
        ggplot(iris, aes(x:'Sepal Length', y:'Petal Length', col:'Species')) \
            + geom_point(color:"blue") + geom_smooth(color:"red")
    }


    @Test
    void testVerticalBarPlot() {
        ggplot(mtcars, aes(x: 'gear')) + geom_bar()
    }

    @Test
    void testHorizontalBarPlot() {
        ggplot(mtcars, aes(x: 'gear')) +geom_bar() + coord_flip()
    }

    @Test
    void testStatisticalTransformation() {
        ggplot(mtcars, aes('hp', 'mpg')) + geom_point(color: "blue") \
        + stat_summary('fun.y': "mean", geom: "line", linetype: "dashed")
    }

    @Test
    void testRuggedLinePlot() {
        ggplot(mtcars, aes('hp', 'mpg')) + geom_point(color: "blue") \
        + geom_rug('show.legend': false) +stat_summary('fun.y': "mean",
                geom: "line", linetype: "dashed")
    }

    @Test
    void testHistogram() {
        ggplot(mtcars,aes(x:'mpg')) + geom_histogram()
    }

    @Test
    void testBoxPlot() {
        //ggplot(mtcars, aes(x=as.factor(cyl), y=mpg)) + geom_boxplot()
        ggplot(mtcars, aes(x:As.factor(mtcars['cyl']), y:'mpg')) + geom_boxplot()

        // Colored boxplot
        def cyl = As.factor(mtcars['cyl'])
        ggplot(mtcars, aes(x:(cyl), y:'mpg',color: 'cyl')) \
        + geom_boxplot() \
        + scale_color_manual(values: ["#3a0ca3", "#c9184a", "#3a5a40"])
    }

    @Test
    void testViolinPlot() {
        ggplot(mtcars, aes(As.factor(mtcars['cyl']), 'mpg')) + \
            geom_violin(aes(fill: 'cyl'))
    }

    @Test
    void testPieChart() {
        ggplot(mtcars, aes(x:"", y:"mpg", fill:"cyl")) \
            + geom_bar(stat:"identity", width:1) \
            + coord_polar(theta: "y", start:0)

        //mtcars %>%
        //dplyr::group_by(cyl) %>%
        //dplyr::summarize(mpg = median(mpg)) %>%
        //ggplot(aes(x = cyl, y = mpg)) + geom_col(aes(fill =cyl), color = NA) + labs(x = "", y = "Median mpg")  + coord_polar()
        Stat.medianBy(mtcars, 'cyl', 'mpg')
    }

}
