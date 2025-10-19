package eu.describeit.plantflow.converter

import com.fasterxml.jackson.databind.ObjectMapper
import eu.describeit.plantflow.block.BlockNode
import eu.describeit.plantflow.block.BlockType
import spock.lang.Specification

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson

class NestedPlantFlowTest extends Specification {
  String puml = """
      @startuml
      start
      :before loop;
      while (count < 3)
        :before if;
        if (ready) then (go)
          :step A1;
          :step A2;
        else (wait)
          :step B1;
          :step B2;
        endif
        :middle loop;
        fork
          :parallel A1;
          :parallel A2;
        fork again
          :parallel B1;
          :parallel B2;
        end merge
        :after fork;
      endwhile
      :after loop;
      stop
      @enduml
    """.stripIndent().trim()

  def "builds ContextTree JSON for nested flow"() {
    given:
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
          "type": "ACTION",
          "idx": 0,
          "name": "before loop",
          "children": []
        },
        {
          "type": "LOOP",
          "idx": 1,
          "name": null,
          "children": [
            {
              "type": "ACTION",
              "idx": 0,
              "name": "before if",
              "children": []
            },
            {
              "type": "CONDITIONAL",
              "idx": 2,
              "name": null,
              "children": [
                { "type": "ACTION", "idx": 0, "name": "step A1", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "step A2", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "step B1", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "step B2", "children": [] }
              ]
            },
            {
              "type": "ACTION",
              "idx": 0,
              "name": "middle loop",
              "children": []
            },
            {
              "type": "FORK",
              "idx": 3,
              "name": null,
              "children": [
                { "type": "ACTION", "idx": 0, "name": "parallel A1", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "parallel A2", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "parallel B1", "children": [] },
                { "type": "ACTION", "idx": 0, "name": "parallel B2", "children": [] }
              ]
            },
            {
              "type": "ACTION",
              "idx": 0,
              "name": "after fork",
              "children": []
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
    blockTree.children.size() == 3
    blockTree.children[0].type == BlockType.ACTION
    blockTree.children[0].name == 'before loop'
    blockTree.children[0].children.size() == 0
    blockTree.children[1].type == BlockType.LOOP
    blockTree.children[1].children.size() == 5
    blockTree.children[1].children[0].type == BlockType.ACTION
    blockTree.children[1].children[0].name == 'before if'
    blockTree.children[1].children[1].type == BlockType.CONDITIONAL
    blockTree.children[1].children[2].type == BlockType.ACTION
    blockTree.children[1].children[2].name == 'middle loop'
    blockTree.children[1].children[3].type == BlockType.FORK
    blockTree.children[1].children[4].type == BlockType.ACTION
    blockTree.children[1].children[4].name == 'after fork'
    blockTree.children[2].type == BlockType.ACTION
    blockTree.children[2].name == 'after loop'
    blockTree.children[2].children.size() == 0
  }
}
