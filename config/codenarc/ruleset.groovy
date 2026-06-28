ruleset {
    description 'Consolidated CodeNarc ruleset for all Matrix modules'

    // Basic rules
    ruleset('rulesets/basic.xml') {
        exclude 'EmptyCatchBlock'
        exclude 'EmptyIfStatement'
        exclude 'EmptyElseBlock'
    }

    // Braces rules
    ruleset('rulesets/braces.xml')

    // Design rules
    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod'
        exclude 'BuilderMethodWithSideEffects'
        exclude 'CloseWithoutCloseable'
        exclude 'Instanceof'
        exclude 'NestedForLoop'
        exclude 'PrivateFieldCouldBeFinal'
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
        exclude 'CatchException'
        exclude 'CatchThrowable'
    }

    // Imports
    ruleset('rulesets/imports.xml') {
        exclude 'NoWildcardImports'
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
        exclude 'CrapMetric'
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
            // NumberExtension is an intentional utility accumulator for numeric extension methods
            maxLines = 1200
        }
        'ParameterCount' {
            maxParameters = 7
        }
    }

    // Unnecessary code
    ruleset('rulesets/unnecessary.xml') {
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessarySetter'
        exclude 'UnnecessaryPublicModifier'
        exclude 'UnnecessaryReturnKeyword'
    }

    // Unused code
    ruleset('rulesets/unused.xml') {
        exclude 'UnusedVariable'
    }

    // Formatting — disabled in favor of Spotless
    ruleset('rulesets/formatting.xml') {
        exclude 'BlockEndsWithBlankLine'
        exclude 'BlockStartsWithBlankLine'
        exclude 'ClassEndsWithBlankLine'
        exclude 'ClassStartsWithBlankLine'
        exclude 'ConsecutiveBlankLines'
        exclude 'Indentation'
        exclude 'LineLength'
        exclude 'SpaceAfterComma'
        exclude 'SpaceAfterMethodCallName'
        exclude 'SpaceAfterOpeningBrace'
        exclude 'SpaceAroundMapEntryColon'
        exclude 'SpaceAroundOperator'
        exclude 'SpaceBeforeClosingBrace'
        exclude 'SpaceInsideParentheses'
    }

    // Comments
    ruleset('rulesets/comments.xml') {
        'ClassJavadoc' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        exclude 'SpaceAfterCommentDelimiter'
    }

    // Groovy-specific rules
    ruleset('rulesets/groovyism.xml')
}
