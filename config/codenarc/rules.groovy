ruleset {
  description 'A CodeNarc ruleset strictly optimized for Clean Code principles'

  // all rulesets are listed bellow
  ruleset('rulesets/basic.xml')
//  ruleset('rulesets/braces.xml')
//  ruleset('rulesets/comments.xml')
//  ruleset('rulesets/concurrency.xml')
  ruleset('rulesets/convention.xml') {
    exclude 'FieldTypeRequired'
    exclude 'ImplicitReturnStatement'
    exclude 'NoDef'
  }
  ruleset('rulesets/design.xml') {
    exclude 'NestedForLoop'
  }
  ruleset('rulesets/dry.xml') {
    DuplicateStringLiteral {
      ignoreStrings = 'true,false'
    }
    DuplicateNumberLiteral {
      ignoreNumbers = '0,1,-1'
    }
  }
//  ruleset('rulesets/enhanced.xml')
  ruleset('rulesets/exceptions.xml')
//  ruleset('rulesets/formatting.xml')
//  ruleset('rulesets/generic.xml')
//  ruleset('rulesets/grails.xml')
  ruleset('rulesets/groovyism.xml')
  ruleset('rulesets/imports.xml') {
    NoWildcardImports {
      ignoreStaticImports = true
    }
  }
//  ruleset('rulesets/jdbc.xml')
//  ruleset('rulesets/junit.xml')
//  ruleset('rulesets/jenkins.xml')
  ruleset('rulesets/logging.xml')
  ruleset('rulesets/naming.xml')
  ruleset('rulesets/size.xml') {
    ClassSize {
      maxLines = 300
    }
    CrapMetric {
      coberturaXmlFile = 'build/reports/jacoco/test/cobertura-jacocoTestReport.xml'
      maxMethodCrapScore = 8
    }
    CyclomaticComplexity {
      maxMethodComplexity = 10
      maxClassAverageMethodComplexity = 10
    }
    MethodSize {
      maxLines = 40
    }
    ParameterCount {
      maxParameters = 5
    }
  }
  ruleset('rulesets/security.xml') {
    exclude 'SystemExit'
  }
  ruleset('rulesets/serialization.xml') {
    // enhanced classpath rule creating many false positives
    exclude 'NonSerializableFieldInSerializableClass'
  }
  ruleset('rulesets/unnecessary.xml') {
    exclude 'UnnecessaryReturnKeyword'
  }
  ruleset('rulesets/unused.xml')
}
