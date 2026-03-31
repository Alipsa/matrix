package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 * Internal parsing and normalization support for formula handling.
 */
@CompileStatic
@PackageScope
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
final class FormulaSupport {

  private FormulaSupport() {
  }

  static ParsedFormula parse(String formula, String defaultResponse) {
    new Parser(requireFormulaText(formula, 'Formula')).parseFormula(defaultResponse)
  }

  static ParsedFormula update(String baseFormula, String updateFormula) {
    ParsedFormula base = parse(baseFormula, null)
    String safeUpdate = requireFormulaText(updateFormula, 'Update formula')
    String leadingTrimmedUpdate = safeUpdate.stripLeading()
    String normalizedUpdate = !safeUpdate.contains('~')
      ? ". ~ ${safeUpdate}"
      : leadingTrimmedUpdate.startsWith('~')
      ? ". ${leadingTrimmedUpdate}"
      : safeUpdate
    ParsedFormula updater = parse(normalizedUpdate, null)
    FormulaExpression response = replaceDots(updater.response, base.response)
    FormulaExpression predictors = replaceDots(updater.predictors, base.predictors)
    new ParsedFormula("${baseFormula} | update ${safeUpdate}", response, predictors)
  }

  static NormalizedFormula normalize(ParsedFormula parsedFormula) {
    FormulaExpression normalizedResponse = normalizeResponse(parsedFormula.response)
    boolean includeIntercept = true
    Set<FormulaTerm> terms = [] as LinkedHashSet<FormulaTerm>

    for (SignedExpression component : flattenSignedExpressions(parsedFormula.predictors, 1)) {
      Boolean interceptDirective = interceptDirective(component.expression)
      if (interceptDirective != null) {
        includeIntercept = component.sign > 0 ? interceptDirective : !interceptDirective
        continue
      }

      Set<FormulaTerm> componentTerms = expandTerms(component.expression)
      if (component.sign > 0) {
        terms.addAll(componentTerms)
      } else {
        terms.removeAll(componentTerms)
      }
    }

    new NormalizedFormula(normalizedResponse, includeIntercept, sortTerms(terms))
  }

  static int operatorPrecedence(String operator) {
    switch (operator) {
      case ':' -> 4
      case '^' -> 3
      case '*', '/' -> 2
      case '+', '-' -> 1
      default -> 0
    }
  }

  private static FormulaExpression normalizeResponse(FormulaExpression expression) {
    FormulaExpression unwrapped = unwrapGrouping(expression)
    if (isMultiResponseFunctionCall(unwrapped)) {
      throw new FormulaParseException('Multiple responses are not supported; use a single response expression', unwrapped.start)
    }
    if (unwrapped instanceof FormulaExpression.Variable || unwrapped instanceof FormulaExpression.FunctionCall) {
      return unwrapped
    }
    if (isMultipleResponseExpression(unwrapped)) {
      throw new FormulaParseException('Multiple responses are not supported; use a single response expression', unwrapped.start)
    }
    throw new FormulaParseException('Response must be a single variable or transform', unwrapped.start)
  }

  private static FormulaExpression unwrapGrouping(FormulaExpression expression) {
    FormulaExpression current = expression
    while (current instanceof FormulaExpression.Grouping) {
      current = (current as FormulaExpression.Grouping).expression
    }
    current
  }

  private static boolean isMultipleResponseExpression(FormulaExpression expression) {
    FormulaExpression unwrapped = unwrapGrouping(expression)
    unwrapped instanceof FormulaExpression.Binary && (unwrapped as FormulaExpression.Binary).operator == '+'
  }

  private static boolean isMultiResponseFunctionCall(FormulaExpression expression) {
    expression instanceof FormulaExpression.FunctionCall &&
      ((expression as FormulaExpression.FunctionCall).name.equalsIgnoreCase('cbind'))
  }

