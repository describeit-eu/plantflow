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
    converter.convertToPlantFlowDsl(puml)
    String actualJson = converter.context.toJson()

    then:
    String expectedJson = '''
    {
      "type": "SEQ",
      "idx": 0,
      "name": null,
      "children": [
        {
          "type": "LOOP",
          "idx": 1,
          "name": null,
          "children": [
            {
              "type": "CONDITIONAL",
              "idx": 2,
              "name": null,
              "children": [
                { "type": "ACTION", "idx": 0, "name": "step A", "children": []},
                { "type": "ACTION", "idx": 0, "name": "step B", "children": [] }
              ]
            },
            {
              "type": "FORK",
              "idx": 3,
              "name": null,
              "children": [
                { "type": "ACTION", "idx": 0, "name": "parallel 1", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "parallel 2", "children": [] }
              ]
            }
          ]
        },
        {
          "type": "ACTION",
          "idx": 0,
          "name": "after loop",
          "children": []
        }
      ]
    }
    '''.stripIndent().trim()

    assertThatJson(actualJson).isEqualTo(expectedJson)

    when:
    def blockTree = new ObjectMapper().readValue(actualJson, BlockNode)

    then:
    blockTree.type == BlockType.SEQ
    blockTree.idx == 0
    blockTree.children.size() == 2
    blockTree.children[0].type == BlockType.LOOP
    blockTree.children[0].children.size() == 2
    blockTree.children[0].children[0].type == BlockType.CONDITIONAL
    blockTree.children[0].children[1].type == BlockType.FORK
    blockTree.children[1].type == BlockType.ACTION
    blockTree.children[1].name == 'after loop'
    blockTree.children[1].children.size() == 0
  }
}
