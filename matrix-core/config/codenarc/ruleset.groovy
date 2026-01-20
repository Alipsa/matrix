ruleset {
    description 'CodeNarc ruleset for Matrix Core'

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
            regex = /[A-Z][\w]*/
        }
        'FieldName' {
            finalRegex = /[a-z][a-zA-Z0-9]*/
            staticFinalRegex = /[A-Z][A-Z_0-9]*/
            regex = /[a-z][a-zA-Z0-9]*/
        }
    }

    // Size/Complexity rules
    ruleset('rulesets/size.xml') {
        'CyclomaticComplexity' {
            maxMethodComplexity = 20
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
        exclude 'ClassJavadoc' // Don't require Javadoc on all classes
    }

    // Groovy-specific rules
    ruleset('rulesets/groovyism.xml') {
        exclude 'ExplicitCallToEqualsMethod'
        exclude 'ExplicitCallToCompareToMethod'
    }
}
