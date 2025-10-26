package eu.describeit.plantflow.block

import com.fasterxml.jackson.annotation.JsonIgnore
import eu.describeit.plantflow.PlantFlowAction
import groovy.transform.CompileStatic
import groovy.transform.ToString

import static eu.describeit.plantflow.block.BlockType.ACTION

@CompileStatic
@ToString(includePackage=false, includes="id")
class BlockNode {

  BlockType type
  int idx = 0
  String name = null
  final List<BlockNode> children = []

  @JsonIgnore
  List<PlantFlowAction> nextActions = []

  @JsonIgnore
  String getId() {
    return type.toString() + idx
  }

  void addChildren(BlockNode child) {
    children.add(child)
  }

  void addAction(String name) {
    children.add(new BlockNode(type: ACTION, name: name))
  }

  BlockNode find(String id) {
    BlockNode child= children.find { BlockNode node -> node.id == id }

    if (!child) {
      for (BlockNode subChild : children) return subChild.find(id)
    } else {
      return child
    }
  }

  @JsonIgnore
  Boolean isFinished() {
    children.every { BlockNode child ->
      if (child.type != ACTION) return child.nextActions.empty
      else                      return true
    }
  }
}
