plugins {
  id("groovy")
}

val groovyVersion    = "5.1.0"
val slf4jVersion     = "2.0.18"
val logbackVersion   = "1.6.3"

val junitVersion     = "6.1.3"
val spockVersion     = "2.4-groovy-5.0"
//val bytebuddyVersion = "1.18.12"
//val mockitoVersion   = "5.23.0"
val jsonUnitVersion  = "6.2.0"

group = "eu.describeit"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.groovy:groovy:$groovyVersion")
  implementation("org.apache.groovy:groovy-json:$groovyVersion")
  implementation("org.apache.groovy:groovy-templates:$groovyVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")

  // Logging dependencies
  implementation("org.slf4j:slf4j-api:${slf4jVersion}")
  implementation("ch.qos.logback:logback-classic:${logbackVersion}")

  // Commons dependencies
  implementation("org.apache.commons:commons-lang3:3.20.0")
  implementation("org.apache.commons:commons-text:1.15.0")
    
  // Test dependencies
  testImplementation(platform("org.junit:junit-bom:$junitVersion"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation(platform("org.spockframework:spock-bom:$spockVersion"))
  testImplementation("org.spockframework:spock-core")
//  testImplementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")
//  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation("net.javacrumbs.json-unit:json-unit:$jsonUnitVersion")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:$jsonUnitVersion")
}

tasks.test {
  useJUnitPlatform()
}
