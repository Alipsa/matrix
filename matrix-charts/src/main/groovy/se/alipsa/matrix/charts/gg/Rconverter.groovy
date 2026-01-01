package se.alipsa.matrix.charts.gg

import groovy.transform.CompileStatic

/**
 * Convert a subset of ggplot2 R expressions into the Groovy gg DSL.
 * The conversion is best-effort and focuses on common ggplot2 patterns.
 */
@CompileStatic
class Rconverter {

  private static final Map<String, String> FUNCTION_ALIASES = [
      'ggplot2::ggplot': 'ggplot',
      'ggplot2::aes': 'aes'
  ]

  private static final Map<String, String> MATH_FUNCTIONS = [
      'log': 'Math.log',
      'log10': 'Math.log10',
      'sqrt': 'Math.sqrt',
      'exp': 'Math.exp',
      'abs': 'Math.abs',
      'sin': 'Math.sin',
      'cos': 'Math.cos',
      'tan': 'Math.tan',
      'asin': 'Math.asin',
      'acos': 'Math.acos',
      'atan': 'Math.atan',
      'floor': 'Math.floor',
      'ceiling': 'Math.ceil',
      'round': 'Math.round'
  ]

  static String convert(String expression) {
    if (expression == null) {
      return null
    }
    String cleaned = stripComments(expression).trim()
    if (cleaned.isEmpty()) {
      return ''
    }

    List<String> statements = splitTopLevelStatements(cleaned)
    if (statements.size() > 1) {
      List<String> convertedStatements = []
      for (String statement : statements) {
        String trimmed = statement.trim()
        if (!trimmed.isEmpty()) {
          convertedStatements.add(convertSingle(trimmed))
        }
      }
      return convertedStatements.join('\n')
    }

    return convertSingle(cleaned)
  }

  private static String convertSingle(String input) {
    int assignIdx = findTopLevelToken(input, '<-')
    if (assignIdx >= 0) {
      String left = input.substring(0, assignIdx).trim()
      String right = input.substring(assignIdx + 2).trim()
      if (right.startsWith('function')) {
        return convertFunctionDefinition(left, right)
      }
      return "${left} = ${convertSingle(right)}"
    }

    List<String> parts = splitTopLevel(input, '+' as char)
    List<String> converted = []
    for (String part : parts) {
      String trimmed = part.trim()
      if (!trimmed.isEmpty()) {
        converted.add(convertComponent(trimmed))
      }
    }
    return converted.join(' + ')
  }

  private static String convertFunctionDefinition(String name, String rhs) {
    int parenStart = rhs.indexOf('(')
    if (parenStart < 0) {
      return "${name} = ${convertSingle(rhs)}"
    }
    int parenEnd = findMatchingParen(rhs, parenStart)
    if (parenEnd < 0) {
      return "${name} = ${convertSingle(rhs)}"
    }
    String params = rhs.substring(parenStart + 1, parenEnd).trim()
    int braceStart = findNextNonSpace(rhs, parenEnd + 1)
    if (braceStart < 0 || rhs.charAt(braceStart) != '{') {
      return "def ${name}(${params})"
    }
    int braceEnd = findMatchingBrace(rhs, braceStart)
    if (braceEnd < 0) {
      return "def ${name}(${params})"
    }
    String body = rhs.substring(braceStart + 1, braceEnd).trim()
    String convertedBody = convertFunctionBody(body)
    String paramsRendered = params.isEmpty() ? '' : params
    return "def ${name}(${paramsRendered}) {\n${indent(convertedBody, '  ')}\n}"
  }

  private static String convertFunctionBody(String body) {
    if (body == null || body.trim().isEmpty()) {
      return ''
    }
    String trimmed = body.trim()
    FunctionCall call = parseFunctionCall(trimmed)
    if (call != null && normalizeFunctionName(call.name) == 'list') {
      List<String> items = splitTopLevel(call.args ?: '', ',' as char)
      List<String> converted = []
      for (String item : items) {
        String itemTrimmed = item.trim()
        if (!itemTrimmed.isEmpty()) {
          converted.add(convertComponent(itemTrimmed))
        }
      }
      return formatList(converted)
    }
    return convert(trimmed)
  }

  private static String formatList(List<String> items) {
    if (items == null || items.isEmpty()) {
      return '[]'
    }
    String joined = items.collect { "  ${it}" }.join(',\n')
    return "[\n${joined}\n]"
  }

