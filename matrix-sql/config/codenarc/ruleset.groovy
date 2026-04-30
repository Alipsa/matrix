ruleset {
    description 'CodeNarc ruleset for Matrix SQL'

    ruleset('rulesets/basic.xml') {
        exclude 'EmptyCatchBlock'
        exclude 'EmptyIfStatement'
        exclude 'EmptyElseBlock'
    }

    ruleset('rulesets/braces.xml')

    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod'
        exclude 'BuilderMethodWithSideEffects'
        exclude 'Instanceof'
    }

    ruleset('rulesets/dry.xml') {
        'DuplicateNumberLiteral' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'DuplicateStringLiteral' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
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
            maxLines = 5000
        }
        'ParameterCount' {
            maxParameters = 7
        }
    }

    ruleset('rulesets/unnecessary.xml') {
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessarySetter'
        exclude 'UnnecessaryReturnKeyword'
        exclude 'UnnecessaryPublicModifier'
    }

    ruleset('rulesets/unused.xml') {
        exclude 'UnusedVariable'
    }

    ruleset('rulesets/formatting.xml') {
        exclude 'SpaceAroundMapEntryColon'
        exclude 'LineLength'
        exclude 'Indentation'
    }

    ruleset('rulesets/comments.xml') {
        'ClassJavadoc' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
    }

    ruleset('rulesets/groovyism.xml') {
        exclude 'GetterMethodCouldBeProperty'
    }
}