  private static FormulaExpression replaceDots(FormulaExpression expression, FormulaExpression replacement) {
    if (expression instanceof FormulaExpression.Dot) {
      return replacement
    }
    if (expression instanceof FormulaExpression.Grouping) {
      FormulaExpression.Grouping grouping = expression as FormulaExpression.Grouping
      return new FormulaExpression.Grouping(replaceDots(grouping.expression, replacement), grouping.start, grouping.end)
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      return new FormulaExpression.Unary(unary.operator, replaceDots(unary.expression, replacement), unary.start, unary.end)
    }
    if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      return new FormulaExpression.Binary(
        binary.operator,
        replaceDots(binary.left, replacement),
        replaceDots(binary.right, replacement),
        binary.start,
        binary.end
      )
    }
    if (expression instanceof FormulaExpression.FunctionCall) {
      FormulaExpression.FunctionCall functionCall = expression as FormulaExpression.FunctionCall
      return new FormulaExpression.FunctionCall(
        functionCall.name,
        functionCall.arguments.collect { FormulaExpression argument -> replaceDots(argument, replacement) },
        functionCall.start,
        functionCall.end
      )
    }
    expression
  }

  static FormulaExpression expandDots(FormulaExpression expression, List<String> dotColumns) {
    if (dotColumns.isEmpty()) {
      return expression
    }
    if (expression instanceof FormulaExpression.Dot) {
      return buildAdditionChain(dotColumns, expression.start, expression.end)
    }
    if (expression instanceof FormulaExpression.Grouping) {
      FormulaExpression.Grouping grouping = expression as FormulaExpression.Grouping
      FormulaExpression expanded = expandDots(grouping.expression, dotColumns)
      if (expanded.is(grouping.expression)) {
        return expression
      }
      return new FormulaExpression.Grouping(expanded, grouping.start, grouping.end)
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      FormulaExpression expanded = expandDots(unary.expression, dotColumns)
      if (expanded.is(unary.expression)) {
        return expression
      }
      return new FormulaExpression.Unary(unary.operator, expanded, unary.start, unary.end)
    }
    if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      FormulaExpression expandedLeft = expandDots(binary.left, dotColumns)
      FormulaExpression expandedRight = expandDots(binary.right, dotColumns)
      if (expandedLeft.is(binary.left) && expandedRight.is(binary.right)) {
        return expression
      }
      return new FormulaExpression.Binary(binary.operator, expandedLeft, expandedRight, binary.start, binary.end)
    }
    if (expression instanceof FormulaExpression.FunctionCall) {
      FormulaExpression.FunctionCall call = expression as FormulaExpression.FunctionCall
      List<FormulaExpression> expandedArgs = call.arguments.collect { FormulaExpression arg ->
        expandDots(arg, dotColumns)
      }
      boolean changed = false
      for (int i = 0; i < expandedArgs.size(); i++) {
        if (!expandedArgs[i].is(call.arguments[i])) {
          changed = true
          break
        }
      }
      if (!changed) {
        return expression
      }
      return new FormulaExpression.FunctionCall(call.name, expandedArgs, call.start, call.end)
    }
    expression
  }

  private static FormulaExpression buildAdditionChain(List<String> names, int start, int end) {
    FormulaExpression result = new FormulaExpression.Variable(names[0], false, start, end)
    for (int i = 1; i < names.size(); i++) {
      FormulaExpression right = new FormulaExpression.Variable(names[i], false, start, end)
      result = new FormulaExpression.Binary('+', result, right, start, end)
    }
    result
  }

  private static List<SignedExpression> flattenSignedExpressions(FormulaExpression expression, int sign) {
    if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      if (binary.operator == '+') {
        return flattenSignedExpressions(binary.left, sign) + flattenSignedExpressions(binary.right, sign)
      }
      if (binary.operator == '-') {
        return flattenSignedExpressions(binary.left, sign) + flattenSignedExpressions(binary.right, -sign)
      }
    }
    [new SignedExpression(sign, expression)]
  }

  private static Boolean interceptDirective(FormulaExpression expression) {
    if (expression instanceof FormulaExpression.NumberLiteral) {
      BigDecimal value = (expression as FormulaExpression.NumberLiteral).value
      if (value == BigDecimal.ZERO) {
        return false
      }
      if (value == BigDecimal.ONE) {
        return true
      }
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      if (unary.operator == '-' && unary.expression instanceof FormulaExpression.NumberLiteral) {
        BigDecimal value = (unary.expression as FormulaExpression.NumberLiteral).value
        if (value == BigDecimal.ONE) {
          return false
        }
      }
    }
    null
  }

  private static Set<FormulaTerm> expandTerms(FormulaExpression expression) {
    if (expression instanceof FormulaExpression.Grouping) {
      return expandTerms((expression as FormulaExpression.Grouping).expression)
    }
    if (expression instanceof FormulaExpression.Variable ||
      expression instanceof FormulaExpression.Dot ||
      expression instanceof FormulaExpression.FunctionCall) {
      return [FormulaTerm.of(expression)] as LinkedHashSet<FormulaTerm>
    }
    if (expression instanceof FormulaExpression.NumberLiteral) {
      if (interceptDirective(expression) != null) {
        return [] as LinkedHashSet<FormulaTerm>
      }
      throw new FormulaParseException(
        "Numeric literal '${(expression as FormulaExpression.NumberLiteral).sourceText}' is only supported for intercept control",
        expression.start
      )
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      if (unary.operator == '+') {
        return expandTerms(unary.expression)
      }
      if (unary.operator == '-') {
        if (interceptDirective(unary) != null) {
          return [] as LinkedHashSet<FormulaTerm>
        }
        throw new FormulaParseException("Unary '${unary.operator}' is only supported for intercept control", unary.start)
      }
    }
    if (!(expression instanceof FormulaExpression.Binary)) {
      throw new FormulaParseException("Unsupported formula expression: ${expression.class.simpleName}", expression.start)
    }

    FormulaExpression.Binary binary = expression as FormulaExpression.Binary
    Set<FormulaTerm> leftTerms = expandTerms(binary.left)

    switch (binary.operator) {
      case '+' -> combineSets(leftTerms, expandTerms(binary.right))
      case '-' -> subtractSets(leftTerms, expandTerms(binary.right))
      case ':' -> interactions(leftTerms, expandTerms(binary.right))
      case '*' -> {
        Set<FormulaTerm> rightTerms = expandTerms(binary.right)
        combineSets(combineSets(leftTerms, rightTerms), interactions(leftTerms, rightTerms))
      }
      case '/' -> combineSets(leftTerms, interactions(nestingBasis(binary.left), expandTerms(binary.right)))
      case '^' -> powerTerms(binary.left, binary.right)
      default -> throw new FormulaParseException("Unsupported formula operator '${binary.operator}'", binary.start)
    }
  }

  private static Set<FormulaTerm> nestingBasis(FormulaExpression expression) {
    List<FormulaExpression> factors = nestingFactors(expression)
    if (factors.isEmpty()) {
      return [] as LinkedHashSet<FormulaTerm>
    }
    [new FormulaTerm(factors)] as LinkedHashSet<FormulaTerm>
  }

  private static List<FormulaExpression> nestingFactors(FormulaExpression expression) {
    if (expression instanceof FormulaExpression.Grouping) {
      return nestingFactors((expression as FormulaExpression.Grouping).expression)
    }
    if (expression instanceof FormulaExpression.Variable ||
      expression instanceof FormulaExpression.Dot ||
      expression instanceof FormulaExpression.FunctionCall) {
      return [expression]
    }
    if (expression instanceof FormulaExpression.NumberLiteral) {
      return []
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      if (unary.operator == '+') {
        return nestingFactors(unary.expression)
      }
      if (unary.operator == '-' && interceptDirective(unary) != null) {
        return []
      }
      throw new FormulaParseException("Unary '${unary.operator}' is only supported for intercept control", unary.start)
    }
    if (!(expression instanceof FormulaExpression.Binary)) {
      throw new FormulaParseException("Unsupported formula expression: ${expression.class.simpleName}", expression.start)
    }

    FormulaExpression.Binary binary = expression as FormulaExpression.Binary
    switch (binary.operator) {
      case '+', ':', '*', '/' -> mergeFactors(nestingFactors(binary.left), nestingFactors(binary.right))
      case '-' -> removeFactors(nestingFactors(binary.left), nestingFactors(binary.right))
      case '^' -> nestingFactors(binary.left)
      default -> throw new FormulaParseException("Unsupported formula operator '${binary.operator}'", binary.start)
    }
  }

  private static Set<FormulaTerm> powerTerms(FormulaExpression left, FormulaExpression right) {
    if (containsDot(left)) {
      throw new FormulaParseException(
        "Dot expansion with interaction power (^) cannot be normalized before model-frame expansion",
        left.start
      )
    }
    int exponent = positiveIntegerExponent(right)
    Set<FormulaTerm> baseTerms = expandTerms(left)
    if (baseTerms.isEmpty()) {
      return [] as LinkedHashSet<FormulaTerm>
    }

    List<FormulaTerm> orderedTerms = sortTerms(baseTerms)
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    int maxDegree = Math.min(exponent, orderedTerms.size())
    for (int degree = 1; degree <= maxDegree; degree++) {
      appendCombinations(orderedTerms, degree, 0, [] as List<FormulaTerm>, result)
    }
    result
  }

  private static boolean containsDot(FormulaExpression expression) {
    FormulaExpression unwrapped = unwrapGrouping(expression)
    if (unwrapped instanceof FormulaExpression.Dot) {
      return true
    }
    if (unwrapped instanceof FormulaExpression.Unary) {
      return containsDot((unwrapped as FormulaExpression.Unary).expression)
    }
    if (unwrapped instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = unwrapped as FormulaExpression.Binary
      return containsDot(binary.left) || containsDot(binary.right)
    }
    if (unwrapped instanceof FormulaExpression.FunctionCall) {
      FormulaExpression.FunctionCall functionCall = unwrapped as FormulaExpression.FunctionCall
      return functionCall.arguments.any { FormulaExpression argument -> containsDot(argument) }
    }
    false
  }

  private static int positiveIntegerExponent(FormulaExpression expression) {
    String sourceText = expression.asFormulaString()
    if (!(expression instanceof FormulaExpression.NumberLiteral)) {
      throw invalidExponent(expression.start, sourceText)
    }
    FormulaExpression.NumberLiteral numberLiteral = expression as FormulaExpression.NumberLiteral
    BigDecimal normalized = numberLiteral.value.stripTrailingZeros()
    if (normalized.scale() > 0 || normalized <= BigDecimal.ZERO) {
      throw invalidExponent(expression.start, numberLiteral.sourceText)
    }
    try {
      normalized.intValueExact()
    } catch (ArithmeticException ignored) {
      throw invalidExponent(expression.start, numberLiteral.sourceText)
    }
  }

  private static void appendCombinations(
    List<FormulaTerm> terms,
    int targetSize,
    int startIndex,
    List<FormulaTerm> current,
    Set<FormulaTerm> result
  ) {
    if (current.size() == targetSize) {
      result << FormulaTerm.combine(current)
      return
    }
    for (int i = startIndex; i <= terms.size() - (targetSize - current.size()); i++) {
      List<FormulaTerm> next = [*current]
      next << terms[i]
      appendCombinations(terms, targetSize, i + 1, next, result)
    }
  }

  private static Set<FormulaTerm> interactions(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    for (FormulaTerm left : leftTerms) {
      for (FormulaTerm right : rightTerms) {
        result << FormulaTerm.combine([left, right])
      }
    }
    result
  }

  private static Set<FormulaTerm> combineSets(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    result.addAll(leftTerms)
    result.addAll(rightTerms)
    result
  }

  private static List<FormulaExpression> mergeFactors(List<FormulaExpression> leftFactors, List<FormulaExpression> rightFactors) {
    Map<String, FormulaExpression> merged = [:]
    for (FormulaExpression factor : leftFactors) {
      merged[factor.asFormulaString()] = factor
    }
    for (FormulaExpression factor : rightFactors) {
      merged[factor.asFormulaString()] = factor
    }
    merged.values().toList()
  }

  private static List<FormulaExpression> removeFactors(List<FormulaExpression> leftFactors, List<FormulaExpression> rightFactors) {
    Set<String> excluded = rightFactors.collect { FormulaExpression factor -> factor.asFormulaString() } as Set<String>
    leftFactors.findAll { FormulaExpression factor -> !excluded.contains(factor.asFormulaString()) }
  }

  private static Set<FormulaTerm> subtractSets(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    result.addAll(leftTerms)
    result.removeAll(rightTerms)
    result
  }

  private static List<FormulaTerm> sortTerms(Collection<FormulaTerm> terms) {
    terms.toList().sort { FormulaTerm a, FormulaTerm b ->
      int factorOrder = a.factors.size() <=> b.factors.size()
      factorOrder != 0 ? factorOrder : a.asFormulaString() <=> b.asFormulaString()
    } as List<FormulaTerm>
  }

  @CompileStatic
  private static final class SignedExpression {
    final int sign
    final FormulaExpression expression

    SignedExpression(int sign, FormulaExpression expression) {
      this.sign = sign
      this.expression = expression
    }
  }

  @CompileStatic
  private enum TokenType {
    IDENTIFIER,
    BACKTICK_IDENTIFIER,
    NUMBER,
    TILDE,
    PLUS,
    MINUS,
    COLON,
    STAR,
    SLASH,
    CARET,
    LPAREN,
    RPAREN,
    COMMA,
    DOT,
    EOF
  }

  @CompileStatic
  private static final class FormulaToken {
    final TokenType type
    final String text
    final int start
    final int end

    FormulaToken(TokenType type, String text, int start, int end) {
      this.type = type
      this.text = text
      this.start = start
      this.end = end
    }
  }

  @CompileStatic
  private static final class Tokenizer {
    private final String source
    private int index = 0

    Tokenizer(String source) {
      this.source = source
    }

    List<FormulaToken> tokenize() {
      List<FormulaToken> tokens = []
      while (index < source.length()) {
        char current = source.charAt(index)
        if (Character.isWhitespace(current)) {
          index++
          continue
        }

        int start = index
        if (isNumberStart(current)) {
          tokens << numberLiteral(start)
          continue
        }
        if (isIdentifierStart(current)) {
          tokens << identifier(start)
          continue
        }
        FormulaToken singleCharacterToken = singleCharacterToken(current, start)
        if (singleCharacterToken != null) {
          tokens << singleCharacterToken
          continue
        }

        throw parseError("Unexpected character '${current}'", start)
      }
      tokens << new FormulaToken(TokenType.EOF, '', source.length(), source.length())
      tokens
    }

    private boolean isNumberStart(char current) {
      Character.isDigit(current) || (current == '.' && index + 1 < source.length() && Character.isDigit(source.charAt(index + 1)))
    }

    private boolean isIdentifierStart(char current) {
      Character.isLetter(current) ||
        current == '_' ||
        (current == '.' && index + 1 < source.length() && isIdentifierCharacter(source.charAt(index + 1)) && !Character.isDigit(source.charAt(index + 1)))
    }

    private FormulaToken singleCharacterToken(char current, int start) {
      switch (current) {
        case '~' -> new FormulaToken(TokenType.TILDE, '~', start, ++index)
        case '+' -> new FormulaToken(TokenType.PLUS, '+', start, ++index)
        case '-' -> new FormulaToken(TokenType.MINUS, '-', start, ++index)
        case ':' -> new FormulaToken(TokenType.COLON, ':', start, ++index)
        case '*' -> new FormulaToken(TokenType.STAR, '*', start, ++index)
        case '/' -> new FormulaToken(TokenType.SLASH, '/', start, ++index)
        case '^' -> new FormulaToken(TokenType.CARET, '^', start, ++index)
        case '(' -> new FormulaToken(TokenType.LPAREN, '(', start, ++index)
        case ')' -> new FormulaToken(TokenType.RPAREN, ')', start, ++index)
        case ',' -> new FormulaToken(TokenType.COMMA, ',', start, ++index)
        case '.' -> new FormulaToken(TokenType.DOT, '.', start, ++index)
        case '`' -> backtickIdentifier(start)
        default -> null
      }
    }

    private FormulaToken identifier(int start) {
      while (index < source.length()) {
        char value = source.charAt(index)
        if (!isIdentifierCharacter(value)) {
          break
        }
        index++
      }
      new FormulaToken(TokenType.IDENTIFIER, source.substring(start, index), start, index)
    }

    private FormulaToken numberLiteral(int start) {
      if (source.charAt(index) == '.') {
        index++
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
          index++
        }
      } else {
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
          index++
        }
        if (index < source.length() && source.charAt(index) == '.') {
          index++
          while (index < source.length() && Character.isDigit(source.charAt(index))) {
            index++
          }
        }
      }
      if (index < source.length() && isExponentMarker(source.charAt(index))) {
        int exponentStart = index++
        if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
          index++
        }
        if (index >= source.length() || !Character.isDigit(source.charAt(index))) {
          throw parseError('Invalid scientific notation', exponentStart)
        }
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
          index++
        }
      }
      new FormulaToken(TokenType.NUMBER, source.substring(start, index), start, index)
    }

    private static boolean isExponentMarker(char current) {
      current == 'e' || current == 'E'
    }

    private static boolean isIdentifierCharacter(char value) {
      Character.isLetterOrDigit(value) || value == '_' || value == '.'
    }

    private FormulaToken backtickIdentifier(int start) {
      index++ // opening backtick
      StringBuilder builder = new StringBuilder()
      while (index < source.length() && source.charAt(index) != '`') {
        builder.append(source.charAt(index))
        index++
      }
      if (index >= source.length()) {
        throw parseError('Unterminated backtick identifier', start)
      }
      if (builder.isEmpty()) {
        throw parseError('Backtick identifier cannot be blank', start)
      }
      index++ // closing backtick
      new FormulaToken(TokenType.BACKTICK_IDENTIFIER, builder.toString(), start, index)
    }

    private FormulaParseException parseError(String message, int position) {
      FormulaSupport.parseError(source, position, message)
    }
  }

  @CompileStatic
  private static final class Parser {
    private final String source
    private final List<FormulaToken> tokens
    private int index = 0

    Parser(String source) {
      this.source = source
      this.tokens = new Tokenizer(source).tokenize()
    }

    ParsedFormula parseFormula(String defaultResponse) {
      FormulaExpression firstSide = parseExpression()
      if (match(TokenType.TILDE)) {
        if (peek().type == TokenType.EOF) {
          throw parseError('Formula is missing a right-hand side', peek().start)
        }
        FormulaExpression predictors = parseExpression()
        expect(TokenType.EOF, 'Unexpected trailing input after formula')
        return new ParsedFormula(source, firstSide, predictors)
      }

      if (defaultResponse == null || defaultResponse.isBlank()) {
        throw parseError("Formula must contain '~' unless a default response is provided", peek().start)
      }

      FormulaExpression response = new Parser(defaultResponse).parseSideOnly()
      expect(TokenType.EOF, 'Unexpected trailing input after predictor expression')
      new ParsedFormula("${defaultResponse} ~ ${source}", response, firstSide)
    }

    private FormulaExpression parseSideOnly() {
      FormulaExpression expression = parseExpression()
      expect(TokenType.EOF, 'Unexpected trailing input in formula side')
      expression
    }

    private FormulaExpression parseExpression() {
      parseAdditive()
    }

    private FormulaExpression parseAdditive() {
      FormulaExpression expression = parseMultiplicative()
      while (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
        FormulaToken operator = advance()
        FormulaExpression right = parseMultiplicative()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseMultiplicative() {
      FormulaExpression expression = parsePower()
      while (peek().type == TokenType.STAR || peek().type == TokenType.SLASH) {
        FormulaToken operator = advance()
        FormulaExpression right = parsePower()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parsePower() {
      FormulaExpression expression = parseInteraction()
      while (peek().type == TokenType.CARET) {
        FormulaToken operator = advance()
        FormulaExpression right = parseInteraction()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseInteraction() {
      FormulaExpression expression = parseUnary()
      while (peek().type == TokenType.COLON) {
        FormulaToken operator = advance()
        FormulaExpression right = parseUnary()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseUnary() {
      if (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
        FormulaToken operator = advance()
        FormulaExpression right = parseUnary()
        return new FormulaExpression.Unary(operator.text, right, operator.start, right.end)
      }
      parsePrimary()
    }

    private FormulaExpression parsePrimary() {
      FormulaToken token = advance()
      if (token.type == TokenType.IDENTIFIER) {
        if (match(TokenType.LPAREN)) {
          return parseFunctionCall(token)
        }
        return new FormulaExpression.Variable(token.text, false, token.start, token.end)
      }
      if (token.type == TokenType.BACKTICK_IDENTIFIER) {
        return new FormulaExpression.Variable(token.text, true, token.start, token.end)
      }
      if (token.type == TokenType.NUMBER) {
        return new FormulaExpression.NumberLiteral(new BigDecimal(token.text), token.text, token.start, token.end)
      }
      if (token.type == TokenType.DOT) {
        return new FormulaExpression.Dot(token.start, token.end)
      }
      if (token.type == TokenType.LPAREN) {
        FormulaExpression inner = parseExpression()
        FormulaToken closing = expect(TokenType.RPAREN, "Expected ')' to close grouped expression")
        return new FormulaExpression.Grouping(inner, token.start, closing.end)
      }
      throw parseError("Unexpected token '${token.text ?: token.type.name()}'", token.start)
    }

    private FormulaExpression parseFunctionCall(FormulaToken nameToken) {
      List<FormulaExpression> arguments = []
      if (!match(TokenType.RPAREN)) {
        do {
          arguments << parseExpression()
        } while (match(TokenType.COMMA))
        FormulaToken closing = expect(TokenType.RPAREN, "Expected ')' after function arguments")
        return new FormulaExpression.FunctionCall(nameToken.text, arguments, nameToken.start, closing.end)
      }
      new FormulaExpression.FunctionCall(nameToken.text, arguments, nameToken.start, previous().end)
    }

    private boolean match(TokenType type) {
      if (peek().type != type) {
        return false
      }
      advance()
      true
    }

    private FormulaToken expect(TokenType type, String message) {
      if (peek().type != type) {
        throw parseError(message, peek().start)
      }
      advance()
    }

    private FormulaToken advance() {
      FormulaToken token = tokens[index]
      index++
      token
    }

    private FormulaToken previous() {
      tokens[index - 1]
    }

    private FormulaToken peek() {
      tokens[index]
    }

    private FormulaParseException parseError(String message, int position) {
      FormulaSupport.parseError(source, position, message)
    }
  }

  private static FormulaParseException parseError(String source, int position, String message) {
    int safePosition = Math.max(0, Math.min(position, source.length()))
    String pointer = "${' ' * safePosition}^"
    new FormulaParseException("${message} at position ${safePosition}\n${source}\n${pointer}", safePosition)
  }

  private static FormulaParseException invalidExponent(int position, String sourceText) {
    new FormulaParseException("Interaction expansion (^) requires a positive integer exponent, got '${sourceText}'", position)
  }

  private static String requireFormulaText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new FormulaParseException("${label} cannot be null or blank", 0)
    }
    value
  }
}
