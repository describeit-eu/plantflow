package eu.describeit.plantflow.converter

import com.fasterxml.jackson.databind.ObjectMapper
import eu.describeit.plantflow.block.BlockNode
import eu.describeit.plantflow.block.BlockType
import spock.lang.Specification

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class ContextTreeJsonTest extends Specification {

  def "builds ContextTree JSON for nested flow"() {
    given:
    String puml = """
      @startuml
      start
      while (count < 3)
        if (ready) then (go)
          :step A;
        else (wait)
          :step B;
        endif
        fork
          :parallel 1;
        fork again
          :parallel 2;
        end merge
      endwhile
      :after loop;
      stop
      @enduml
    """.stripIndent().trim()

    def converter = new PlantUmlConverter()

    when:
    String actualPflow = converter.convertToPlantFlowDsl(puml)
    println actualPflow
    String actualJson = converter.context.toJson()

    then:
    String expectedJson = '''
    {
      "idx": 0,
      "type": "SEQ",
      "children": [
        {
          "idx": 1,
          "type": "LOOP",
          "children": [
            {
              "idx": 2,
              "type": "CONDITIONAL",
              "children": [],
              "actions": ["step A", "step B"]
            },
            {
              "idx": 3,
              "type": "FORK",
              "children": [],
              "actions": ["parallel 1","parallel 2"]
            }
          ],
          "actions": []
        }
      ],
      "actions": ["after loop"]
    }
    '''.stripIndent().trim()

    assertThatJson(actualJson).isEqualTo(expectedJson)

    when:
    def blockTree = new ObjectMapper().readValue(actualJson, BlockNode)
    then:
    blockTree.type == BlockType.SEQ
    blockTree.idx == 0
  }
}
