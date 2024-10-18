package test.alipsa.matrix;

import groovy.lang.Closure;
import org.junit.jupiter.api.Test;
import se.alipsa.groovy.matrix.*;
import se.alipsa.groovy.matrix.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static se.alipsa.groovy.matrix.ListConverter.*;
import static se.alipsa.groovy.matrix.ValueConverter.*;
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
    var empData = Matrix.builder()
        .matrixName("empData")
        .columns(columns)
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();
    assertEquals("empData", empData.getMatrixName());
    assertEquals(1, (int) empData.getAt(0, 0));
    assertEquals("Dan", empData.getAt(1, 1));
    assertEquals(611.0, empData.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), empData.getAt(4, 3));
    assertIterableEquals(
        c(Integer.class, String.class, Number.class, LocalDate.class),
        empData.types()
    );

    var dims = empData.dimensions();
    assertEquals(5, dims.get("observations"));
    assertEquals(4, dims.get("variables"));

    var ed = Matrix.builder()
        .matrixName("ed")
        .columnNames("id", "name", "salary", "start")
        .columns(
            toIntegers(1, 2, 3, 4, 5),
            toStrings("Rick", "Dan", "Michelle", "Ryan", "Gary"),
            toBigDecimals(623.3, 515.2, 611.0, 729.0, 843.25),
            toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .build();
    assertEquals("ed", ed.getMatrixName());
    assertEquals(1, ed.getAt(0, 0, int.class));
    assertEquals("Dan", ed.getAt(1, 1));
    assertEquals(asBigDecimal("611.0"), ed.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), ed.getAt(4, 3));
    assertIterableEquals(
        c(Object.class, Object.class, Object.class, Object.class),
        ed.types()
    );

    var e = Matrix.builder().columns(c(
        toIntegers(1, 2, 3, 4, 5),
        toStrings("Rick", "Dan", "Michelle", "Ryan", "Gary"),
        toBigDecimals(623.3, 515.2, 611.0, 729.0, 843.25),
        toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    )).build();
    assertNull(e.getMatrixName());
    assertEquals(1, (int) e.getAt(0, 0));
    assertEquals("Dan", e.getAt(1, 1));
    assertEquals(asBigDecimal("611.0"), e.getAt(2, 2));
    assertEquals(LocalDate.of(2015, 3, 27), e.getAt(4, 3));
    assertIterableEquals(
        c(Object.class, Object.class, Object.class, Object.class),
        ed.types()
    );

    Matrix m = Matrix.builder()
        .matrixName("years")
        .columnNames(Stream.of(1, 2, 3, 4, 5).map(it -> "Y" + it).collect(Collectors.toList()))
        .build();
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

    var table = Matrix.builder().rows(employees).build();
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
    var table = Matrix.builder().data(report).build();

    var tr = table.transpose(c("y1", "y2", "y3", "y4"));
    assertEquals(c("y1", "y2", "y3", "y4"), tr.columnNames());
    assertEquals(cg(
        c(1, 2, 3, 4),
        c(4563.153, 380.263, 4.938, 101.1),
        c(3385.593, 282.133, 3.664, 123.123),
        c(2700, 225, 2.922, 1010.12)
    ), tr.rows(), table.content());
    assertEquals(4, tr.types().size(), "Column types");

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
    assertEquals(5, tr2.types().size(), tr2.content() + "\nColumn types: " + tr2.typeNames());

    var t3 = table.transpose("Year", true);
    assertEquals(cg(
        c("Year", 1, 2, 3, 4),
        c("Full Funding", 4563.153, 380.263, 4.938, 101.1),
        c("Baseline Funding", 3385.593, 282.133, 3.664, 123.123),
        c("Current Funding", 2700, 225, 2.922, 1010.12)
    ), t3.rows(), t3.content());
    assertEquals(c("", "1", "2", "3", "4"), t3.columnNames());
    assertEquals(5, t3.types().size(), t3.content() + "\nColumn types: " + t3.typeNames());

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
    assertEquals(5, t4.types().size(), t4.content() + "\nColumn types: " + t4.typeNames());

  }


  @Test
  void testStr() {
    var empData = Matrix.builder().data(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        )
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();
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
    var table = Matrix.builder().data(file).build();
    assertIterableEquals(data.get(0), table.columnNames());
    assertEquals(data.get(1).get(1), table.getAt(0, 1));
    assertEquals("Team SD Worx", table.getAt(2, 3));

    var plantGrowth = Matrix.builder()
        .data(getClass().getResource("/PlantGrowth.csv"), ",", "\"")
        .build();
    assertEquals("PlantGrowth", plantGrowth.getMatrixName());
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
    var table = Matrix.builder()
        .data(data)
        .types(cr(String.class, 4))
        .build();

    var table2 = table.clone().convert(Map.of("place", Integer.class, "start", LocalDate.class));
    table2 = table2.convert(Map.of("end", LocalDateTime.class),
        "yyyy-MM-dd HH:mm:ss");
    assertEquals(Integer.class, table2.type("place"));
    assertEquals(Integer.class, table2.getAt(0, 0).getClass());

    assertEquals(LocalDate.class, table2.type("start"));
    assertEquals(LocalDate.class, table2.getAt(0, 2).getClass());
    assertEquals(LocalDateTime.parse("2022-12-01T10:00:00.000"), table2.getAt("end").get(0));

    var table3 = table.clone().convert("place", Integer.class, new ValueClosure<>(it -> {
      String val = String.valueOf(it).trim();
      if ("null".equals(val) || ",".equals(val) || val.isBlank()) return null;
      return Integer.valueOf(val);
    }
    ));

    assertEquals(Integer.class, table3.type("place"));
    assertEquals(3, table3.getAt("place").get(2));


    var table4 = table.clone().convert(new Converter[]{
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
    assertEquals(Integer.class, table4.type("place"));
    assertEquals(Integer.class, table4.getAt(0, 0).getClass());
    assertEquals(3, table4.getAt(2, 0, Integer.class));

    assertEquals(LocalDate.class, table4.type("start"));
    assertEquals(LocalDate.class, table4.getAt(0, 2).getClass());
    assertEquals(LocalDate.of(2023, 5, 27), table4.getAt(2, 2));

    var table5 = table.clone().convert(
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
    var table = Matrix.builder()
        .data(data)
        .types(c(int.class, String.class, String.class))
        .build();
    List<Integer> idx = (List<Integer>) table.getAt("place").stream().filter(it -> (Integer) it > 1).collect(Collectors.toList());
    var rows = table.rows(idx);
    assertEquals(2, rows.size());

    // Same thing using subset
    var subSet = table.subset("place", new ObjectCriteriaClosure(it ->
        ((Integer) it) > 1
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
    var table = Matrix.builder().data(new Columns()
            .add("place", 1, 20, 3)
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", "2021-12-01", "2022-07-10", "2023-05-27")
        )
        .types(c(int.class, String.class, String.class))
        .build();
    var head = table.head(1, false);
    assertEquals(" 1\tLorena  \t2021-12-01\n", head, head);
    var tail = table.tail(2, false);
    assertEquals("20\tMarianne\t2022-07-10\n 3\tLotte   \t2023-05-27\n", tail, tail);
    String[] content = table.content(Map.of("includeHeader", false, "includeTitle", false, "maxColumnLength", 7)).split("\n");
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
      project = Matrix.builder().data(rs).build();
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
    var table = Matrix.builder().data(data).types(c(int.class, String.class, LocalDate.class)).build();
    var selection = table.selectRowIndices(new RowCriteriaClosure(it ->
        it.getAt(2, LocalDate.class)
            .isAfter(LocalDate.of(2022, 1, 1))
    ));
    assertIterableEquals(c(1, 2), selection);

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

    var table = Matrix.builder().data(data).build()
        .convert(Map.of("place", int.class, "start", LocalDate.class));
    var table2 = table.apply("start",
        new ValueClosure<LocalDate, LocalDate>(startDate -> startDate.plusDays(10)
        ));
    assertEquals(LocalDate.of(2021, 12, 11), table2.getAt("start").get(0));
    assertEquals(LocalDate.of(2022, 7, 20), table2.getAt(1, "start"));
    assertEquals(LocalDate.of(2023, 6, 6), table2.getAt("start").get(2));
    assertEquals(LocalDate.of(2023, 1, 20), table2.getAt(3, "start"));
    assertEquals(LocalDate.class, table2.type("start"));
  }


  @Test
  void testApplyChangeType() {
    var data = new Columns(
        m("foo", 1, 2, 3),
        m("firstname", "Lorena", "Marianne", "Lotte"),
        m("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
    );

    var table = Matrix.builder()
        .data(data)
        .types(int.class, String.class, LocalDate.class)
        .build();

    var foo = table.apply("start", new ValueClosure<>(ValueConverter::asYearMonth));
    assertEquals(YearMonth.of(2021, 12), foo.getAt(0, 2));
    assertEquals(YearMonth.class, foo.type("start"));
  }


  @Test
  void testSelectRowsAndApply() {
    var data = new Columns(m("place", 1, 2, 3))
        .add("firstname", "Lorena", "Marianne", "Lotte")
        .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"));
    var table = Matrix.builder()
        .data(data)
        .types(c(int.class, String.class, LocalDate.class))
        .build();
    assertEquals(Integer.class, table.type(0), "place column type");
    var selection = table.selectRowIndices(new RowCriteriaClosure(it -> {
      var date = it.getAt(2, LocalDate.class);
      return date.isAfter(LocalDate.of(2022, 1, 1));
    }
    ));
    assertIterableEquals(c(1, 2), selection);

    var foo = table.clone().apply("place", selection,
        new ValueClosure<Integer, Integer>(it -> it * 2));
    //println(foo.content())
    assertEquals(4, foo.getAt(1, 0, Integer.class));
    assertEquals(6, foo.getAt(2, 0, Integer.class));
    assertEquals(LocalDate.class, foo.type(2));
    assertEquals(Integer.class, foo.type(0), "place column type");

    var bar = table.clone().apply("place", new RowCriteriaClosure(it -> {
      var date = it.getAt(2, LocalDate.class);
      return date.isAfter(LocalDate.of(2022, 1, 1));
    }), new ValueClosure<Integer, Integer>(it -> it * 2));
    //println(bar.content())
    assertEquals(4, bar.getAt(1, 0, Integer.class));
    assertEquals(6, bar.getAt(2, 0, Integer.class));
    assertEquals(LocalDate.class, bar.type(2), "start column type");
    assertEquals(Integer.class, bar.type(0), "place column type");

    var r = table.rows(new RowCriteriaClosure(row -> row.getAt("place", Integer.class) == 2));
    assertEquals(c(2, "Marianne", LocalDate.parse("2022-07-10")), r.get(0));

    // An item in a Row can also be referenced by the column name
    Row r2 = table.rows().stream().filter(row -> row.getAt("place", Integer.class) == 3).findFirst().orElse(null);
    assertIterableEquals(c(3, "Lotte", LocalDate.parse("2023-05-27")), r2, String.valueOf(r2));
  }


  @Test
  void testAddColumn() {
    var empData = Matrix.builder().columns(new Columns(
            m("emp_id", r(1, 5)),
            m("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary"),
            m("salary", 623.3, 515.2, 611.0, 729.0, 843.25),
            m("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")))
        )
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();
    var table = empData.clone().addColumn("yearMonth", YearMonth.class, toYearMonths(empData.column("start_date")));
    assertEquals(5, table.columnCount());
    assertEquals("yearMonth", table.columnNames().get(table.columnCount() - 1));
    assertEquals(YearMonth.class, table.type("yearMonth"));
    assertEquals(YearMonth.of(2012, 1), table.getAt(0, 4));
    assertEquals(YearMonth.of(2015, 3), table.getAt(4, 4));

    // Append a new column to the end
    Matrix table2 = empData.clone();
    table2.putAt("yearMonth", YearMonth.class, toYearMonths(table2.getAt("start_date")));
    assertEquals(empData.columnCount() + 1, table2.columnCount());
    assertEquals("yearMonth", table2.columnNames().get(table2.columnCount() - 1));
    assertEquals(YearMonth.class, table2.type("yearMonth"));
    assertEquals(YearMonth.of(2012, 1), table2.getAt(0, 4));
    assertEquals(YearMonth.of(2015, 3), table2.getAt(4, 4));

    // Insert a new column first
    Matrix table3 = empData.clone();
    table3.putAt("yearMonth", YearMonth.class, 0, toYearMonths(table3.getAt("start_date")));
    assertEquals(empData.columnCount() + 1, table3.columnCount());
    assertEquals("yearMonth", table3.columnNames().get(0));
    assertEquals(YearMonth.class, table3.type("yearMonth"));
    assertEquals(YearMonth.of(2012, 1), table3.getAt(0, 0));
    assertEquals(YearMonth.of(2015, 3), table3.getAt(4, 0));
  }


  @Test
  void testAddColumns() {
    var empData = Matrix.builder()
        .columns(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2019-01-01", "2019-01-23", "2019-05-15", "2019-05-11", "2019-03-27")))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();

    empData = empData.addColumn("yearMonth", YearMonth.class, toYearMonths(empData.getAt("start_date")));
    assertEquals(YearMonth.class, empData.getAt(0, 4).getClass(), "type of the added column");
    assertEquals(YearMonth.class, empData.type("yearMonth"), "claimed type of the added column");

    var counts = Stat.countBy(empData, "yearMonth").orderBy("yearMonth");
    assertEquals(YearMonth.class, counts.getAt(0, 0).getClass(), "type of the count column");
    assertEquals(YearMonth.class, counts.type("yearMonth"), "claimed type of the count column");

    assertEquals(2, counts.subset("yearMonth", new ObjectCriteriaClosure(it -> it.equals(YearMonth.of(2019, 5)))).getAt(0, 1, Integer.class));
    assertEquals(1, counts.subset("yearMonth", new ObjectCriteriaClosure(it -> it.equals(YearMonth.of(2019, 3)))).getAt("yearMonth_count").get(0));
    assertEquals(2, counts.getAt(0, "yearMonth_count", Integer.class));

    var sums = Stat.sumBy(empData, "salary", "yearMonth").orderBy("yearMonth", true);
    assertEquals(YearMonth.class, sums.getAt(0, 0).getClass(), "type of the sums column");
    assertEquals(YearMonth.class, sums.type("yearMonth"), "claimed type of the sums column");
    assertEquals(611.0 + 729.0, sums.getAt(0, 1, Double.class), sums.content());
    assertEquals(843.25, sums.getAt(1, 1, Double.class), sums.content());
    assertEquals(623.3 + 515.2, sums.getAt(2, 1, Double.class), sums.content());

    var salaryPerYearMonth = counts
        .orderBy("yearMonth", true)
        .addColumns(sums, "salary");
    // In groovy, the default decimal type is BigDecimal, In java it is Double, so we have to cast to get comparable things
    assertEquals(asYearMonth("2019-05"), salaryPerYearMonth.getAt(0, 0), salaryPerYearMonth.content());
    assertEquals(611.0 + 729.0, salaryPerYearMonth.getAt(0, 2, Double.class), salaryPerYearMonth.content());
    assertEquals(2, salaryPerYearMonth.getAt(0, 1, Integer.class), salaryPerYearMonth.content());
    assertEquals(843.25, salaryPerYearMonth.getAt(1, 2, Double.class), salaryPerYearMonth.content());
    assertEquals(1, salaryPerYearMonth.getAt(1, 1, Integer.class), salaryPerYearMonth.content());
  }

  @Test
  void testSort() {
    var empData = Matrix.builder().columns(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")))
        .types(Integer.class, String.class, Number.class, LocalDate.class)
        .build();
    var dateSorted = empData.orderBy("start_date");
    assertEquals(4, dateSorted.getAt(4, 0, Integer.class), "Last row should be the Ryan row: \n${dateSorted.content()}");
    assertEquals(asLocalDate("2012-03-27"), dateSorted.getAt(0, 3), "First row should be the Dan Row");

    //println(empData.content())
    var salarySorted = empData.orderBy(lhm("salary", Matrix.DESC));
    assertEquals(843.25, salarySorted.getAt("salary").get(0), "Highest salary: ${salarySorted.content()}");
    assertEquals(515.2, salarySorted.getAt("salary").get(4), "Lowest salary: ${salarySorted.content()}");
  }


  @Test
  void testDropColumns() {
    var empData = Matrix.builder().columns(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")))
        .types(Integer.class, String.class, Number.class, LocalDate.class)
        .build();
    var empList = empData.dropColumns("salary", "start_date");
    //println(empList.content())
    assertEquals(2, empList.columnCount(), "Number of columns after drop");
    assertEquals(5, empList.rowCount(), "Number of rows after drop");
    assertIterableEquals(c("emp_id", "emp_name"), empList.columnNames(), "column names after drop");
    assertIterableEquals(c(Integer.class, String.class), empList.types(), "Column types after drop");
  }


  @Test
  void testDropColumnsExcept() {
    var empData = Matrix.builder().columns(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")))
        .types(Integer.class, String.class, Number.class, LocalDate.class)
        .build();
    var empList = empData.dropColumnsExcept("emp_id", "start_date");
    //println(empList.content())
    assertEquals(2, empList.columnCount(), "Number of columns after drop");
    assertEquals(5, empList.rowCount(), "Number of rows after drop");
    assertIterableEquals(c("emp_id", "start_date"), empList.columnNames(), "column names after drop");
    assertIterableEquals(c(Integer.class, LocalDate.class), empList.types(), "Column types after drop");
  }


  @Test
  void testIteration() {
    var empData = Matrix.builder().columns(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")))
        .types(Integer.class, String.class, Number.class, LocalDate.class)
        .build();

    AtomicInteger i = new AtomicInteger(1);
    empData.forEach(row -> {
      assertEquals(i.get(), row.get(0), String.valueOf(row));
      assertEquals(empData.getAt(i.get() - 1, "emp_name"), row.get(1), String.valueOf(row));
      i.incrementAndGet();
    });

    for (Row row : empData.rows()) {
      if (row.getAt(2, Double.class) > 600) {
        Double value = row.getAt(2, double.class) - 600;
        row.putAt(2, value);
      }
    }

    assertEquals(23.3, empData.getAt(0, 2), 0.00001, empData.toMarkdown());
    assertEquals(515.2, empData.getAt(1, 2), 0.00001, empData.toMarkdown());
    assertEquals(11.0, empData.getAt(2, 2), 0.00001, empData.toMarkdown());
    assertEquals(129.0, empData.getAt(3, 2), 0.00001, empData.toMarkdown());
    assertEquals(243.25, empData.getAt(4, 2), 0.00001, empData.toMarkdown());
  }


  @Test
  void testMatrixToGrid() {
    // BigDecimals are the default in Groovy, in java we need to convert upon creation
    var report = new Columns(
        m("Full Funding", toBigDecimals(4563.153, 380.263, 4.938, 12.23)),
        m("Baseline Funding", toBigDecimals(3385.593, 282.133, 3.664, 2.654)),
        m("Current Funding", toBigDecimals(2700, 225, 2.922, 1.871))
    );
    Matrix table = Matrix.builder().data(report).types(cr(BigDecimal.class, 3)).build();

    Grid<Object> grid = table.grid();
    assertEquals(new BigDecimal("3.664"), grid.getAt(2, 1));
    Grid<BigDecimal> typedGrid = table.grid(BigDecimal.class);
    assertEquals(new BigDecimal("380.263"), typedGrid.getAt(1, 0));

    var report2 = new Columns(
        m("Full Funding", toDoubles(4563.153, 380.263, 4.938, 12.23)),
        m("Baseline Funding", 3385.593, 282.133, 3.664, 2.654),
        m("Current Funding", 2700, 225, 2.922, 1.871)
    );
    table = Matrix.builder().data(report2).types(cr(Double.class, 3)).build();
    Grid<BigDecimal> tg2 = table.grid(BigDecimal.class, true);
    assertEquals(new BigDecimal("3.664"), tg2.getAt(2, 1));
    assertEquals(new BigDecimal("380.263"), tg2.getAt(1, 0));
  }


  @Test
  void testSelectColumns() {
    var report = new Columns(
        m("Full Funding", toBigDecimals(4563.153, 380.263, 4.938, 12.23)),
        m("Baseline Funding", toBigDecimals(3385.593, 282.133, 3.664, 2.654)),
        m("Current Funding", toBigDecimals(2700, 225, 2.922, 1.871))
    );
    Matrix table = Matrix.builder().data(report).types(cr(BigDecimal.class, 3)).build()
        .selectColumns("Baseline Funding", "Full Funding");

    assertEquals(asBigDecimal(3385.593), table.getAt(0, 0));
    assertEquals(asBigDecimal(12.23), table.getAt(3, 1));
  }


  @Test
  void testRenameColumns() {
    var empData = Matrix.builder()
        .data(new Columns()
            .add("emp_id", r(1, 5))
            .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
            .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
            .add("start_date", toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")))
        .types(Integer.class, String.class, Number.class, LocalDate.class)
        .build();

    empData.renameColumn("emp_id", "id");
    empData.renameColumn(1, "name");

    assertEquals("id", empData.columnNames().get(0));
    assertEquals("name", empData.columnNames().get(1));
  }


  @Test
  void testToMarkdown() {
    var report = new Columns(
        m("YearMonth", toYearMonths("2023-01", "2023-02", "2023-03", "2023-04")),
        m("Full Funding", 4563.153, 380.263, 4.938, 12.23),
        m("Baseline Funding", 3385.593, 282.133, 3.664, 2.654),
        m("Current Funding", 2700, 225, 2.922, 1.871)
    );
    Matrix table = Matrix.builder()
        .data(report)
        .types(YearMonth.class, BigDecimal.class, BigDecimal.class, BigDecimal.class)
        .build();

    var md = table.toMarkdown();
    var rows = md.split("\n");
    assertEquals(6, rows.length);
    assertEquals("| YearMonth | Full Funding | Baseline Funding | Current Funding |", rows[0]);
    assertEquals("| --- | ---: | ---: | ---: |", rows[1]);
    assertEquals("| 2023-01 | 4563.153 | 3385.593 | 2700 |", rows[2]);
    assertEquals("| 2023-04 | 12.23 | 2.654 | 1.871 |", rows[5]);

    md = table.toMarkdown(Map.of("class", "table"));
    rows = md.split("\n");
    assertEquals(7, rows.length);
    assertEquals("{class=\"table\" }", rows[6]);
  }


  @Test
  void testEquals() {
    var empData = Matrix.builder().columns(new Columns(
            m("emp_id", c(1, 2)),
            m("emp_name", "Rick", "Dan"),
            m("salary", 623.3, 515.2),
            m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();

    assertEquals(empData, Matrix.builder().columns(new Columns(
            m("emp_id", c(1, 2)),
            m("emp_name", "Rick", "Dan"),
            m("salary", 623.3, 515.2),
            m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build());

    assertNotEquals(empData, Matrix.builder().columns(new Columns(
            m("emp_id", c(1, 2)),
            m("emp_name", "Rick", "Dan"),
            m("salary", 623.3, 515.1),
            m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build()
    );

    Matrix differentTypes = Matrix.builder().columns(
            new Columns(
                m("emp_id", c(1, 2)),
                m("emp_name", "Rick", "Dan"),
                m("salary", 623.3, 515.2),
                m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(cr(Object.class, 4))
        .build();
    assertEquals(empData, differentTypes, empData.diff(differentTypes));
    assertNotEquals(empData, differentTypes.withMatrixName("differentTypes"), empData.diff(differentTypes));
  }


  @Test
  void testDiff() {
    var empData = Matrix.builder().columns(
            new Columns(
                m("emp_id", c(1, 2)),
                m("emp_name", "Rick", "Dan"),
                m("salary", 623.3, 515.2),
                m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(c(int.class, String.class, Number.class, LocalDate.class))
        .build();
    var d1 = Matrix.builder().columns(
            new Columns(
                m("emp_id", c(1, 2)),
                m("emp_name", "Rick", "Dan"),
                m("salary", 623.3, 515.1),
                m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();
    assertEquals("Row 1 differs: \n\tthis: 2, Dan, 515.2, 2012-03-27 \n\tthat: 2, Dan, 515.1, 2012-03-27",
        empData.diff(d1).trim());

    var d2 = Matrix.builder().data(
            new Columns(
                m("emp_id", c(1, 2)),
                m("emp_name", "Rick", "Dan"),
                m("salary", 623.3, 515.2),
                m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(cr(Object.class, 4))
        .build();
    assertEquals("Column types differ: \n\tthis: Integer, String, Number, LocalDate \n\tthat: Object, Object, Object, Object",
        empData.diff(d2));
  }


  @Test
  void testRemoveEmptyRows() {
    var empData = Matrix.builder().matrixName("empData").columns(new Columns(
            m("emp_id", c(1, 2)),
            m("emp_name", "Rick", "Dan"),
            m("salary", 623.3, 515.2),
            m("start_date", toLocalDates("2013-01-01", "2012-03-27"))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();

    var d0 = Matrix.builder().matrixName("empData").columns(new Columns(
            m("emp_id", 1, null, 2, null),
            m("emp_name", "Rick", "", "Dan", " "),
            m("salary", 623.3, null, 515.2, null),
            m("start_date", toLocalDates("2013-01-01", null, "2012-03-27", null))))
        .types(int.class, String.class, Number.class, LocalDate.class)
        .build();
    var d0r = d0.removeEmptyRows();
    System.out.println(d0r.content());
    assertEquals(empData, d0r, empData.diff(d0r, true));
  }


  @Test
  void testRemoveEmptyColumns() {
    var empData = Matrix.builder().data(new Columns()
            .add("emp_id", 1, 2)
            .add("emp_name", null, null)
            .add("salary", 623.3, 515.2)
            .add("start_date", null, null)
            .add("other", null, null))
        .types(int.class, String.class, Number.class, LocalDate.class, String.class)
        .build();
    assertIterableEquals(c("emp_id", "emp_name", "salary", "start_date", "other"), empData.columnNames());
    empData.removeEmptyColumns();
    assertEquals(2, empData.columnCount());
    assertIterableEquals(c("emp_id", "salary"), empData.columnNames());
  }

  boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }


  @Test
  void testWithColumns() {
    var table = Matrix.builder().data(new Columns()
            .add("a", toIntegers(1, 2, 3, 4, 5))
            .add("b", toBigDecimals(1.2, 2.3, 0.7, 1.3, 1.9))
        ).types(Integer.class, BigDecimal.class)
        .build();

    var m = table.withColumns(c("a", "b"), new ValueTwoArgClosure<BigDecimal, Integer, BigDecimal>(
        (x, y) -> new BigDecimal(x).subtract(y))
    );

    // we can always create a Closure directly of course, but that is less elegant in java
    var m2 = table.withColumns(c("a", "b"), new Closure<BigDecimal>(null) {
      public BigDecimal doCall(Integer x, BigDecimal y) {
        return new BigDecimal(x).subtract(y);
      }
    });
    assertIterableEquals(m, m2);
    assertIterableEquals(toBigDecimals(-0.2, -0.3, 2.3, 2.7, 3.1), m);

    var n = table.withColumns(
        toIntegers(0, 1)
            .toArray(new Integer[]{}),
        new ValueTwoArgClosure<BigDecimal, Integer, BigDecimal>((x, y) -> new BigDecimal(x).subtract(y))
    );
    assertIterableEquals(toBigDecimals(-0.2, -0.3, 2.3, 2.7, 3.1), n);
  }


  @Test
  void testPopulateColumn() {
    Matrix components = Matrix.builder().data(new Columns()
            .add("id", 1, 2, 3, 4, 5)
            .add("size", 1.2, 2.3, 0.7, 1.3, 1.9))
        .types(Integer.class, Double.class)
        .build();
    components.putAt("id", c(10, 11, 12, 13, 14));
    assertEquals(10, components.getAt(0, "id", Integer.class));
    assertEquals(13, components.getAt(3, "id", Integer.class));
    assertEquals(14, components.getAt(4, "id", Integer.class));
  }


  @Test
  void testMoveColumn() {
    var table = Matrix.builder().data(new Columns()
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
            .add("foo", 1, 2, 3)
        )
        .types(String.class, LocalDate.class, int.class)
        .build();

    table.moveColumn("foo", 0);
    assertIterableEquals(c("foo", "firstname", "start"), table.columnNames());
    assertIterableEquals(c(1, 2, 3), table.getAt(0));
    assertIterableEquals(c(Integer.class, String.class, LocalDate.class), table.types());
  }


  @Test
  void testPutAt() {
    // putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
    var table = Matrix.builder().data(new Columns()
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
            .add("foo", 1, 2, 3)
        )
        .types(c(String.class, LocalDate.class, int.class))
        .build();
    table.putAt("yearMonth", YearMonth.class, 0, toYearMonths(table.getAt("start")));
    assertEquals(4, table.columnCount());
    assertIterableEquals(c("yearMonth", "firstname", "start", "foo"), table.columnNames());
    assertIterableEquals(toYearMonths("2021-12", "2022-07", "2023-05"), table.getAt(0));

    // putAt(List where, List<?> column)
    table = Matrix.builder().data(new Columns()
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
            .add("foo", 1, 2, 3)
        )
        .types(c(String.class, LocalDate.class, int.class))
        .build();
    table.putAt(
        "start",
        table.getAt("start", LocalDate.class)
            .stream().map(it -> it.plusDays(10))
            .collect(Collectors.toList())
    );
    assertEquals(3, table.columnCount());
    assertIterableEquals(c("firstname", "start", "foo"), table.columnNames());
    // getAt and putAt should have the same semantics i refer to columns:
    assertIterableEquals(toLocalDates(c("2021-12-11", "2022-07-20", "2023-06-06")), table.getAt(1));
    assertIterableEquals(table.column(2), table.getAt(2));
    assertIterableEquals(table.column("foo"), table.getAt("foo"));
  }


  @Test
  void testGetAt() {
    var table = Matrix.builder().data(new Columns()
            .add("firstname", "Lorena", "Marianne", "Lotte")
            .add("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
            .add("foo", 1, 2, 3)
        )
        .types(String.class, LocalDate.class, int.class)
        .build();

    assertEquals(Integer.class, table.getAt(2, 2).getClass());
    assertEquals(3, table.getAt(2, 2, Integer.class));

    assertEquals(Integer.class, table.getAt(2, 2).getClass());
    assertEquals(3, table.getAt(2, 2, Integer.class));

    assertEquals(LocalDate.class, table.getAt(2, "start").getClass());
    assertEquals(asLocalDate("2023-05-27"), table.getAt(2, "start"));

    assertEquals(asLocalDate("2023-05-27"), table.getAt(2, "start"));
    assertEquals(LocalDate.class, table.getAt(2, "start").getClass());


    Row row = table.row(1);
    assertEquals(LocalDate.class, row.getAt("start").getClass());
    assertEquals(LocalDate.class, row.getAt(1).getClass());
    assertEquals(LocalDate.class, row.getAt("start").getClass());
    assertEquals(LocalDate.parse("2022-07-10"), row.getAt(1));
    assertEquals(LocalDate.parse("2022-07-10"), row.getAt("start"));

    assertEquals(String.class, table.getAt(0, 1, String.class).getClass());
    assertEquals("2021-12-01", table.getAt(0, 1, String.class));

    assertEquals(String.class, table.getAt(0, "foo", String.class).getClass());
    assertEquals("3", table.getAt(2, "foo", String.class));

    assertEquals(asBigDecimal(2), row.getAt("foo", BigDecimal.class));
    assertEquals(asBigDecimal(2), row.getAt("foo", BigDecimal.class));
    assertEquals("2", row.getAt("foo", String.class));
    assertEquals(asBigDecimal(2), row.getAt(2, BigDecimal.class));
    assertEquals("2", row.getAt(2, String.class));
    assertEquals("2", row.getAt(2, String.class));
  }
}
