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
modules.each { String module ->
  if (module == ':matrix-bigquery' && !dedicated) return
  File sources = new File(root, module.substring(1) + '/src/test')
  if (!sources.exists()) return
  sources.eachFileRecurse { File source ->
    if (source.name.endsWith('.java')) {
      ToolProvider.systemJavaCompiler.getStandardFileManager(null, null, null).withCloseable { manager ->
        def task = ToolProvider.systemJavaCompiler.getTask(null, manager, null, ['-proc:none'], null, manager.getJavaFileObjects(source))
        task.parse().each { tree ->
          Closure owner = { annotation ->
            String name = annotation.annotationType.toString()
            if (name.contains('.')) return name
            def named = tree.imports.find { it.qualifiedIdentifier.toString().tokenize('.').last() == name }
            named?.qualifiedIdentifier?.toString() ?: (tree.imports.any { it.qualifiedIdentifier.toString() == 'org.junit.jupiter.api.*' } ? 'org.junit.jupiter.api.' + name : name)
          }
          Closure<Set<String>> tags = { annotations ->
            if (annotations.any { owner(it) == 'org.junit.jupiter.api.Tags' }) throw new IllegalStateException('Map container test tags explicitly')
            annotations.findAll { owner(it) == 'org.junit.jupiter.api.Tag' }.collect { annotation ->
              def value = annotation.arguments.first()
              if (!(value instanceof LiteralTree)) throw new IllegalStateException('Resolve nonliteral Java test tags explicitly')
              value.value.toString()
            } as Set<String>
          }
          tree.accept(new TreeScanner<Void, Map>() {
            @Override
            Void visitClass(ClassTree cls, Map parent) {
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
      }
      return
    }
    if (!source.name.endsWith('.groovy')) return
    def unit = SourceUnit.create(source.name, source.text)
    unit.parse(); unit.completePhase(); unit.nextPhase(); unit.convert()
    Closure owner = { annotation ->
      String name = annotation.classNode.name
      if (name.contains('.')) return name
      unit.AST.imports.find { it.alias == name }?.type?.name ?:
        (unit.AST.starImports.any { it.packageName == 'org.junit.jupiter.api.' } ? 'org.junit.jupiter.api.' + name : name)
    }
    Closure<Set<String>> tags = { annotations ->
      if (annotations.any { owner(it) == 'org.junit.jupiter.api.Tags' }) throw new IllegalStateException('Map container test tags explicitly')
      annotations.findAll { owner(it) == 'org.junit.jupiter.api.Tag' }.collect { annotation ->
        if (!(annotation.getMember('value') instanceof ConstantExpression)) throw new IllegalStateException('Resolve nonliteral external-test tags explicitly')
        annotation.getMember('value').text
      } as Set<String>
    }
    unit.AST.classes.each { cls ->
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
