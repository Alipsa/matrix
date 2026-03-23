ruleset {
    description 'CodeNarc ruleset for Matrix'

    // Basic rules
    ruleset('rulesets/basic.xml') {
        exclude 'EmptyCatchBlock' // Allow empty catch blocks for now
        exclude 'EmptyIfStatement'
        exclude 'EmptyElseBlock'
    }

    // Braces rules
    ruleset('rulesets/braces.xml')

    // Design rules
    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod'
        exclude 'BuilderMethodWithSideEffects'
        'Instanceof' {
            priority = 3
        }
    }

    // DRY rules
    ruleset('rulesets/dry.xml') {
        'DuplicateNumberLiteral' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'DuplicateStringLiteral' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
    }

    // Exception handling
    ruleset('rulesets/exceptions.xml') {
        exclude 'CatchException' // Allow catching Exception for compatibility
        exclude 'CatchThrowable'
    }

    // Imports
    ruleset('rulesets/imports.xml') {
        exclude 'NoWildcardImports' // Allow wildcard imports in Groovy style
    }

    // Naming conventions
    ruleset('rulesets/naming.xml') {
        'MethodName' {
            regex = /[a-z][\w]*/
        }
        'ClassName' {
            regex = /[A-Z][\w$]*/
        }
        'FieldName' {
            finalRegex = /[a-z][a-zA-Z0-9]*/
            staticRegex = /([a-z][a-zA-Z0-9]*|[A-Z][A-Z_0-9]*)/
            staticFinalRegex = /(log|[A-Z][A-Z_0-9]*)/
            regex = /[a-z][a-zA-Z0-9]*/
        }
        exclude 'FactoryMethodName'
        exclude 'ConfusingMethodName'
    }

    // Size/Complexity rules
    ruleset('rulesets/size.xml') {
        exclude 'CrapMetric' // Requires a Cobertura XML report; this build uses JaCoCo instead
        'AbcMetric' {
            maxMethodAbcScore = 70
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'CyclomaticComplexity' {
            maxMethodComplexity = 35
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'MethodCount' {
            maxMethods = 250
        }
        'MethodSize' {
            maxLines = 100
        }
        'ClassSize' {
            maxLines = 1000
        }
        'ParameterCount' {
            maxParameters = 7
        }
    }

    // Unnecessary code
    ruleset('rulesets/unnecessary.xml') {
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessarySetter'
        exclude 'UnnecessaryReturnKeyword'
        exclude 'UnnecessaryPublicModifier'
    }

    // Unused code
    ruleset('rulesets/unused.xml') {
        exclude 'UnusedVariable' // Too strict for dynamic Groovy code
    }

    // Formatting
    ruleset('rulesets/formatting.xml') {
        exclude 'SpaceAroundMapEntryColon'
        exclude 'LineLength' // Don't enforce line length
        exclude 'Indentation' // Skip indentation checks
    }

    // Comments rules (ClassJavadoc moved here in CodeNarc 3.7+)
    ruleset('rulesets/comments.xml') {
        'ClassJavadoc' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
    }

    // Groovy-specific rules
    ruleset('rulesets/groovyism.xml')
}
