package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

class ScenarioConverterTest extends Specification {

  @Unroll
  def "convert complete puml resource file: #fileName"() {
    given:
    def puml = PlantUmlConverter.getResourceText("${fileName}.puml")
    def expectedPflow = PlantUmlConverter.getResourceText("${fileName}.pflow")

    when:
    def resultPflow = PlantUmlConverter.convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    where:
//    fileName << ['whileEndwhile']
    fileName << ['sequence','ifThenElseEndif','ifIsThenEndif','forkEndMerge','whileInfinite','whileEndwhile','repeatWhile','crud']//'switchCaseEndswitch'
  }
}
