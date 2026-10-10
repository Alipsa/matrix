#!/usr/bin/env groovy
import com.sun.source.tree.ClassTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MethodTree
import com.sun.source.util.TreeScanner
import groovy.json.JsonSlurper
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.SourceUnit
import javax.tools.ToolProvider

// Inspect real annotations and effective filters, not comments or profile activation.
def manifest = new JsonSlurper().parse(new File(args[0]))
boolean external = System.getenv('RUN_EXTERNAL_TESTS') == 'true'
boolean dedicated = System.getenv('RELEASE_ALL_DEDICATED_EXTERNAL_TESTS') == 'true'
boolean slow = System.getenv('RUN_SLOW_TESTS') != 'false'
if (dedicated && !external) throw new IllegalStateException('Dedicated external tests require RUN_EXTERNAL_TESTS=true')
if (!external) return
File root = new File(args[1])
Map mappings = new JsonSlurper().parse(new File(root, 'scripts/release-all/external-requirements.json')) as Map
Set<String> requirements = []
Set<String> modules = manifest.verificationScope as Set<String>
if (manifest.selected?.any { !it.projectPath }) modules.add(':matrix-bom')
Set<String> testAnnotations = ['org.junit.jupiter.api.Test', 'org.junit.jupiter.api.RepeatedTest',
  'org.junit.jupiter.api.TestFactory', 'org.junit.jupiter.api.TestTemplate', 'org.junit.jupiter.params.ParameterizedTest']
