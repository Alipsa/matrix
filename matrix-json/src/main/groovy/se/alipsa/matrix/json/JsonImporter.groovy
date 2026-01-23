package se.alipsa.matrix.json

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Imports JSON arrays into Matrix format using Jackson streaming API.
 *
 * @deprecated Use {@link JsonReader} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code JsonImporter.parse(string)} → {@code JsonReader.read(string)}</li>
 *   <li>{@code JsonImporter.parse(file)} → {@code JsonReader.read(file)}</li>
 *   <li>{@code JsonImporter.parseFromFile(path)} → {@code JsonReader.readFile(path)}</li>
 *   <li>{@code JsonImporter.parseFromUrl(url)} → {@code JsonReader.readUrl(url)}</li>
 * </ul>
 *
 * @see JsonReader
 * @see JsonExporter
 */
@Deprecated
@CompileStatic
class JsonImporter {

  private JsonImporter() {
    // Only static methods
  }

  /**
   * Parse a JSON string containing an array of objects into a Matrix.
   *
   * @param str a JSON string containing a list of rows (JSON objects)
   * @return a Matrix with columns derived from JSON keys
   * @deprecated Use {@link JsonReader#read(String)} instead
   */
  @Deprecated
  static Matrix parse(String str) {
    JsonReader.read(str)
  }

  /**
   * Parse JSON from an InputStream into a Matrix.
   *
   * @param is the input stream containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @deprecated Use {@link JsonReader#read(InputStream, Charset)} instead
   */
  @Deprecated
  static Matrix parse(InputStream is, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.read(is, charset)
  }

  /**
   * Parse JSON from a File into a Matrix.
   *
   * @param file the file containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @deprecated Use {@link JsonReader#read(File, Charset)} instead
   */
  @Deprecated
  static Matrix parse(File file, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.read(file, charset)
  }

  /**
   * Parse JSON from a Reader into a Matrix.
   *
   * @param reader the reader providing JSON content
   * @return a Matrix with columns derived from JSON keys
   * @deprecated Use {@link JsonReader#read(Reader)} instead
   */
  @Deprecated
  static Matrix parse(Reader reader) {
    JsonReader.read(reader)
  }

  /**
   * Parse JSON from a URL into a Matrix.
   *
   * @param url URL pointing to JSON content
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the URL fails
   * @deprecated Use {@link JsonReader#read(URL, Charset)} instead
   */
  @Deprecated
  static Matrix parse(URL url, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.read(url, charset)
  }

  /**
   * Parse JSON from a URL string into a Matrix.
   *
   * @param urlString String URL pointing to JSON content
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the URL fails or URL is invalid
   * @deprecated Use {@link JsonReader#readUrl(String, Charset)} instead
   */
  @Deprecated
  static Matrix parseFromUrl(String urlString, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.readUrl(urlString, charset)
  }

  /**
   * Parse JSON from a Path into a Matrix.
   *
   * @param path Path to the file containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the file fails
   * @deprecated Use {@link JsonReader#read(Path, Charset)} instead
   */
  @Deprecated
  static Matrix parse(Path path, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.read(path, charset)
  }

  /**
   * Parse JSON from a file path string (convenience method).
   *
   * @param filePath path to the JSON file as a String
   * @param charset character encoding (default UTF-8)
   * @return Matrix containing the parsed data
   * @throws IOException if reading the file fails or file not found
   * @deprecated Use {@link JsonReader#readFile(String, Charset)} instead
   */
  @Deprecated
  static Matrix parseFromFile(String filePath, Charset charset = StandardCharsets.UTF_8) {
    JsonReader.readFile(filePath, charset)
  }
}
