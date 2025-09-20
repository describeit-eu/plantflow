plugins {
  id("groovy")
}

val groovyVersion    = "4.0.28"
val slf4jVersion     = "2.0.17"
val logbackVersion   = "1.5.18"

val junitVersion = "5.13.4"

group = "eu.describeit"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.groovy:groovy:$groovyVersion")

  // Logging dependencies
  implementation("org.slf4j:slf4j-api:${slf4jVersion}")
  implementation("ch.qos.logback:logback-classic:${logbackVersion}")

  implementation("org.apache.commons:commons-lang3:3.18.0")
  implementation("org.apache.commons:commons-text:1.14.0")

  testImplementation(platform("org.junit:junit-bom:$junitVersion"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}
