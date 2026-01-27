package tech.tablesaw.column.numbers;

import org.junit.jupiter.api.Test;
import tech.tablesaw.io.xml.XmlReadOptions;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BigDecimalParserTest {

  @Test
  public void testMissingIndicatorsAreTreatedAsMissing() {
    var options = XmlReadOptions.builderFromString("<doc/>")
        .missingValueIndicator("NA", "")
        .build();
    var parser = new BigDecimalParser(BigDecimalColumnType.instance(), options);

    assertTrue(parser.canParse(null));
    assertNull(parser.parse(null));

    assertTrue(parser.canParse(""));
    assertNull(parser.parse(""));

    assertTrue(parser.canParse("NA"));
    assertNull(parser.parse("NA"));

    assertEquals(new BigDecimal("1.5"), parser.parse("1.5"));
  }
}
