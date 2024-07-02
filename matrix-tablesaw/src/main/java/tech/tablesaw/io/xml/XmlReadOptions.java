package tech.tablesaw.io.xml;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.io.ReadOptions;
import tech.tablesaw.io.Source;

import java.io.*;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class XmlReadOptions extends ReadOptions {

  public static Builder builder(File file) {
    return new Builder(file).tableName(file.getName());
  }

  public static Builder	builder(InputStream stream) {
    return new Builder(stream);
  }

  public static Builder	builder(Reader reader) {
    return new Builder(reader);
  }

  public static Builder	builder(String fileName) {
    return new Builder(new File(fileName));
  }

  public static Builder	builder(URL url) throws IOException {
    return new Builder(url);
  }

  public static Builder	builder(Source source) {
    return new Builder(source);
  }

  public static Builder	builderFromString(String contents) {
    return new Builder(new StringReader(contents));
  }

  public static Builder builderFromUrl(String url) throws IOException {
    return new Builder(new URL(url));
  }


  protected XmlReadOptions(ReadOptions.Builder builder) {
    super(builder);
  }

  public static class Builder extends ReadOptions.Builder {

    protected Builder(Source source) {
      super(source);
    }

    protected Builder(File file) {
      super(file);
    }

    protected Builder(InputStream stream) {
      super(stream);
    }

    protected Builder(Reader reader) {
      super(reader);
    }

    protected Builder(URL url) throws IOException {
      super(url);
    }

    @Override
    public Builder header(boolean header) {
      super.header(header);
      return this;
    }

    @Override
    public Builder tableName(String tableName) {
      super.tableName(tableName);
      return this;
    }

    @Override
    public Builder sample(boolean sample) {
      super.sample(sample);
      return this;
    }

    @Override
    public Builder dateFormat(DateTimeFormatter dateFormat) {
      super.dateFormat(dateFormat);
      return this;
    }

    @Override
    public Builder timeFormat(DateTimeFormatter timeFormat) {
      super.timeFormat(timeFormat);
      return this;
    }

    @Override
    public Builder dateTimeFormat(DateTimeFormatter dateTimeFormat) {
      super.dateTimeFormat(dateTimeFormat);
      return this;
    }

    @Override
    public Builder locale(Locale locale) {
      super.locale(locale);
      return this;
    }

    @Override
    public Builder missingValueIndicator(String... missingValueIndicators) {
      super.missingValueIndicator(missingValueIndicators);
      return this;
    }

    @Override
    public Builder minimizeColumnSizes() {
      super.minimizeColumnSizes();
      return this;
    }

    @Override
    public Builder columnTypes(ColumnType[] columnTypes) {
      super.columnTypes(columnTypes);
      return this;
    }

    @Override
    public Builder columnTypes(Function<String, ColumnType> columnTypeFunction) {
      super.columnTypes(columnTypeFunction);
      return this;
    }

    @Override
    public Builder columnTypesPartial(Function<String, Optional<ColumnType>> columnTypeFunction) {
      super.columnTypesPartial(columnTypeFunction);
      return this;
    }

    @Override
    public Builder columnTypesPartial(Map<String, ColumnType> columnTypeByName) {
      super.columnTypesPartial(columnTypeByName);
      return this;
    }

    @Override
    public XmlReadOptions build() {
      return new XmlReadOptions(this);
    }
  }
}
