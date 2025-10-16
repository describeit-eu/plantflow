package eu.describeit.plantflow.block


import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includePackage=false, includes="id")
class BlockNode {

  BlockType type
  int idx = 0

  final List<BlockNode> children = []
  final List<String> actions = []

  @JsonIgnore
  String getId() {
    return type.toString() + idx
  }

  void addChildren(BlockNode child) {
    children.add(child)
  }

  BlockNode find(String id) {
    def child= children.find { it.id == id }

    if (!child) {
      for (def subChild : children) return subChild.find(id)
    } else {
      return child
    }
  }
}