  private static String indent(String text, String prefix) {
    if (text == null || text.isEmpty()) {
      return ''
    }
    return text.readLines().collect { it.isEmpty() ? it : "${prefix}${it}" }.join('\n')
  }

  private static String convertComponent(String part) {
    FunctionCall call = parseFunctionCall(part)
    if (call == null) {
      return part
    }
    String functionName = normalizeFunctionName(call.name)
    if (call.args == null) {
      return "${functionName}()"
    }
    String args = call.args.trim()
    if (args.isEmpty()) {
      return "${functionName}()"
    }
    if (functionName == 'ggplot') {
      return convertGgplot(args)
    }
    if (functionName == 'aes') {
      return "aes(${convertAesArgs(args)})"
    }
    return "${functionName}(${convertGenericArgs(args, functionName)})"
  }

  private static String convertGgplot(String args) {
    List<Arg> parsed = parseArgs(args)
    Arg dataArg = parsed.find { it.name == 'data' }
    Arg mappingArg = parsed.find { it.name == 'mapping' }
    List<Arg> positional = parsed.findAll { !it.named }

    if (dataArg == null && !positional.isEmpty()) {
      dataArg = positional.remove(0)
    }
    if (mappingArg == null && !positional.isEmpty()) {
      mappingArg = positional.remove(0)
    }

    String data = dataArg != null ? convertRValue(dataArg.value, null, null) : 'null'
    String mapping = mappingArg != null ? convertComponent(mappingArg.value) : 'aes([:])'

    return "ggplot(${data}, ${mapping})"
  }

  private static String convertAesArgs(String args) {
    List<Arg> parsed = parseArgs(args)
    List<String> converted = []
    for (Arg arg : parsed) {
      if (arg.named) {
        String key = normalizeKey(arg.name)
        String value = convertAesValue(arg.value)
        converted.add("${key}: ${value}".toString())
      } else {
        converted.add(convertAesPositionalValue(arg.value))
      }
    }
    return converted.join(', ')
  }

  private static String convertGenericArgs(String args, String functionName) {
    List<Arg> parsed = parseArgs(args)
    List<String> converted = []
    for (Arg arg : parsed) {
      if (arg.named) {
        String key = normalizeKey(arg.name)
        String value = convertRValue(arg.value, functionName, arg.name)
        converted.add("${key}: ${value}".toString())
      } else {
        converted.add(convertRValue(arg.value, functionName, null))
      }
    }
    return converted.join(', ')
  }

  private static String convertAesPositionalValue(String value) {
    String trimmed = value.trim()
    if (isBacktickName(trimmed)) {
      return quote(stripBackticks(trimmed))
    }
    if (isQuoted(trimmed)) {
      return "I(${trimmed})"
    }
    if (isNumeric(trimmed) || isBooleanLiteral(trimmed) || isNullLiteral(trimmed)) {
      return "I(${normalizeLiteral(trimmed)})"
    }
    if (isBareIdentifier(trimmed)) {
      return quote(trimmed)
    }
    return "expr { ${convertExpression(trimmed)} }"
  }

  private static String convertAesValue(String value) {
    String trimmed = value.trim()
    if (isAfterStatSymbol(trimmed)) {
      return "after_stat(${quote(trimmed.substring(2, trimmed.length() - 2))})"
    }
    FunctionCall call = parseFunctionCall(trimmed)
    if (call != null && normalizeFunctionName(call.name) == 'after_stat') {
      String arg = firstPositionalArg(call.args)
      String statName = arg != null ? stripQuotes(arg.trim()) : ''
      return "after_stat(${quote(statName)})"
    }
    if (isBacktickName(trimmed)) {
      return quote(stripBackticks(trimmed))
    }
    if (isQuoted(trimmed)) {
      return "I(${trimmed})"
    }
    if (isNumeric(trimmed) || isBooleanLiteral(trimmed) || isNullLiteral(trimmed)) {
      return "I(${normalizeLiteral(trimmed)})"
    }
    if (isBareIdentifier(trimmed)) {
      return quote(trimmed)
    }
    return "expr { ${convertExpression(trimmed)} }"
  }

