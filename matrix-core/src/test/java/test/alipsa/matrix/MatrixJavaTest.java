package test.alipsa.matrix;

import org.junit.jupiter.api.Test;
import se.alipsa.groovy.matrix.*;
import se.alipsa.groovy.matrix.util.Columns;
import se.alipsa.groovy.matrix.util.RowCriteriaClosure;
import se.alipsa.groovy.matrix.util.ObjectCriteriaClosure;
import se.alipsa.groovy.matrix.util.ValueClosure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static se.alipsa.groovy.matrix.ListConverter.toLocalDates;
import static se.alipsa.groovy.matrix.ListConverter.toYearMonth;
import static se.alipsa.groovy.matrix.ValueConverter.asLocalDate;
import static se.alipsa.groovy.matrix.ValueConverter.asYearMonth;
import static se.alipsa.groovy.matrix.util.CollectionUtils.*;

/**
 * Testing that a Matrix can work fine outside Groovy e.g. used in Java
 */
class MatrixJavaTest {

  @Test
  void testMatrixConstructors() {
    Columns columns = new Columns();
    columns.add("emp_id", 1, 2, 3, 4, 5);
    columns.add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary");
    columns.add("salary", 623.3, 515.2, 611.0, 729.0, 843.25);
    columns.add("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"));
    var empData = new Matrix("empData", columns,
        c(int.class, String.class, Number.class, LocalDate.class)
    );
    assertEquals("empData", empData.getName());
    assertEquals(1, (int) empData.getAt(0, 0));
    assertEquals("Dan", empData.getAt(1, 1));
    assertEquals(611.0, empData.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), empData.getAt(4, 3));
    assertIterableEquals(
        c(Integer.class, String.class, Number.class, LocalDate.class),
        empData.columnTypes()
    );

    var dims = empData.dimensions();
    assertEquals(5, dims.get("observations"));
    assertEquals(4, dims.get("variables"));

    var ed = new Matrix("ed",
        c("id", "name", "salary", "start"), c(
        c(1, 2, 3, 4, 5),
        c("Rick", "Dan", "Michelle", "Ryan", "Gary"),
        c(623.3, 515.2, 611.0, 729.0, 843.25),
        toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ));
    assertEquals("ed", ed.getName());
    assertEquals(1, ed.getAt(0, 0, int.class));
    assertEquals("Dan", ed.getAt(1, 1));
    assertEquals(611.0, ed.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), ed.getAt(4, 3));
    assertIterableEquals(
        c(Object.class, Object.class, Object.class, Object.class),
        ed.columnTypes()
    );

    var e = new Matrix(c(
        c(1, 2, 3, 4, 5),
        c("Rick", "Dan", "Michelle", "Ryan", "Gary"),
        c(623.3, 515.2, 611.0, 729.0, 843.25),
        toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ));
    assertNull(e.getName());
    assertEquals(1, (int) e.getAt(0, 0));
    assertEquals("Dan", e.getAt(1, 1));
    assertEquals(611.0, e.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), e.getAt(4, 3));
    assertIterableEquals(
        c(Object.class, Object.class, Object.class, Object.class),
        ed.columnTypes()
    );

    Matrix m = new Matrix("years", Stream.of(1, 2, 3, 4, 5).map(it -> "Y" + it).collect(Collectors.toList()));
    m.addRow(c(1, 2, 3, 4, 5));
    m.addRow(c(10, 20, 30, 40, 50));
    assertIterableEquals(c("Y1", "Y2", "Y3", "Y4", "Y5"), m.columnNames());
    assertIterableEquals(c(1, 2, 3, 4, 5), m.row(0));
    assertIterableEquals(c(10, 20, 30, 40, 50), m.row(1));
  }

  @Test
  void testTableCreationFromMatrix() {
    var employees = new ArrayList<List<?>>();
    employees.add(c("John Doe", 21000, asLocalDate("2013-11-01"), asLocalDate("2020-01-10")));
    employees.add(c("Peter Smith", 23400, "2018-03-25", "2020-04-12"));
    employees.add(c("Jane Doe", 26800, asLocalDate("2017-03-14"), asLocalDate("2020-10-02")));

    var table = Matrix.create(employees);
    assertEquals("John Doe", table.getAt(0, 0));
    assertEquals(23400, table.getAt(1, 1, Integer.class));
    assertEquals(LocalDate.of(2017, 3, 14), table.getAt(2, 2));
  }


  @Test
  void testTransposing() {
    var report = new Columns(
        m("Year", r(1, 4)),
        m("Full Funding", 4563.153, 380.263, 4.938, 101.1),
        m("Baseline Funding", 3385.593, 282.133, 3.664, 123.123),
        m("Current Funding", 2700, 225, 2.922, 1010.12)
    );
    var table = new Matrix(report);

    var tr = table.transpose(c("y1", "y2", "y3", "y4"));
    assertEquals(c("y1", "y2", "y3", "y4"), tr.columnNames());
    assertEquals(cg(
        c(1, 2, 3, 4),
        c(4563.153, 380.263, 4.938, 101.1),
        c(3385.593, 282.133, 3.664, 123.123),
        c(2700, 225, 2.922, 1010.12)
    ), tr.rows(), table.content());
    assertEquals(4, tr.columnTypes().size(), "Column types");

    assertEquals(cg(
        c(1, 2, 3, 4),
        c(4563.153, 380.263, 4.938, 101.1),
        c(3385.593, 282.133, 3.664, 123.123),
        c(2700, 225, 2.922, 1010.12)
    ), tr.rowList(), table.content());

    var tr2 = table.transpose(true);
    assertEquals(cg(
        c("Year", 1, 2, 3, 4),
        c("Full Funding", 4563.153, 380.263, 4.938, 101.1),
        c("Baseline Funding", 3385.593, 282.133, 3.664, 123.123),
        c("Current Funding", 2700, 225, 2.922, 1010.12)
    ), tr2.rows());
    assertEquals(5, tr2.columnTypes().size(), tr2.content() + "\nColumn types: " + tr2.columnTypeNames());

    var t3 = table.transpose("Year", true);
    assertEquals(cg(
        c("Year", 1, 2, 3, 4),
        c("Full Funding", 4563.153, 380.263, 4.938, 101.1),
        c("Baseline Funding", 3385.593, 282.133, 3.664, 123.123),
        c("Current Funding", 2700, 225, 2.922, 1010.12)
    ), t3.rows(), t3.content());
    assertEquals(c("", "1", "2", "3", "4"), t3.columnNames());
    assertEquals(5, t3.columnTypes().size(), t3.content() + "\nColumn types: " + t3.columnTypeNames());

    var t4 = table.transpose(
        "Year",
        c(String.class, Number.class, Number.class, Number.class, Number.class),
        true);
    assertEquals(c(
        c("Year", 1, 2, 3, 4),
        c("Full Funding", 4563.153, 380.263, 4.938, 101.1),
        c("Baseline Funding", 3385.593, 282.133, 3.664, 123.123),
        c("Current Funding", 2700, 225, 2.922, 1010.12)
    ), t4.rows(), t4.content());
    assertEquals(c("", "1", "2", "3", "4"), t4.columnNames());
    assertEquals(5, t4.columnTypes().size(), t4.content() + "\nColumn types: " + t4.columnTypeNames());

  }


  @Test
  void testStr() {
    var empData = new Matrix(new Columns()
        .add("emp_id", r(1, 5))
        .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
        .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
        .add("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")),
        c(int.class, String.class, Number.class, LocalDate.class)
    );
    Structure struct = Stat.str(empData);
    assertEquals(c("5 observations of 4 variables"), struct.getAt("Matrix"));
    assertIterableEquals(c("Integer", "1", "2", "3", "4"), struct.getAt("emp_id"));
    assertIterableEquals(c("LocalDate", "2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11"), struct.getAt("start_date"));
  }


  @Test
  void testCsv() throws IOException {
    var data = c(
        c("place", "firstname", "lastname", "team"),
        c("1", "Lorena", "Wiebes", "Team DSM"),
        c("2", "Marianne", "Vos", "Team Jumbo Visma"),
        c("3", "Lotte", "Kopecky", "Team SD Worx")
    );
    var file = File.createTempFile("FemmesStage1Podium", ".csv");
    try (FileWriter fw = new FileWriter(file)) {
      for (List<String> row : data) {
        fw.write(String.join(",", row) + '\n');
      }
    }
    var table = Matrix.create(file);
    assertIterableEquals(data.get(0), table.columnNames());
    assertEquals(data.get(1).get(1), table.getAt(0, 1));
    assertEquals("Team SD Worx", table.getAt(2, 3));

    var plantGrowth = Matrix.create(
        getClass().getResource("/PlantGrowth.csv"),
        ",",
        "\""
    );
    assertEquals("PlantGrowth", plantGrowth.getName());
    assertIterableEquals(c("id", "weight", "group"), plantGrowth.columnNames());
    var row30 = plantGrowth.findFirstRow("id", "30");
    assertEquals("5.26", row30.get(1));
    assertEquals("trt2", row30.get(2));
  }

  @Test
  void testConvert() {
    var data = new Columns(
        m("place", "1", "2", "3", ","),
        m("firstname", "Lorena", "Marianne", "Lotte", "Chris"),
        m("start", "2021-12-01", "2022-07-10", "2023-05-27", "2023-01-10"),
        m("end", "2022-12-01 10:00:00", "2023-07-10 00:01:00", "2024-05-27 00:00:30", "2042-01-10 00:00:00")
    );
    var table = new Matrix(data, cr(String.class, 4));

    var table2 = table.convert(Map.of("place", Integer.class, "start", LocalDate.class));
    table2 = table2.convert(Map.of("end", LocalDateTime.class),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    assertEquals(Integer.class, table2.columnType("place"));
    assertEquals(Integer.class, table2.getAt(0, 0).getClass());

    assertEquals(LocalDate.class, table2.columnType("start"));
    assertEquals(LocalDate.class, table2.getAt(0, 2).getClass());
    assertEquals(LocalDateTime.parse("2022-12-01T10:00:00.000"), table2.getAt("end").get(0));

    var table3 = table.convert("place", Integer.class, new ValueClosure<>(it -> {
        String val = String.valueOf(it).trim();
        if ("null".equals(val) || ",".equals(val) || val.isBlank()) return null;
        return Integer.valueOf(val);
      }
    ));

    assertEquals(Integer.class, table3.columnType("place"));
    assertEquals(3, table3.getAt("place").get(2));


    var table4 = table.convert(new Converter[]{
        new Converter("place", Integer.class, new ValueClosure<Integer, String>(it -> {
            try {
              return Integer.parseInt(it);
            } catch (NumberFormatException e) {
              return null;
            }
          }
        )),
        new Converter("start", LocalDate.class,
            new ValueClosure<LocalDate, String>(LocalDate::parse))
    });

    //println table.content()
    //println table4.content()
    assertEquals(Integer.class, table4.columnType("place"));
    assertEquals(Integer.class, table4.getAt(0, 0).getClass());
    assertEquals(3, table4.getAt(2, 0, Integer.class));

    assertEquals(LocalDate.class, table4.columnType("start"));
    assertEquals(LocalDate.class, table4.getAt(0, 2).getClass());
    assertEquals(LocalDate.of(2023, 5, 27), table4.getAt(2, 2));

    var table5 = table.convert(
        c(Integer.class, String.class, LocalDate.class, String.class)
    );
    assertEquals(table4, table5, table4.diff(table5));
  }


  @Test
  void testGetRowsForCriteria() {
    var data = new Columns()
        .add("place", 1, 2, 3)
        .add("firstname", "Lorena", "Marianne", "Lotte")
        .add("start", "2021-12-01", "2022-07-10", "2023-05-27");
    var table = new Matrix(data, c(int.class, String.class, String.class));
    List<Integer> idx = (List<Integer>) table.getAt("place").stream().filter(it -> (Integer) it > 1).collect(Collectors.toList());
    var rows = table.rows(idx);
    assertEquals(2, rows.size());

    // Same thing using subset
    var subSet = table.subset("place", new ObjectCriteriaClosure(it ->
        ((Integer)it) > 1
    ));
    assertIterableEquals(table.rows(c(1, 2)), subSet.rows());

    var subSet2 = table.subset(new RowCriteriaClosure(it ->
        it.getAt(0, Integer.class) > 1
    ));
    assertIterableEquals(table.rows(c(1, 2)), subSet2.rows());

    var subSet3 = table.subset(new RowCriteriaClosure(it -> {
        String name = it.getAt(1, String.class);
        return !name.startsWith("Ma")
            && asLocalDate(it.getAt(2, LocalDate.class)).isBefore(LocalDate.of(2022, 10, 1));
      }
    ));
    assertEquals(table.getAt(0, 1, String.class), subSet3.getAt(0, 1, String.class));
  }


  @Test
  void testHeadAndTail() {
    var table = new Matrix(new Columns()
        .add("place", 1, 20, 3)
        .add("firstname", "Lorena", "Marianne", "Lotte")
        .add("start", "2021-12-01", "2022-07-10", "2023-05-27")
        ,
        c(int.class, String.class, String.class)
    );
    var head = table.head(1, false);
    assertEquals(" 1\tLorena  \t2021-12-01\n", head, head);
    var tail = table.tail(2, false);
    assertEquals("20\tMarianne\t2022-07-10\n 3\tLotte   \t2023-05-27\n", tail, tail);
    String[] content = table.content(Map.of("includeHeader", false, "maxColumnLength", 7)).split("\n");
    assertEquals(" 1\tLorena \t2021-12", content[0]);
  }


  @SuppressWarnings("SqlNoDataSourceInspection")
  @Test
  void testCreateFromDb() throws SQLException, ClassNotFoundException {
    var dbDriver = "org.h2.Driver";
    var dbFileName = System.getProperty("java.io.tmpdir") + "/testdb";
    var dbUrl = "jdbc:h2:file:" + dbFileName;
    var dbUser = "sa";
    var dbPasswd = "123";

    File dbFile = new File(dbFileName + ".mv.db");
    if (dbFile.exists()) {
      dbFile.delete();
    }
    File dbTraceFile = new File(dbFileName + "trace.db");
    if (dbTraceFile.exists()) {
      dbTraceFile.delete();
    }
    Properties props = new Properties();
    props.setProperty("user", dbUser);
    props.setProperty("password", dbPasswd);

    Class.forName(dbDriver);
    try (Connection con = DriverManager.getConnection(dbUrl, props); Statement sql = con.createStatement()) {
      sql.execute("""
              create table IF NOT EXISTS PROJECT  (
                  id integer not null primary key,
                  name varchar(50),
                  url varchar(100)
              )
          """);
      sql.execute("delete from PROJECT");
      sql.execute("insert into PROJECT (id, name, url) values (10, 'Groovy', 'http://groovy.codehaus.org')");
      sql.execute("insert into PROJECT (id, name, url) values (20, 'Alipsa', 'http://www.alipsa.se')");
    }

    Matrix project;
    try (Connection con = DriverManager.getConnection(dbUrl, props); Statement sql = con.createStatement()) {
      var rs = sql.executeQuery("SELECT * FROM PROJECT");
      project = Matrix.create(rs);
    }

    assertEquals(2, project.rowCount());
  }


    @Test
    void testSelectRows() {
        var data = new Columns(
            m("place", 1, 2, 3),
            m("firstname", "Lorena", "Marianne", "Lotte"),
            m("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
        );
        var table = new Matrix(data, c(int.class, String.class, LocalDate.class));
        var selection = table.selectRowIndices(new RowCriteriaClosure(it ->
            it.getAt(2, LocalDate.class)
                .isAfter(LocalDate.of(2022,1, 1))
        ));
        assertIterableEquals(c(1,2), selection);

        var rows = table.rows(new RowCriteriaClosure(it ->
            it.getAt(2, LocalDate.class)
                .isAfter(LocalDate.of(2022, 10, 1))
        ));
        assertIterableEquals(
            c(c(3, "Lotte", LocalDate.of(2023, 5, 27))),
            rows
        );
    }


    @Test
    void testApply() {
        var data = new Columns()
            .add("place", "1", "2", "3", ",")
            .add("firstname", "Lorena", "Marianne", "Lotte", "Chris")
            .add("start", "2021-12-01", "2022-07-10", "2023-05-27", "2023-01-10");

        var table = new Matrix(data)
            .convert(Map.of("place", int.class, "start", LocalDate.class));
        var table2 = table.apply("start",
            new ValueClosure<LocalDate, LocalDate>(startDate -> startDate.plusDays(10)
            ));
        assertEquals(LocalDate.of(2021, 12, 11), table2.getAt("start").get(0));
        assertEquals(LocalDate.of(2022, 7, 20), table2.getAt(1, "start"));
        assertEquals(LocalDate.of(2023, 6, 6), table2.getAt("start").get(2));
        assertEquals(LocalDate.of(2023, 1, 20), table2.getAt(3, "start"));
        assertEquals(LocalDate.class, table2.columnType("start"));
    }


    @Test
    void testApplyChangeType() {
        var data = new Columns(
            m("foo", 1, 2, 3),
            m("firstname", "Lorena", "Marianne", "Lotte"),
            m("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
        );

        var table = new Matrix(data, c(int.class, String.class, LocalDate.class));

        var foo = table.apply("start", new ValueClosure<>(ValueConverter::asYearMonth));
        assertEquals(YearMonth.of(2021,12), foo.getAt(0, 2));
        assertEquals(YearMonth.class, foo.columnType("start"));
    }

  
    @Test
    void testSelectRowsAndApply() {
        var data = new Columns(m("place", 1, 2, 3))
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"));
        var table = new Matrix(data, c(int.class, String.class, LocalDate.class));
        assertEquals(Integer.class, table.columnType(0), "place column type");
        var selection = table.selectRowIndices( new RowCriteriaClosure(it -> {
            var date = it.getAt(2, LocalDate.class);
            return date.isAfter(LocalDate.of(2022,1, 1));
          }
        ));
        assertIterableEquals(c(1,2), selection);

        var foo = table.apply("place", selection,
            new ValueClosure<Integer, Integer>( it -> it * 2));
        //println(foo.content())
        assertEquals(4, foo.getAt(1, 0, Integer.class));
        assertEquals(6, foo.getAt(2, 0, Integer.class));
        assertEquals(LocalDate.class, foo.columnType(2));
        assertEquals(Integer.class, foo.columnType(0), "place column type");

        var bar = table.apply("place", new RowCriteriaClosure(it -> {
            var date = it.getAt(2, LocalDate.class);
            return date.isAfter(LocalDate.of(2022,1, 1));
        }), new ValueClosure<Integer, Integer>(it -> it * 2));
        //println(bar.content())
        assertEquals(4, bar.getAt(1, 0, Integer.class));
        assertEquals(6, bar.getAt(2, 0, Integer.class));
        assertEquals(LocalDate.class, bar.columnType(2), "start column type");
        assertEquals(Integer.class, bar.columnType(0), "place column type");

        var r = table.rows(new RowCriteriaClosure(row -> row.getAt("place", Integer.class) == 2));
        assertEquals(c(2, "Marianne", LocalDate.parse("2022-07-10")), r.get(0));

        // An item in a Row can also be referenced by the column name
        Row r2 = table.rows().stream().filter(row -> row.getAt("place", Integer.class) == 3).findFirst().orElse(null);
        assertIterableEquals(c(3, "Lotte", LocalDate.parse("2023-05-27")), r2, String.valueOf(r2));
    }


    @Test
    void testAddColumn() {
        var empData = new Matrix( new Columns(
            m("emp_id", r(1,5)),
            m("emp_name", "Rick","Dan","Michelle","Ryan","Gary"),
            m("salary", 623.3,515.2,611.0,729.0,843.25),
            m("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
            ), c(int.class, String.class, Number.class, LocalDate.class)
        );
        var table = empData.clone().addColumn("yearMonth", YearMonth.class, toYearMonth(empData.column("start_date")));
        assertEquals(5, table.columnCount());
        assertEquals("yearMonth", table.columnNames().get(table.columnCount()-1));
        assertEquals(YearMonth.class, table.columnType("yearMonth"));
        assertEquals(YearMonth.of(2012, 1), table.getAt(0,4));
        assertEquals(YearMonth.of(2015, 3), table.getAt(4,4));

        // Append a new column to the end
        Matrix table2 = empData.clone();
        table2.putAt("yearMonth", YearMonth.class, toYearMonth(table2.getAt("start_date")));
        assertEquals(empData.columnCount() + 1, table2.columnCount());
        assertEquals("yearMonth", table2.columnNames().get(table2.columnCount()-1));
        assertEquals(YearMonth.class, table2.columnType("yearMonth"));
        assertEquals(YearMonth.of(2012, 1), table2.getAt(0,4));
        assertEquals(YearMonth.of(2015, 3), table2.getAt(4,4));

        // Insert a new column first
        Matrix table3 = empData.clone();
        table3.putAt("yearMonth", YearMonth.class, 0, toYearMonth(table3.getAt("start_date")));
        assertEquals(empData.columnCount() + 1, table3.columnCount());
        assertEquals("yearMonth", table3.columnNames().get(0));
        assertEquals(YearMonth.class, table3.columnType("yearMonth"));
        assertEquals(YearMonth.of(2012, 1), table3.getAt(0,0));
        assertEquals(YearMonth.of(2015, 3), table3.getAt(4,0));
    }

    /*
    @Test
    void testAddColumns() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2019-01-01", "2019-01-23", "2019-05-15", "2019-05-11", "2019-03-27"),
            [int, String, Number, LocalDate]
        )

        empData = empData.addColumn("yearMonth", YearMonth, toYearMonth(empData["start_date"]))
        assertEquals(YearMonth, empData[0,4].class, "type of the added column")
        assertEquals(YearMonth, empData.columnType("yearMonth"), "claimed type of the added column")

        def counts = Stat.countBy(empData, "yearMonth").orderBy('yearMonth')
        assertEquals(YearMonth, counts[0,0].class, "type of the count column")
        assertEquals(YearMonth, counts.columnType("yearMonth"), "claimed type of the count column")

        assertEquals(2, counts.subset('yearMonth', {it == YearMonth.of(2019,5)})[0,1])
        assertEquals(1, counts.subset('yearMonth', {it == YearMonth.of(2019,3)})['yearMonth_count'][0])
        assertEquals(2, counts[0, 'yearMonth_count'])

        def sums = Stat.sumBy(empData, "salary", "yearMonth").orderBy("yearMonth", true)
        assertEquals(YearMonth, sums[0,0].class, "type of the sums column")
        assertEquals(YearMonth, sums.columnType("yearMonth"), "claimed type of the sums column")
        assertEquals(611.0 + 729.0, sums[0, 1], sums.content())
        assertEquals(843.25, sums[1, 1], sums.content())
        assertEquals(623.3 + 515.2, sums[2, 1], sums.content())

        def salaryPerYearMonth = counts
                .orderBy("yearMonth", true)
                .addColumns(sums, "salary")

        assertEquals(asYearMonth("2019-05"), salaryPerYearMonth[0, 0], salaryPerYearMonth.content())
        assertEquals(611.0 + 729.0, salaryPerYearMonth[0, 2], salaryPerYearMonth.content())
        assertEquals(2, salaryPerYearMonth[0, 1], salaryPerYearMonth.content())
        assertEquals(843.25, salaryPerYearMonth[1, 2], salaryPerYearMonth.content())
        assertEquals(1, salaryPerYearMonth[1, 1], salaryPerYearMonth.content())
    }*/

    /*
    @Test
    void testSort() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [Integer, String, Number, LocalDate]
        )
        def dateSorted = empData.orderBy("start_date")
        assertEquals(4, dateSorted[4, 0], "Last row should be the Ryan row: \n${dateSorted.content()}")
        assertEquals(asLocalDate("2012-03-27"), dateSorted[0, 3], "First row should be the Dan Row")

        //println(empData.content())
        def salarySorted = empData.orderBy(["salary": Matrix.DESC])
        assertEquals(843.25, salarySorted["salary"][0], "Highest salary: ${salarySorted.content()}")
        assertEquals(515.2, salarySorted["salary"][4], "Lowest salary: ${salarySorted.content()}")
    }*/

    /*
    @Test
    void testDropColumns() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )
        def empList = empData.dropColumns("salary", "start_date")
        //println(empList.content())
        assertEquals(2, empList.columnCount(), "Number of columns after drop")
        assertEquals(5, empList.rowCount(), "Number of rows after drop")
        assertIterableEquals(["emp_id",	"emp_name"], empList.columnNames(), "column names after drop")
        assertIterableEquals([Integer, String], empList.columnTypes(), "Column types after drop")
    }*/

    /*
    @Test
    void testDropColumnsExcept() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )
        def empList = empData.dropColumnsExcept("emp_id", "start_date")
        //println(empList.content())
        assertEquals(2, empList.columnCount(), "Number of columns after drop")
        assertEquals(5, empList.rowCount(), "Number of rows after drop")
        assertIterableEquals(["emp_id", "start_date"], empList.columnNames(), "column names after drop")
        assertIterableEquals([Integer, LocalDate], empList.columnTypes(), "Column types after drop")
    }*/

    /*
    @Test
    void testIteration() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )

        int i = 1
        empData.each { row ->
            assertEquals(i, row[0], String.valueOf(row))
            assertEquals(empData[i-1, 'emp_name'], row[1], String.valueOf(row))
            i++
        }

        for (row in empData) {
            if (row[2] > 600) {
                row[2] = row[2] - 600
            }
        }

        assertEquals(23.3, empData[0, 2], empData.toMarkdown())
        assertEquals(515.2, empData[1, 2], empData.toMarkdown())
        assertEquals(11.0, empData[2, 2], empData.toMarkdown())
        assertEquals(129.0, empData[3, 2], empData.toMarkdown())
        assertEquals(243.25, empData[4, 2], empData.toMarkdown())
    }*/

    /*
    @Test
    void testMatrixToGrid() {
        def report = [
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [BigDecimal]*3)

        Grid grid = table.grid()
        assertEquals(3.664, grid[2,1] as BigDecimal)
    }*/

    /*
    @Test
    void testSelectColumns() {
        def report = [
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Base Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [BigDecimal]*3)
            .selectColumns("Base Funding", "Full Funding")

        assertEquals(3385.593, table[0,0])
        assertEquals(12.23, table[3,1])

    }*/

    /*
    @Test
    void testRenameColumns() {
        def empData = new Matrix (
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )

        empData.renameColumn('emp_id', 'id')
        empData.renameColumn(1, 'name')

        assertEquals('id', empData.columnNames()[0])
        assertEquals('name', empData.columnNames()[1])

    }*/

    /*
    @Test
    void testToMarkdown() {
        def report = [
            "YearMonth": toYearMonth(['2023-01', '2023-02', '2023-03', '2023-04']),
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [YearMonth, BigDecimal, BigDecimal, BigDecimal])

        def md = table.toMarkdown()
        def rows = md.split('\n')
        assertEquals(6, rows.length)
        assertEquals('| YearMonth | Full Funding | Baseline Funding | Current Funding |', rows[0])
        assertEquals('| --- | ---: | ---: | ---: |', rows[1])
        assertEquals('| 2023-01 | 4563.153 | 3385.593 | 2700 |', rows[2])
        assertEquals('| 2023-04 | 12.23 | 2.654 | 1.871 |', rows[5])

        md = table.toMarkdown(Map.of("class", "table"))
        rows = md.split('\n')
        assertEquals(7, rows.length)
        assertEquals('{class="table" }', rows[6])
    }*/

    /*
    @Test
    void testEquals() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )

        assertEquals(empData, new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        ))

        assertNotEquals(empData, new Matrix (
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.1],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        ))

        Matrix differentTypes = new Matrix (
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [Object, Object, Object, Object]
        )
        assertEquals(empData,differentTypes , empData.diff(differentTypes))
        assertNotEquals(empData,differentTypes.withName("differentTypes") , empData.diff(differentTypes))
    }*/

    /*
    @Test
    void testDiff() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )
        def d1 = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.1],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )
        assertEquals('Row 1 differs: this: 2, Dan, 515.2, 2012-03-27; that: 2, Dan, 515.1, 2012-03-27',
                    empData.diff(d1).trim())

        def d2 = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [Object, Object, Object, Object]
        )
        assertEquals('Column types differ: this: Integer, String, Number, LocalDate; that: Object, Object, Object, Object',
            empData.diff(d2))
    }*/

    /*
    @Test
    void testRemoveEmptyRows() {
        def empData = new Matrix(
                emp_id: [1,2],
                emp_name: ["Rick","Dan"],
                salary: [623.3,515.2],
                start_date: toLocalDates("2013-01-01", "2012-03-27"),
                [int, String, Number, LocalDate]
        )

        def d0 = new Matrix(
                emp_id: [1,null, 2, null],
                emp_name: ["Rick", "", "Dan", " "],
                salary: [623.3, null, 515.2, null],
                start_date: toLocalDates("2013-01-01", null, "2012-03-27", null),
                [int, String, Number, LocalDate]
        )
        assertEquals(empData, d0.removeEmptyRows(), empData.diff(d0))
    }*/

    /*
    @Test
    void testRemoveEmptyColumns() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: [null, null],
            salary: [623.3,515.2],
            start_date: [null, null],
            other: [null, null],
            [int, String, Number, LocalDate, String]
        )
        assertIterableEquals(['emp_id', 'emp_name', 'salary', 'start_date', 'other'], empData.columnNames())
        empData.removeEmptyColumns()
        assertEquals(2, empData.columnCount())
        assertIterableEquals(['emp_id', 'salary'], empData.columnNames())
    }
    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles()
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file)
            }
        }
        return directoryToBeDeleted.delete()
    }*/

    /*
    @Test
    void testWithColumns() {
        def table = new Matrix([
            a: [1,2,3,4,5],
            b: [1.2,2.3,0.7,1.3,1.9]
        ], [Integer, BigDecimal])

        def m = table.withColumns(['a', 'b']) { x, y -> x - y }
        assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], m)

        def n = table.withColumns([0,1] as Integer[]) { x, y -> x - y }
        assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], n)
    }*/

    /*
    @Test
    void testPopulateColumn() {
        Matrix components = new Matrix([
            id: [1,2,3,4,5],
            size: [1.2,2.3,0.7,1.3,1.9]
        ], [Integer, BigDecimal])
        components['id'] = [10, 11, 12, 13, 14]
        assertEquals(10, components[0, 'id'])
        assertEquals(13, components[3, 'id'])
        assertEquals(14, components[4, 'id'])
    }*/

    /*
    @Test
    void testMoveColumn() {
        def table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])

        table.moveColumn('foo', 0)
        assertIterableEquals(['foo', 'firstname', 'start'], table.columnNames())
        assertIterableEquals([1, 2, 3], table[0])
        assertIterableEquals([Integer, String, LocalDate], table.columnTypes())
    }*/

    /*
    @Test
    void testPutAt() {
        // putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
        def table = new Matrix([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo': [1, 2, 3]
        ], [String, LocalDate, int])
        table["yearMonth", YearMonth, 0] = toYearMonth(table["start"])
        assertEquals(4, table.columnCount())
        assertIterableEquals(['yearMonth', 'firstname', 'start', 'foo'], table.columnNames())
        assertIterableEquals(toYearMonth(['2021-12', '2022-07', '2023-05']), table[0])

        // putAt(List where, List<?> column)
        table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])
        table["start"] = table["start"].collect {it.plusDays(10)}
        assertEquals(3, table.columnCount())
        assertIterableEquals(['firstname', 'start', 'foo'], table.columnNames())
        // getAt and putAt should have the same semantics i refer to columns:
        assertIterableEquals(toLocalDates(['2021-12-11', '2022-07-20', '2023-06-06']), table[1])
        assertIterableEquals(table.column(2), table[2])
        assertIterableEquals(table.column("foo"), table["foo"])
    }*/

    /*
    @Test
    void testGetAt() {
        def table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])

        assertEquals(Integer, table.getAt(2, 2).class)
        assertEquals(3, table.getAt(2, 2))

        assertEquals(Integer, table[2, 2].class)
        assertEquals(3, table[2, 2])

        assertEquals(LocalDate, table[2, 'start'].class)
        assertEquals(asLocalDate('2023-05-27'), table[2, 'start'])

        assertEquals(asLocalDate('2023-05-27'), table.getAt(2, 'start'))
        assertEquals(LocalDate, table.getAt(2, 'start').class)


        Row row = table.row(1)
        assertEquals(LocalDate, row.getAt('start').class)
        assertEquals(LocalDate, row[1].class)
        assertEquals(LocalDate, row['start'].class)
        assertEquals(LocalDate.parse('2022-07-10'), row[1])
        assertEquals(LocalDate.parse('2022-07-10'), row['start'])

        assertEquals(String, table[0, 1, String].class)
        assertEquals('2021-12-01', table[0, 1, String])

        assertEquals(String, table[0, 'foo', String].class)
        assertEquals('3', table[2, 'foo', String])

        assertEquals(2 as BigDecimal, row['foo', BigDecimal])
        assertEquals(2 as BigDecimal, row.getAt('foo', BigDecimal))
        assertEquals('2', row['foo', String])
        assertEquals(2 as BigDecimal, row[2, BigDecimal])
        assertEquals('2', row[2, String])
        assertEquals('2', row.getAt(2, String))
    }*/
}
