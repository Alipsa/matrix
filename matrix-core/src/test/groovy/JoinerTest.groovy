import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Joiner

import static org.junit.jupiter.api.Assertions.*

class JoinerTest {

    @Test
    void testInnerJoin() {
        def e = new Matrix('employees', [
                id: 1..5,
                firstName:["Rick","Dan","Michelle","Ryan","Gary"],
                salary:[623.3,515.2,611.0,729.0,843.25],
                startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ])

        def f = new Matrix('eln', [
                id: 1..5,
                lastName: ["Smith", "Carpenter", "Bowman", "Carson", "McDougal"]
        ])

        def merged = Joiner.merge(e, f, 'id')
        //println merged.toMarkdown()
        assertIterableEquals([1, 'Rick', 623.3, '2012-01-01', 'Smith'], merged.row(0))
        assertIterableEquals([5, 'Gary', 843.25, '2015-03-27', 'McDougal'], merged.row(4))


        // Make sure the original matrices was unaltered
        assertEquals(new Matrix('employees', [
                id: 1..5,
                firstName:["Rick","Dan","Michelle","Ryan","Gary"],
                salary:[623.3,515.2,611.0,729.0,843.25],
                startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ]), e)
    }

    @Test
    void testLeftJoin(){
        def e = new Matrix('employees', [
                id: 1..5,
                firstName:["Rick","Dan","Michelle","Ryan","Gary"],
                salary:[623.3,515.2,611.0,729.0,843.25],
                startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ])
        def g = new Matrix('eln', [
                employeeId: 2..4,
                lastName: ["Carpenter", "Bowman", "Carson"]
        ])
        def leftJoined = Joiner.merge(e, g, [x: 'id', y:'employeeId'], true)
        //println leftJoined.toMarkdown()
        assertEquals([1, 'Rick', 623.3, '2012-01-01', null], leftJoined.row(0))
        assertEquals([2, 'Dan', 515.2, '2013-09-23', 'Carpenter'], leftJoined.row(1))
        assertEquals([5, 'Gary', 843.25, '2015-03-27', null], leftJoined.row(4))

        // Make sure the original matrices was unaltered
        assertEquals(new Matrix('employees', [
                id: 1..5,
                firstName:["Rick","Dan","Michelle","Ryan","Gary"],
                salary:[623.3,515.2,611.0,729.0,843.25],
                startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ]), e)
        assertEquals(new Matrix('eln', [
                employeeId: 2..4,
                lastName: ["Carpenter", "Bowman", "Carson"]
        ]), g)
    }
}