  private static String convertRValue(String value, String functionName, String argName) {
    String trimmed = value.trim()
    if (functionName == 'aes') {
      return convertAesValue(trimmed)
    }
    if (argName == 'formula' && !isQuoted(trimmed)) {
      return quote(trimmed)
    }
    if (isBacktickName(trimmed)) {
      return quote(stripBackticks(trimmed))
    }
    if (isQuoted(trimmed)) {
      return trimmed
    }
    if (isBooleanLiteral(trimmed)) {
      return normalizeLiteral(trimmed)
    }
    if (isNullLiteral(trimmed)) {
      return 'null'
    }
    if (isNumeric(trimmed)) {
      return trimmed
    }
    if (isVectorLiteral(trimmed)) {
      return convertVectorLiteral(trimmed, functionName)
    }
    FunctionCall call = parseFunctionCall(trimmed)
    if (call != null) {
      return convertComponent(trimmed)
    }
    return trimmed
  }

  private static String convertVectorLiteral(String value, String functionName) {
    String inner = value.trim()
    inner = inner.substring(2, inner.length() - 1)
    List<String> items = splitTopLevel(inner, ',' as char)
    List<String> converted = []
    for (String item : items) {
      converted.add(convertRValue(item.trim(), functionName, null))
    }
    return "[${converted.join(', ')}]"
  }

  private static String convertExpression(String expr) {
    StringBuilder out = new StringBuilder()
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    int i = 0
    while (i < expr.length()) {
      char ch = expr.charAt(i)
      if (escaped) {
        out.append(ch)
        escaped = false
        i++
        continue
      }
      if (ch == '\\\\') {
        out.append(ch)
        escaped = true
        i++
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        out.append(ch)
        i++
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        out.append(ch)
        i++
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        out.append(ch)
        i++
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        out.append(ch)
        i++
        continue
      }
      if (ch == '(') {
        depth++
        out.append(ch)
        i++
        continue
      }
      if (ch == ')') {
        depth = Math.max(0, depth - 1)
        out.append(ch)
        i++
        continue
      }
      if (ch == '^') {
        out.append('**')
        i++
        continue
      }
      if (Character.isLetter(ch) || ch == '_' || ch == '.') {
        int start = i
        i++
        while (i < expr.length()) {
          char next = expr.charAt(i)
          if (Character.isLetterOrDigit(next) || next == '_' || next == '.') {
            i++
          } else {
            break
          }
        }
        String token = expr.substring(start, i)
        String normalized = normalizeLiteral(token)
        if (isBooleanLiteral(token) || isNullLiteral(token)) {
          out.append(normalized)
          continue
        }
        if (token == 'pi') {
          out.append('Math.PI')
          continue
        }
        int j = i
        while (j < expr.length() && Character.isWhitespace(expr.charAt(j))) {
          j++
        }
        boolean isFunction = j < expr.length() && expr.charAt(j) == '('
        if (isFunction) {
          String mapped = MATH_FUNCTIONS.get(token)
          out.append(mapped != null ? mapped : token)
          continue
        }
        if (token.contains('.')) {
          out.append("it.'").append(token).append("'")
        } else {
          out.append('it.').append(token)
        }
        continue
      }
      out.append(ch)
      i++
    }
    return out.toString()
  }

  private static String normalizeFunctionName(String name) {
    String trimmed = name.trim()
    String mapped = FUNCTION_ALIASES.get(trimmed)
    if (mapped != null) {
      return mapped
    }
    if (trimmed.contains('::')) {
      return trimmed.substring(trimmed.lastIndexOf('::') + 2)
    }
    return trimmed
  }

  private static String normalizeKey(String key) {
    String trimmed = key.trim()
    if (trimmed ==~ /[A-Za-z_]\w*/) {
      return trimmed
    }
    return quote(trimmed)
  }

  private static boolean isBareIdentifier(String value) {
    return value ==~ /[A-Za-z_][A-Za-z0-9_.]*/
  }

  private static boolean isQuoted(String value) {
    if (value.length() < 2) {
      return false
    }
    char first = value.charAt(0)
    char last = value.charAt(value.length() - 1)
    return (first == '\'' && last == '\'') || (first == '"' && last == '"')
  }

  private static boolean isBacktickName(String value) {
    return value.length() >= 2 && value.charAt(0) == '`' && value.charAt(value.length() - 1) == '`'
  }

  private static String stripBackticks(String value) {
    return value.substring(1, value.length() - 1)
  }

