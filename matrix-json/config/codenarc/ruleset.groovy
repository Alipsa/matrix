ruleset {
    description 'CodeNarc ruleset for Matrix JSON'

    ruleset('rulesets/basic.xml') {
    }

    ruleset('rulesets/braces.xml')

    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod'
        exclude 'BuilderMethodWithSideEffects'
        exclude 'Instanceof'
        exclude 'NestedForLoop'
        exclude 'PrivateFieldCouldBeFinal'
    }

    ruleset('rulesets/dry.xml') {
        exclude 'DuplicateListLiteral'
        exclude 'DuplicateNumberLiteral'
        exclude 'DuplicateStringLiteral'
    }

    ruleset('rulesets/exceptions.xml') {
        exclude 'CatchException'
        exclude 'CatchThrowable'
    }

    ruleset('rulesets/imports.xml') {
        exclude 'NoWildcardImports'
    }

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

    ruleset('rulesets/size.xml') {
        exclude 'CrapMetric'
        'AbcMetric' {
            maxMethodAbcScore = 35
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'CyclomaticComplexity' {
            maxMethodComplexity = 20
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

    ruleset('rulesets/unnecessary.xml') {
        exclude 'UnnecessaryCollectCall'
        exclude 'UnnecessaryElseStatement'
        exclude 'UnnecessaryGString'
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessaryObjectReferences'
        exclude 'UnnecessaryPublicModifier'
        exclude 'UnnecessarySetter'
    }

    ruleset('rulesets/unused.xml')

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

    ruleset('rulesets/comments.xml') {
        'ClassJavadoc' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        exclude 'SpaceAfterCommentDelimiter'
    }

    ruleset('rulesets/groovyism.xml')
}