Closure<Boolean> excluded = { Set<String> tags -> tags.contains('flaky') || (!slow && tags.contains('slow')) }
Closure register = { String site, String module ->
  Map mapped = mappings[site]
  if (mapped == null || mapped.module != module) throw new IllegalStateException("Unmapped external-tagged test: ${site}; update external-requirements.json")
  requirements.addAll(mapped.requirements as List<String>)
  System.err.println("External prerequisite triggered by ${site}")
}
Closure parseGroovy = { File source ->
  def unit = SourceUnit.create(source.name, source.text)
  unit.parse(); unit.completePhase(); unit.nextPhase(); unit.convert()
  unit
}
Closure parseJava = { File source, Closure inspect ->
  ToolProvider.systemJavaCompiler.getStandardFileManager(null, null, null).withCloseable { manager ->
    def task = ToolProvider.systemJavaCompiler.getTask(null, manager, null, ['-proc:none'], null, manager.getJavaFileObjects(source))
    task.parse().each(inspect)
  }
}
// Index local annotation definitions before scanning their usages. Unknown composed
// annotations and test-source inheritance must not silently hide JUnit tags.
Map<String, Map> localAnnotations = [:]
Set<String> sourceClasses = []
Closure groovyOwner = { unit, annotation ->
  String name = annotation.classNode.name
  if (name.contains('.')) return name
  unit.AST.imports.find { it.alias == name }?.type?.name ?:
    (name in ['Override', 'Deprecated', 'SuppressWarnings'] ? 'java.lang.' + name :
      (unit.AST.starImports.any { it.packageName == 'org.junit.jupiter.api.' } ? 'org.junit.jupiter.api.' + name : name))
}
modules.each { String module ->
  if (module == ':matrix-bigquery' && !dedicated) return
  File sources = new File(root, module.substring(1) + '/src/test')
  if (!sources.exists()) return
  sources.eachFileRecurse { File source ->
    if (!(source.name.endsWith('.groovy') || source.name.endsWith('.java'))) return
    sourceClasses.add(source.name.replaceFirst(/\.(groovy|java)$/, ''))
    if (source.name.endsWith('.java')) {
      parseJava(source) { tree ->
        tree.accept(new TreeScanner<Void, Void>() {
          @Override
          Void visitClass(ClassTree cls, Void ignored) {
            sourceClasses.add(cls.simpleName.toString())
            super.visitClass(cls, ignored)
          }
        }, null)
      }
      return
    }
    def unit = parseGroovy(source)
    sourceClasses.addAll(unit.AST.classes.collect { it.nameWithoutPackage.tokenize('$').last() })
    unit.AST.classes.findAll { it.isAnnotationDefinition() }.each { cls ->
      localAnnotations[cls.name] = [annotations: cls.annotations, owner: { annotation -> groovyOwner(unit, annotation) }]
    }
  }
}
Closure<Set<String>> composedTags
composedTags = { String name, Set<String> visiting ->
  Map definition = localAnnotations[name]
  if (definition == null) {
    if (name.startsWith('org.junit.jupiter.') || name.startsWith('java.lang.') || name.startsWith('groovy.') ||
        name in ['Override', 'Deprecated', 'SuppressWarnings', 'org.testcontainers.junit.jupiter.Testcontainers', 'org.testcontainers.junit.jupiter.Container']) return [] as Set<String>
    throw new IllegalStateException("Cannot resolve test annotation ${name}; use explicit JUnit tags or a local Groovy annotation")
  }
  if (name in visiting) throw new IllegalStateException("Cyclic test annotation ${name}")
  Set<String> result = []
  definition.annotations.each { annotation ->
    String owner = definition.owner(annotation)
    if (owner == 'org.junit.jupiter.api.Tag') {
      if (!(annotation.getMember('value') instanceof ConstantExpression)) throw new IllegalStateException("Resolve nonliteral test tag in ${name} explicitly")
      result.add(annotation.getMember('value').text)
    } else if (owner == 'org.junit.jupiter.api.Tags') {
      throw new IllegalStateException("Map container test tags in ${name} explicitly")
    } else {
      result.addAll(composedTags(owner, visiting + name))
    }
  }
  result
}
Closure checkSuperclass = { String name, String site ->
  if (name && name.tokenize('.').last() in sourceClasses) {
    throw new IllegalStateException("Cannot resolve inherited test tags for ${site} extending test-source class ${name}; declare tests without test-source inheritance")
  }
}
modules.each { String module ->
  if (module == ':matrix-bigquery' && !dedicated) return
  File sources = new File(root, module.substring(1) + '/src/test')
  if (!sources.exists()) return
  sources.eachFileRecurse { File source ->
    if (source.name.endsWith('.java')) {
      parseJava(source) { tree ->
        Closure owner = { annotation ->
          String name = annotation.annotationType.toString()
          if (name.contains('.')) return name
          if (name in ['Override', 'Deprecated', 'SuppressWarnings']) return 'java.lang.' + name
          def named = tree.imports.find { it.qualifiedIdentifier.toString().tokenize('.').last() == name }
          named?.qualifiedIdentifier?.toString() ?: (tree.imports.any { it.qualifiedIdentifier.toString() == 'org.junit.jupiter.api.*' } ? 'org.junit.jupiter.api.' + name : name)
        }
        Closure<Set<String>> tags = { annotations ->
          if (annotations.any { owner(it) == 'org.junit.jupiter.api.Tags' }) throw new IllegalStateException('Map container test tags explicitly')
          Set<String> indirect = annotations.findAll { !(owner(it) in ['org.junit.jupiter.api.Tag', 'org.junit.jupiter.api.Tags']) }.collectMany { composedTags(owner(it), [] as Set<String>) }.toSet()
          indirect + (annotations.findAll { owner(it) == 'org.junit.jupiter.api.Tag' }.collect { annotation ->
            def value = annotation.arguments.first()
            if (!(value instanceof LiteralTree)) throw new IllegalStateException('Resolve nonliteral Java test tags explicitly')
            value.value.toString()
          } as Set<String>)
        }
        tree.accept(new TreeScanner<Void, Map>() {
          @Override
          Void visitClass(ClassTree cls, Map parent) {
            checkSuperclass(cls.extendsClause?.toString(), cls.simpleName.toString())
            Map context = [name: parent ? "${parent.name}\$${cls.simpleName}" : "${tree.packageName}.${cls.simpleName}", tags: (parent?.tags ?: [] as Set) + tags(cls.modifiers.annotations),
              disabled: parent?.disabled || cls.modifiers.annotations.any { owner(it) == 'org.junit.jupiter.api.Disabled' }]
            super.visitClass(cls, context)
          }
          @Override
          Void visitMethod(MethodTree method, Map context) {
            Set<String> effective = context.tags + tags(method.modifiers.annotations)
            if (!context.disabled && !method.modifiers.annotations.any { owner(it) == 'org.junit.jupiter.api.Disabled' } &&
                method.modifiers.annotations.any { owner(it) in testAnnotations } && effective.contains('external') && !excluded(effective)) {
              register("${context.name}#${method.name}".toString(), module)
            }
            super.visitMethod(method, context)
          }
        }, null)
      }
      return
    }
    if (!source.name.endsWith('.groovy')) return
    def unit = parseGroovy(source)
    Closure owner = { annotation -> groovyOwner(unit, annotation) }
    Closure<Set<String>> tags = { annotations ->
      if (annotations.any { owner(it) == 'org.junit.jupiter.api.Tags' }) throw new IllegalStateException('Map container test tags explicitly')
      Set<String> indirect = annotations.findAll { !(owner(it) in ['org.junit.jupiter.api.Tag', 'org.junit.jupiter.api.Tags']) }.collectMany { composedTags(owner(it), [] as Set<String>) }.toSet()
      indirect + (annotations.findAll { owner(it) == 'org.junit.jupiter.api.Tag' }.collect { annotation ->
        if (!(annotation.getMember('value') instanceof ConstantExpression)) throw new IllegalStateException('Resolve nonliteral external-test tags explicitly')
        annotation.getMember('value').text
      } as Set<String>)
    }
    unit.AST.classes.each { cls ->
      checkSuperclass(cls.superClass?.name, cls.name)
      List ancestors = []
      def enclosing = cls
      while (enclosing != null) {
        ancestors.add(enclosing)
        enclosing = enclosing.outerClass
      }
      Set<String> classTags = ancestors.collectMany { tags(it.annotations) }.toSet()
      if (excluded(classTags) || ancestors.any { ancestor -> ancestor.annotations.any { owner(it) == 'org.junit.jupiter.api.Disabled' } }) return
      cls.methods.each { method ->
        Set<String> effective = classTags + tags(method.annotations)
        if (method.annotations.any { owner(it) in testAnnotations } && effective.contains('external') && !excluded(effective) &&
            !method.annotations.any { owner(it) == 'org.junit.jupiter.api.Disabled' }) {
          register("${cls.name}#${method.name}".toString(), module)
        }
      }
    }
  }
}
requirements.sort().each { println it }
