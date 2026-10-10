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
Map<String, Map> sourceClasses = [:]
Closure groovyOwner = { unit, annotation ->
  String name = annotation.classNode.name
  if (name.contains('.')) return name
  unit.AST.imports.find { it.alias == name }?.type?.name ?:
    (name in ['Override', 'Deprecated', 'SuppressWarnings'] ? 'java.lang.' + name :
      (unit.AST.starImports.any { it.packageName == 'org.junit.jupiter.api.' } ? 'org.junit.jupiter.api.' + name : name))
}
Closure javaOwner = { tree, annotation ->
  String name = annotation.annotationType.toString()
  if (name.contains('.')) return name
  if (name in ['Override', 'Deprecated', 'SuppressWarnings']) return 'java.lang.' + name
  def named = tree.imports.find { it.qualifiedIdentifier.toString().tokenize('.').last() == name }
  named?.qualifiedIdentifier?.toString() ?: (tree.imports.any { it.qualifiedIdentifier.toString() == 'org.junit.jupiter.api.*' } ? 'org.junit.jupiter.api.' + name : name)
}
modules.each { String module ->
  if (module == ':matrix-bigquery' && !dedicated) return
  File sources = new File(root, module.substring(1) + '/src/test')
  if (!sources.exists()) return
  sources.eachFileRecurse { File source ->
    if (!(source.name.endsWith('.groovy') || source.name.endsWith('.java'))) return
    if (source.name.endsWith('.java')) {
      parseJava(source) { tree ->
        Closure owner = { annotation -> javaOwner(tree, annotation) }
        tree.accept(new TreeScanner<Void, String>() {
          @Override
          Void visitClass(ClassTree cls, String enclosing) {
            String name = enclosing ? "${enclosing}.${cls.simpleName}" : [tree.packageName, cls.simpleName].findAll { it }.join('.')
            sourceClasses["${module}|${name}".toString()] = [name: name, module: module,
              packageName: tree.packageName?.toString() ?: '', superclass: cls.extendsClause?.toString(),
              annotations: cls.modifiers.annotations, owner: owner,
              imports: tree.imports.collect { it.qualifiedIdentifier.toString() }]
            super.visitClass(cls, name)
          }
        }, null)
      }
      return
    }
    def unit = parseGroovy(source)
    unit.AST.classes.each { cls ->
      String name = cls.name.replace('$', '.')
      sourceClasses["${module}|${name}".toString()] = [name: name, module: module,
        packageName: unit.AST.packageName?.replaceFirst(/\.$/, '') ?: '', superclass: cls.superClass?.name,
        annotations: cls.annotations, owner: { annotation -> groovyOwner(unit, annotation) },
        imports: unit.AST.imports.collect { it.type.name },
        aliases: unit.AST.imports.collectEntries { [(it.alias): it.type.name] },
        starImports: unit.AST.starImports.collect { it.packageName + '*' }]
    }
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
// Resolve within the module, respecting explicit imports, packages and enclosing
// classes. Never match a library superclass against an unrelated simple name.
Closure<Map> superclass = { Map cls ->
  if (cls == null) return null
  String name = cls.superclass?.replaceAll(/<.*>/, '')?.replace('$', '.')
  if (!name) return null
  String first = name.tokenize('.').first()
  String imported = cls.aliases?.get(first) ?: cls.imports.find { it.tokenize('.').last() == first }
  List<String> candidates = []
  if (imported) {
    candidates.add(imported + name.substring(first.length()))
  } else if (name.contains('.') && Character.isLowerCase(name.charAt(0))) {
    candidates.add(name)
  } else {
    String enclosing = cls.name
    while (enclosing.contains('.')) {
      enclosing = enclosing.substring(0, enclosing.lastIndexOf('.'))
      if (enclosing == cls.packageName) break
      candidates.add(enclosing + '.' + name)
    }
    candidates.add(cls.packageName ? cls.packageName + '.' + name : name)
    (cls.starImports ?: cls.imports.findAll { it.endsWith('.*') }).each { candidates.add(it.substring(0, it.length() - 1) + name) }
  }
  candidates.collect { sourceClasses["${cls.module}|${it}".toString()] }.find { it != null }
}
Closure checkSuperclass = { String site, String module ->
  Map cls = sourceClasses["${module}|${site.replace('$', '.')}".toString()]
  Set<String> visited = []
  while ((cls = superclass(cls)) != null) {
    if (!visited.add(cls.name)) throw new IllegalStateException("Cyclic test-source inheritance for ${site}")
    boolean affectsTags = cls.annotations.any { annotation ->
      String name = cls.owner(annotation)
      name in ['org.junit.jupiter.api.Tag', 'org.junit.jupiter.api.Tags', 'org.junit.jupiter.api.Disabled'] ||
        !composedTags(name, [] as Set<String>).empty
    }
    if (affectsTags) {
      throw new IllegalStateException("Cannot resolve inherited test tags for ${site} from ${cls.name}; declare the effective tags and disabled state explicitly on a test without tagged test-source inheritance")
    }
  }
}
modules.each { String module ->
  if (module == ':matrix-bigquery' && !dedicated) return
  File sources = new File(root, module.substring(1) + '/src/test')
  if (!sources.exists()) return
  sources.eachFileRecurse { File source ->
    if (source.name.endsWith('.java')) {
      parseJava(source) { tree ->
        Closure owner = { annotation -> javaOwner(tree, annotation) }
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
            Map context = [name: parent ? "${parent.name}\$${cls.simpleName}" : [tree.packageName, cls.simpleName].findAll { it }.join('.'), tags: (parent?.tags ?: [] as Set) + tags(cls.modifiers.annotations),
              disabled: parent?.disabled || cls.modifiers.annotations.any { owner(it) == 'org.junit.jupiter.api.Disabled' }]
            checkSuperclass(context.name.toString(), module)
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
      checkSuperclass(cls.name, module)
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
