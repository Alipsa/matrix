import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import groovy.json.JsonBuilder
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrixjson.*

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.groovy.matrix.ListConverter.toLocalDates

class JsonImporterTest {

  @Test
  void testJsonImport() {
    def importer = new JsonImporter()
    def table = importer.parse('''[
        {
          "emp_id": 1,
          "emp_name": "Rick",
          "salary": 623.3,
          "start_date": "2012-01-01"
        },
        {
          "emp_id": 2,
          "emp_name": "Dan",
          "salary": 515.2,
          "start_date": "2013-09-23"
        },
        {
          "emp_id": 3,
          "emp_name": "Michelle",
          "salary": 611.0,
          "start_date": "2014-11-15"
        }
    ]''').convert([int, String, Number, LocalDate])

    Matrix empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick","Dan","Michelle"],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
      .types(int, String, Number, LocalDate)
      .build()
    assertEquals(empData, table, empData.diff(table))
  }

  @Test
  void testNested() {
    ObjectMapper mapper = new ObjectMapper()

    Family f1 = new Family("Nyfelt-Wersäll")
    f1.members.add(new Person('Per'))
    f1.members.add(new Person('Louise'))

    Family f2 = new Family('Nyfelt-Anselius')
    f2.members.add(new Person('Jonas'))
    f2.members.add(new Person('Kristin'))
    f2.members.add(new Person('Frida'))
    f2.members.add(new Person('Oliver'))

    Clan c1 = new Clan('Nyfelt')
    c1.clanMembers.add(f1)
    c1.clanMembers.add(f2)

    String json = mapper.writeValueAsString([f1, f2])
    println json
    JsonNode root = mapper.readTree(json)
    JsonNode flatJson = null
    JsonNodeFactory factory = JsonNodeFactory.instance

    List<Map<String, String>> flatMaps = []
    root.each {
      Map<String, String> flatMap = [:]
      addKeys('', it, flatMap)
      flatMaps << flatMap
    }
    // Find the max of each key and fill the rest with null
    Set<String> keySet = new LinkedHashSet<>()
    flatMaps.each {
      keySet.addAll(it.keySet())
    }
    for (int i = 0; i < flatMaps.size(); i++) {
      def m = flatMaps.get(0)
      keySet.each {
        if (!m.containsKey(it)) {
          m.put(it, null)
        }
      }
    }
    flatJson = new ArrayNode(factory)
    flatMaps.each {
      def rowJson = new JsonBuilder(it).toString()
      //println ("adding $rowJson")
      flatJson.add(mapper.readTree(rowJson))
    }

    def flatJsonString = mapper.writeValueAsString(flatJson)
    println (flatJsonString)
    def importer = new JsonImporter()
    def table = importer.parse(flatJsonString)
    println table.content()
  }

  class Person {
    Person(String name) {
      this.name = name
    }
    String name
  }

  class Family {
    Family(String name) {
      this.name = name
    }
    String name
    List<Person> members = []
  }

  class Clan {
    Clan(String name) {
      this.clanName = name
    }
    String clanName
    List<Family> clanMembers = []
  }

  def addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
    if (jsonNode.isObject()) {
      ObjectNode objectNode = (ObjectNode) jsonNode
      Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields()
      def pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

      while (iter.hasNext()) {
        Map.Entry<String, JsonNode> entry = iter.next();
        addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
      }
    } else if (jsonNode.isArray()) {
      ArrayNode arrayNode = (ArrayNode) jsonNode;
      for (int i = 0; i < arrayNode.size(); i++) {
        addKeys(currentPath + '[' + i + ']', arrayNode.get(i), map);
      }
    } else if (jsonNode.isValueNode()) {
      ValueNode valueNode = (ValueNode) jsonNode;
      map.put(currentPath, valueNode.asText());
    }
  }
}
