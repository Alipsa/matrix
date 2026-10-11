package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.junit.jupiter.api.Test

class AuthenticationInventoryTest {
  private static final String OWNER = 'se.alipsa.matrix.gsheets.GsAuthenticator'

  @Test
  void productionCallSitesAreMapped() {
    List<String> inventory = []
    new File('src/main/groovy').eachFileRecurse { File source ->
      if (source.name.endsWith('.groovy') && source.name != 'GsAuthenticator.groovy') {
        inventory.addAll(calls(source.text))
      }
    }
    assertEquals([
        'se.alipsa.matrix.gsheets.GsUtil#deleteSheet|authenticate(scopes)',
        'se.alipsa.matrix.gsheets.GsUtil#getSheetNames|authenticate(GsAuthenticator.SCOPE_SHEETS_READONLY)',
        'se.alipsa.matrix.gsheets.GsheetsReader#buildSheetsService|authenticate(GsAuthenticator.SCOPE_SHEETS_READONLY)',
        'se.alipsa.matrix.gsheets.GsheetsWriter#buildSheetsService|authenticate(SCOPES)'
    ].sort(), inventory.sort(), 'Map every added authentication call to an exercised public operation')
  }

  @Test
  void resolvesNamedAliasedAndWildcardImportsAndShadowing() {
    assertEquals(['Example#run|authenticate([])'], calls("import static ${OWNER}.authenticate\nclass Example { void run() { authenticate([]) } }"))
    assertEquals(['Example#run|auth([])'], calls("import static ${OWNER}.authenticate as auth\nclass Example { void run() { auth([]) } }"))
    assertEquals(['Example#run|authenticate([])'], calls("import static ${OWNER}.*\nclass Example { void run() { authenticate([]) } }"))
    assertTrue(calls("import static ${OWNER}.*\nclass Example { void authenticate(List scopes) {}\nvoid run() { authenticate([]) } }").empty)
    assertTrue(calls('class Example { void run() { def authenticate = { List scopes -> scopes }; authenticate([]) } }').empty)
    assertTrue(calls("import static ${OWNER}.*\nclass Example { void run() { def authenticate = { List scopes -> scopes }; authenticate([]) } }").empty)
    assertTrue(calls('// GsAuthenticator.authenticate([])\nclass Example {}').empty)
  }

  private static List<String> calls(String text) {
    CompilerConfiguration configuration = new CompilerConfiguration()
    configuration.classpath = System.getProperty('authInventoryClasspath', System.getProperty('java.class.path'))
    SourceUnit unit = new SourceUnit('Inventory.groovy', text, configuration, null, null)
    unit.parse()
    unit.completePhase()
    unit.nextPhase()
    unit.convert()
    List<String> found = []
    unit.AST.classes.each { cls ->
      def callableBodies = (cls.methods + cls.declaredConstructors).collect {
        [name: it.name, parameters: it.parameters, code: it.code]
      } + cls.fields.collect { [name: "<field:${it.name}>", parameters: [], code: it.initialExpression] }
      callableBodies.each { method ->
        Set<String> localNames = (method.parameters*.name + cls.fields*.name) as Set<String>
        method.code?.visit(new ClassCodeVisitorSupport() {
          @Override
          protected SourceUnit getSourceUnit() { unit }

          @Override
          void visitDeclarationExpression(DeclarationExpression declaration) {
            if (declaration.leftExpression instanceof VariableExpression) {
              localNames.add(declaration.leftExpression.name)
            }
            super.visitDeclarationExpression(declaration)
          }

          @Override
          void visitMethodCallExpression(MethodCallExpression call) {
            String name = call.methodAsString
            def named = unit.AST.staticImports[name]
            boolean imported = named?.type?.name == AuthenticationInventoryTest.OWNER && named.fieldName == 'authenticate'
            boolean wildcard = named == null && name == 'authenticate' && unit.AST.staticStarImports.values().any { it.type.name == AuthenticationInventoryTest.OWNER }
            boolean qualified = name == 'authenticate' && call.objectExpression.text in [AuthenticationInventoryTest.OWNER, 'GsAuthenticator']
            boolean shadowed = localNames.contains(name) || cls.methods.any { it.name == name } || call.objectExpression.text != 'this'
            if (qualified || ((imported || wildcard) && !shadowed)) {
              found.add("${cls.name}#${method.name}|${name}${call.arguments.text}".toString())
            }
            super.visitMethodCallExpression(call)
          }

          @Override
          void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
            if (call.ownerType.name == AuthenticationInventoryTest.OWNER && call.method == 'authenticate') {
              found.add("${cls.name}#${method.name}|${call.method}${call.arguments.text}".toString())
            }
            super.visitStaticMethodCallExpression(call)
          }
        })
      }
    }
    found
  }
}
