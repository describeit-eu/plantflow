plugins {
  groovy
  jacoco
  codenarc
  id("name.remal.jacoco-to-cobertura") version "2.0.4"
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

val groovyVersion  = "5.1.2"
val slf4jVersion   = "2.0.19"
val logbackVersion = "1.6.3"
val junitVersion   = "6.1.3"
val spockVersion   = "2.4-groovy-5.0"
val jacksonVersion = "2.22.2"

group = "eu.describeit"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

codenarc {
  toolVersion = "4.0.0"
  configFile = file("${rootProject.projectDir}/config/codenarc/rules.groovy")
  reportFormat = "console"
  isIgnoreFailures = false
  maxPriority1Violations = 0
  sourceSets = listOf()
}

tasks.codenarcTest {
  configFile = file("${rootProject.projectDir}/config/codenarc/test-rules.groovy")
}

dependencies {
  implementation("org.apache.groovy:groovy:$groovyVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

  // Logging dependencies
  implementation("org.slf4j:slf4j-api:${slf4jVersion}")
  implementation("ch.qos.logback:logback-classic:${logbackVersion}")

  // Test dependencies
  testImplementation(platform("org.junit:junit-bom:$junitVersion"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation(platform("org.spockframework:spock-bom:$spockVersion"))
  testImplementation("org.spockframework:spock-core")
}

tasks.test {
  useJUnitPlatform()
  // Ensure tests run before generating the report, and conversion runs after
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  reports {
    xml.required.set(true) // Required for the conversion
    html.required.set(true)
  }
  finalizedBy(tasks.jacocoTestReportToCobertura)
}
