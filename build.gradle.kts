plugins {
  id("groovy")
}

val groovyVersion    = "4.0.28"
val slf4jVersion     = "2.0.17"
val logbackVersion   = "1.5.18"

val junitVersion     = "5.13.4"
val spockVersion     = "2.4-M6-groovy-4.0"
//val bytebuddyVersion = "1.17.7"
//val mockitoVersion   = "5.20.0"
val jsonUnitVersion  = "5.0.0"
val orgJsonVersion   = "20250517"

group = "eu.describeit"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.groovy:groovy:$groovyVersion")
  implementation("org.apache.groovy:groovy-json:$groovyVersion")

  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

  // Logging dependencies
  implementation("org.slf4j:slf4j-api:${slf4jVersion}")
  implementation("ch.qos.logback:logback-classic:${logbackVersion}")

  implementation("org.apache.commons:commons-lang3:3.18.0")
  implementation("org.apache.commons:commons-text:1.14.0")

  testImplementation(platform("org.junit:junit-bom:$junitVersion"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation(platform("org.spockframework:spock-bom:$spockVersion"))
  testImplementation("org.spockframework:spock-core")
//  testImplementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")
//  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation("net.javacrumbs.json-unit:json-unit:$jsonUnitVersion")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:$jsonUnitVersion")
  testImplementation("org.json:json:$orgJsonVersion")
}

tasks.test {
  useJUnitPlatform()
}
