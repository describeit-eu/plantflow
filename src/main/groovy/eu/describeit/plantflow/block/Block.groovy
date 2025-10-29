package eu.describeit.plantflow.block

import com.fasterxml.jackson.annotation.JsonIgnore
import eu.describeit.plantflow.PlantFlowAction
import groovy.transform.CompileStatic
import groovy.transform.ToString

import static eu.describeit.plantflow.block.BlockType.ACTION

@CompileStatic
@ToString(includePackage=false, includes="id")
class Block {

  BlockType type
  int idx = 0
  String name = null
  final List<Block> children = []

  @JsonIgnore
  List<PlantFlowAction> nextActions = []

  @JsonIgnore
  String getId() {
    return type.toString() + idx
  }

  void addChildren(Block child) {
    children.add(child)
  }

  void addAction(String name) {
    children.add(new Block(type: ACTION, name: name))
  }

  Block find(String id) {
    Block child= children.find { Block node -> node.id == id }

    if (!child) {
      for (Block subChild : children) return subChild.find(id)
    } else {
      return child
    }
  }

  List<Block> find(BlockType type) {
    List<Block> nodes = children.findAll { Block node -> node.type == type }

    for (Block subChild : children) nodes.addAll(subChild.find(type))

    return nodes
  }

  @JsonIgnore
  Boolean isFinished() {
    children.every { Block child ->
      if (child.type != ACTION) return child.nextActions.empty
      else                      return true
    }
  }
}