  private static boolean isNumeric(String value) {
    return value ==~ /-?\d+(\.\d+)?([eE][+-]?\d+)?/
  }

  private static boolean isBooleanLiteral(String value) {
    return value == 'TRUE' || value == 'FALSE' || value == 'true' || value == 'false'
  }

  private static boolean isNullLiteral(String value) {
    return value == 'NULL' || value == 'NA' || value == 'null'
  }

  private static String normalizeLiteral(String value) {
    if (value == 'TRUE') return 'true'
    if (value == 'FALSE') return 'false'
    if (value == 'NULL' || value == 'NA') return 'null'
    return value
  }

  private static boolean isVectorLiteral(String value) {
    return value.startsWith('c(') && value.endsWith(')')
  }

  private static boolean isAfterStatSymbol(String value) {
    return value.startsWith('..') && value.endsWith('..') && value.length() > 4
  }

  private static String quote(String value) {
    return "'${value.replace("'", "\\'")}'"
  }

  private static String stripQuotes(String value) {
    if (!isQuoted(value)) {
      return value
    }
    return value.substring(1, value.length() - 1)
  }

  private static String stripComments(String input) {
    StringBuilder out = new StringBuilder()
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        out.append(ch)
        escaped = false
        continue
      }
      if (ch == ('\\' as char)) {
        out.append(ch)
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        out.append(ch)
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        out.append(ch)
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        out.append(ch)
        continue
      }
      if (!inSingle && !inDouble && !inBacktick && ch == '#') {
        while (i < input.length() && input.charAt(i) != '\n') {
          i++
        }
        if (i < input.length()) {
          out.append('\n')
        }
        continue
      }
      out.append(ch)
    }
    return out.toString()
  }

  private static List<String> splitTopLevel(String input, char delimiter) {
    List<String> parts = []
    StringBuilder current = new StringBuilder()
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        current.append(ch)
        escaped = false
        continue
      }
      if (ch == '\\\\') {
        current.append(ch)
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        current.append(ch)
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        current.append(ch)
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        current.append(ch)
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        current.append(ch)
        continue
      }
      if (ch == '(' || ch == '[' || ch == '{') {
        depth++
        current.append(ch)
        continue
      }
      if (ch == ')' || ch == ']' || ch == '}') {
        depth = Math.max(0, depth - 1)
        current.append(ch)
        continue
      }
      if (ch == delimiter && depth == 0) {
        parts.add(current.toString())
        current.setLength(0)
        continue
      }
      current.append(ch)
    }
    parts.add(current.toString())
    return parts
  }

  private static List<String> splitTopLevelStatements(String input) {
    List<String> parts = []
    StringBuilder current = new StringBuilder()
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        current.append(ch)
        escaped = false
        continue
      }
      if (ch == ('\\' as char)) {
        current.append(ch)
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        current.append(ch)
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        current.append(ch)
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        current.append(ch)
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        current.append(ch)
        continue
      }
      if (ch == '(' || ch == '[' || ch == '{') {
        depth++
        current.append(ch)
        continue
      }
      if (ch == ')' || ch == ']' || ch == '}') {
        depth = Math.max(0, depth - 1)
        current.append(ch)
        continue
      }
      if (depth == 0 && (ch == '\n' || ch == ';')) {
        parts.add(current.toString())
        current.setLength(0)
        continue
      }
      current.append(ch)
    }
    parts.add(current.toString())
    return parts
  }

  private static int findTopLevelToken(String input, String token) {
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i <= input.length() - token.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        escaped = false
        continue
      }
      if (ch == ('\\' as char)) {
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        continue
      }
      if (ch == '(' || ch == '[' || ch == '{') {
        depth++
        continue
      }
      if (ch == ')' || ch == ']' || ch == '}') {
        depth = Math.max(0, depth - 1)
        continue
      }
      if (depth == 0 && input.startsWith(token, i)) {
        return i
      }
    }
    return -1
  }

  private static int findNextNonSpace(String input, int startIdx) {
    int idx = startIdx
    while (idx < input.length() && Character.isWhitespace(input.charAt(idx))) {
      idx++
    }
    return idx < input.length() ? idx : -1
  }

  private static int findMatchingParen(String input, int openIdx) {
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = openIdx; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        escaped = false
        continue
      }
      if (ch == '\\\\') {
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        continue
      }
      if (ch == '(') {
        depth++
      } else if (ch == ')') {
        depth--
        if (depth == 0) {
          return i
        }
      }
    }
    return -1
  }

  private static int findMatchingBrace(String input, int openIdx) {
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = openIdx; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        escaped = false
        continue
      }
      if (ch == '\\\\') {
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        continue
      }
      if (ch == '{') {
        depth++
      } else if (ch == '}') {
        depth--
        if (depth == 0) {
          return i
        }
      }
    }
    return -1
  }

  private static FunctionCall parseFunctionCall(String input) {
    int openIdx = -1
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        escaped = false
        continue
      }
      if (ch == '\\\\') {
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        continue
      }
      if (ch == '(') {
        openIdx = i
        depth = 1
        break
      }
    }
    if (openIdx < 0) {
      return null
    }
    int closeIdx = -1
    boolean innerSingle = false
    boolean innerDouble = false
    boolean innerBacktick = false
    boolean innerEscaped = false
    for (int i = openIdx + 1; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (innerEscaped) {
        innerEscaped = false
        continue
      }
      if (ch == ('\\' as char)) {
        innerEscaped = true
        continue
      }
      if (ch == '\'' && !innerDouble && !innerBacktick) {
        innerSingle = !innerSingle
        continue
      }
      if (ch == '"' && !innerSingle && !innerBacktick) {
        innerDouble = !innerDouble
        continue
      }
      if (ch == '`' && !innerSingle && !innerDouble) {
        innerBacktick = !innerBacktick
        continue
      }
      if (innerSingle || innerDouble || innerBacktick) {
        continue
      }
      if (ch == '(') {
        depth++
        continue
      }
      if (ch == ')') {
        depth--
        if (depth == 0) {
          closeIdx = i
          break
        }
      }
    }
    if (closeIdx < 0) {
      return null
    }
    String name = input.substring(0, openIdx).trim()
    String args = input.substring(openIdx + 1, closeIdx)
    return new FunctionCall(name, args)
  }

  private static List<Arg> parseArgs(String args) {
    List<String> parts = splitTopLevel(args, ',' as char)
    List<Arg> result = []
    for (String part : parts) {
      String trimmed = part.trim()
      if (trimmed.isEmpty()) {
        continue
      }
      int eqIdx = findTopLevelEquals(trimmed)
      if (eqIdx >= 0) {
        String name = trimmed.substring(0, eqIdx).trim()
        String value = trimmed.substring(eqIdx + 1).trim()
        result.add(new Arg(name, value, true))
      } else {
        result.add(new Arg(null, trimmed, false))
      }
    }
    return result
  }

  private static int findTopLevelEquals(String input) {
    int depth = 0
    boolean inSingle = false
    boolean inDouble = false
    boolean inBacktick = false
    boolean escaped = false
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i)
      if (escaped) {
        escaped = false
        continue
      }
      if (ch == ('\\' as char)) {
        escaped = true
        continue
      }
      if (ch == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle
        continue
      }
      if (ch == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble
        continue
      }
      if (ch == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick
        continue
      }
      if (inSingle || inDouble || inBacktick) {
        continue
      }
      if (ch == '(' || ch == '[' || ch == '{') {
        depth++
        continue
      }
      if (ch == ')' || ch == ']' || ch == '}') {
        depth = Math.max(0, depth - 1)
        continue
      }
      if (depth == 0 && ch == '=') {
        boolean isComparison = (i + 1 < input.length() && input.charAt(i + 1) == '=') ||
            (i > 0 && (input.charAt(i - 1) == '!' || input.charAt(i - 1) == '<' || input.charAt(i - 1) == '>'))
        if (!isComparison) {
          return i
        }
      }
    }
    return -1
  }

  private static String firstPositionalArg(String args) {
    if (args == null) {
      return null
    }
    List<Arg> parsed = parseArgs(args)
    Arg first = parsed.find { !it.named }
    return first?.value
  }

  private static class FunctionCall {
    final String name
    final String args

    FunctionCall(String name, String args) {
      this.name = name
      this.args = args
    }
  }

  private static class Arg {
    final String name
    final String value
    final boolean named

    Arg(String name, String value, boolean named) {
      this.name = name
      this.value = value
      this.named = named
    }
  }
}
