package eu.describeit.plantflow.converter

import eu.describeit.plantflow.Utility
import spock.lang.Specification
import spock.lang.Unroll

class ScenarioConverterTest extends Specification {

  @Unroll
  def "convert complete puml resource file: #fileName"() {
    given:
    def puml = Utility.getResourceText("${fileName}.puml")
    def expectedPflow = Utility.getResourceText("${fileName}.pflow")

    when:
    def resultPflow = new PlantUmlConverter().convertToPlantFlowDsl(puml)

    then:
    resultPflow.contains(expectedPflow)

    where:
    fileName << ['sequence','ifThenElseEndif','ifIsThenEndif','ifEqualsThenEndif','forkEndMerge','whileInfinite','whileEndwhile','repeatWhile','crud']//'switchCaseEndswitch'
  }
}
