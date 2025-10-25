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
          :if A1;
          :if A2;
        else (wait)
          :else B1;
          :else B2;
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
      end
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
      "type": "SEQ", "idx": 0, "name": null, "children": [
        { "type": "ACTION", "idx": 0, "name": "before loop", "children": [] },
        {
          "type": "LOOP", "idx": 1, "name": null, "children": [
            { "type": "ACTION", "idx": 0, "name": "before if", "children": [] },
            { "type": "CONDITIONAL", "idx": 2, "name": null, "children": [
                {
                  "type": "IF_BLOCK", "idx": 3, "name": null, "children": [
                    { "type": "ACTION", "idx": 0, "name": "if A1", "children": [] },
                    { "type": "ACTION", "idx": 0, "name": "if A2", "children": [] }
                  ]
                },
                {
                  "type": "ELSE_BLOCK", "idx": 4, "name": null, "children": [
                    { "type": "ACTION", "idx": 0, "name": "else B1", "children": [] },
                    { "type": "ACTION", "idx": 0, "name": "else B2", "children": [] }
                  ]
                }
              ]
            },
            { "type": "ACTION","idx": 0, "name": "middle loop", "children": [] },
            {
              "type": "FORK", "idx": 5, "name": null, "children": [
                {
                  "type": "FORK_BLOCK", "idx": 6, "name": null, "children": [
                    { "type": "ACTION", "idx": 0, "name": "parallel A1", "children": [] },
                    { "type": "ACTION", "idx": 0, "name": "parallel A2", "children": [] }
                  ]
                },
                {
                  "type": "FORK_BLOCK", "idx": 7, "name": null, "children": [
                    { "type": "ACTION", "idx": 0, "name": "parallel B1", "children": [] },
                    { "type": "ACTION", "idx": 0, "name": "parallel B2", "children": [] }
                  ]
                }
              ]
            },
            { "type": "ACTION", "idx": 0, "name": "after fork", "children": [] }
          ]
        },
        { "type": "ACTION", "idx": 0, "name": "after loop", "children": [] }
      ]
    }
    '''.stripIndent().trim()

    assertThatJson(actualJson).isEqualTo(expectedJson)

    when: ""
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

  def "builds PlantFlow Script for nested flow"() {
    given:
    def converter = new PlantUmlConverter()

    when:
    String actualPflow = converter.convertToPlantFlowDsl(puml).stripIndent().trim()

    then:
    String expectedPflow = '''
    if (isActive("before loop", "SEQ0")) return
      loop("LOOP1") { while (eval("count < 3", null, "LOOP1")) {
        if (isActive("before if", "LOOP1")) return
        conditional("CONDITIONAL2") { if (eval("ready", null, "CONDITIONAL2")) { ifBlock("IF_BLOCK3") { // go
          if (isActive("if A1", "IF_BLOCK3")) return
          if (isActive("if A2", "IF_BLOCK3")) return
        } } else { elseBlock("ELSE_BLOCK4") { // wait CONDITIONAL2
          if (isActive("else B1", "ELSE_BLOCK4")) return
          if (isActive("else B2", "ELSE_BLOCK4")) return
        } } } // CONDITIONAL2
        if (isActive("middle loop", "LOOP1")) return
        fork("FORK5") { forkBlock("FORK_BLOCK6") {
          if (isActive("parallel A1", "FORK_BLOCK6")) return
          if (isActive("parallel A2", "FORK_BLOCK6")) return
        } forkBlock("FORK_BLOCK7") {
          if (isActive("parallel B1", "FORK_BLOCK7")) return
          if (isActive("parallel B2", "FORK_BLOCK7")) return
        } } // FORK5
        if (isActive("after fork", "LOOP1")) return
      } } // LOOP1
      if (isActive("after loop", "SEQ0")) return
      '''.stripIndent().trim()

    actualPflow.contains(expectedPflow)
  }
}
