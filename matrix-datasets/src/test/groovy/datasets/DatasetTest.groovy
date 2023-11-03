package datasets

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.*
import se.alipsa.groovy.matrix.*

class DatasetTest {

    @Test
    void testIris() {
        Matrix iris = Dataset.iris()
        assertEquals(150, iris.rowCount(), 'number of rows')
        assertEquals(5, iris.columnCount(), 'number of columns')
        assertEquals(876.5, Stat.sum(iris['Sepal Length']))
        assertEquals(Dataset.descIris(), Dataset.describe(iris))
        assertEquals(Dataset.descIris(), Dataset.describe('iris'))
        assertEquals(['Sepal Length','Sepal Width','Petal Length','Petal Width','Species'], iris.columnNames())
        def speciesIdx = iris.columnIndex("Species")
        def setosa = iris.subset {
            it[speciesIdx] == 'setosa'
        }
        def virginica = iris.subset {
            it[speciesIdx] == 'virginica'
        }
        assertEquals(1.462, Stat.mean(setosa['Petal Length']))
        assertEquals(5.552, Stat.mean(virginica['Petal Length']))
    }


    @Test
    void testMtcars() {
        def mtcars = Dataset.mtcars()
        assertEquals(32, mtcars.rowCount(), 'number of rows')
        assertEquals(12, mtcars.columnCount(), 'number of columns')
        assertEquals(642.9, Stat.sum(mtcars['mpg']), 'sum of mpg')
        assertEquals(Dataset.descMtcars(), Dataset.describe(mtcars))
        assertEquals(Dataset.descMtcars(), Dataset.describe('mtcars'))
    }

    @Test
    void testPlantGrowth() {
        def plantGrowth = Dataset.plantGrowth()
        assertEquals(30, plantGrowth.rowCount())
        assertEquals(3, plantGrowth.columnCount())
        assertEquals(152.19, Stat.sum(plantGrowth['weight']), 'sum of weight')
        assertEquals(Dataset.descPlantGrowth(), Dataset.describe(plantGrowth))
        assertEquals(Dataset.descPlantGrowth(), Dataset.describe('plantGrowth'))
    }

    @Test
    void testToothGrowth() {
        def toothGrowth = Dataset.toothGrowth()
        assertEquals(60, toothGrowth.rowCount())
        assertEquals(4, toothGrowth.columnCount())
        assertEquals(1128.8, Stat.sum(toothGrowth['len']), 'sum of len')
        assertEquals(Dataset.descToothGrowth(), Dataset.describe(toothGrowth))
        assertEquals(Dataset.descToothGrowth(), Dataset.describe('toothGrowth'))
    }

    @Test
    void testUsArrests() {
        def usArrests = Dataset.usArrests()
        assertEquals(50, usArrests.rowCount())
        assertEquals(5, usArrests.columnCount())
        assertEquals(389.4, Stat.sum(usArrests['Murder']), 'sum of Murder')
        assertEquals(Dataset.descUsArrests(), Dataset.describe(usArrests))
        assertEquals(Dataset.descUsArrests(), Dataset.describe('usArrests'))
    }

    @Test
    void testMpg() {
        def mpg = Dataset.mpg()
        assertEquals(234, mpg.rowCount())
        assertEquals(11, mpg.columnCount())
        assertEquals(812.4, Stat.sum(mpg['displ']), 'sum of displ')
        assertEquals(Dataset.descMpg(), Dataset.describe(mpg))
        assertEquals(Dataset.descMpg(), Dataset.describe('mpg'))
    }

    @Test
    void testDiamonds() {
        def diamonds = Dataset.diamonds()
        assertEquals(53940, diamonds.rowCount())
        assertEquals(10, diamonds.columnCount())
        assertEquals(43040.87, Stat.sum(diamonds['carat']), 'sum of carat')
        assertEquals(Dataset.descDiamonds(), Dataset.describe(diamonds))
        assertEquals(Dataset.descDiamonds(), Dataset.describe('diamonds'))
    }

    @Test
    void testMapDataCounty() {
        def michigan = Dataset.mapData('county', 'michigan')
        assertEquals(1471, michigan.rowCount(), "county, michigan num rows")
        assertEquals(65938.658821106,
            Stat.sum(michigan['lat']).setScale(9, RoundingMode.HALF_EVEN),
            "county, michigan, sum of lat"
        )
    }

    @Test
    void testMapDataFrance() {
        def france = Dataset.mapData('france')
        assertEquals(13353, france.rowCount(), "france number of rows")
        assertEquals(32828.4454960893,
            Stat.sum(france["long"]).setScale(10, RoundingMode.HALF_EVEN),
            "france, sum of long"
        )
    }

    @Test
    void testMapDataItaly() {
        def italy = Dataset.mapData('italy')
        assertEquals(10284, italy.rowCount(), "italy number of rows")
        assertEquals(123984.6834597588,
            Stat.sum(italy["long"]).setScale(10, RoundingMode.HALF_EVEN),
            "italy, sum of long")
    }

    @Test
    void testMapDataNz() {
        def nz = Dataset.mapData('nz')
        assertEquals(1552, nz.rowCount(), "nz, number of rows")
        def nzArapawa = Dataset.mapData('nz', 'Arapawa.Island')
        assertEquals(11, nzArapawa.rowCount(), "nzArapawa number of rows")
        assertEquals(-453.1090660095,
            Stat.sum(nzArapawa["lat"]).setScale(10, RoundingMode.HALF_EVEN),
            "nzArapawa, sum of lat")
    }

    @Test
    void testMapDataState() {
        def state = Dataset.mapData('state')
        assertEquals(15537, state.rowCount(), "state, number of rows")
        def oregon = Dataset.mapData('state', 'oregon')
        assertEquals(236, oregon.rowCount(), "Oregon number of rows")
        assertEquals(10591.1076698303,
            Stat.sum(oregon["lat"]).setScale(10, RoundingMode.HALF_EVEN),
            "oregon, sum of lat")
    }

    @Test
    void testMapDataUsa() {
        def us = Dataset.mapData('usa')
        assertEquals(7243, us.rowCount(), "usa, number of rows")
        def manhattan = Dataset.mapData('usa', 'manhattan')
        assertEquals(16, manhattan.rowCount(), "Manhattan number of rows")
        assertEquals(652.4327774048,
            Stat.sum(manhattan["lat"]).setScale(10, RoundingMode.HALF_EVEN),
            "manhattan, sum of lat")
    }

    @Test
    void testMapDataWorld() {
        def world = Dataset.mapData('world')
        assertEquals(99338, world.rowCount(), "world, number of rows")
        def sweden = Dataset.mapData('world', 'Sweden')
        assertEquals(593, sweden.rowCount(), "Sweden number of rows")
        assertEquals(36704.2741737366,
            Stat.sum(sweden["lat"]).setScale(10, RoundingMode.HALF_EVEN),
            "Sweden, sum of lat")
    }

    @Test
    void testMapDataWorld2() {
        def world2 = Dataset.mapData('world2')
        assertEquals(99385, world2.rowCount(), "world2, number of rows")
        def sweden = Dataset.mapData('world2', 'Sweden')
        assertEquals(593, sweden.rowCount(), "Sweden number of rows")
        assertEquals(36704.2741737366,
            Stat.sum(sweden["lat"]).setScale(10, RoundingMode.HALF_EVEN),
            "Sweden, sum of lat")
    }
}
