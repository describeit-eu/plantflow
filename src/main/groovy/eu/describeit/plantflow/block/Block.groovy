package eu.describeit.plantflow.block

import com.fasterxml.jackson.annotation.JsonIgnore
import eu.describeit.plantflow.PlantFlowAction
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.block.BlockType.ACTION
import static eu.describeit.plantflow.block.BlockType.FORK

@Slf4j
@CompileStatic
@ToString(includePackage=false, includes="id")
class Block {

  BlockType type
  int idx = 0
  String name = null
  List<Block> children = []

  @JsonIgnore
  List<PlantFlowAction> nextActions = []

  @JsonIgnore
  String getId() {
    return type.toString() + idx
  }
  
  void initialise() {
    nextActions.clear()
  }

  void addChildren(Block child) {
    children.add(child)
  }

  void addAction(String name) {
    children.add(new Block(type: ACTION, name: name))
  }

  void addNextAction(PlantFlowAction action) {
    log.info('addNextAction() - name:{} in {}', action.name, this)
    assert type != FORK && nextActions.empty, "$this cannot have more than one next Action"

    nextActions.add(action)
  }

  Block find(String blockId) {
    if (this.id == blockId) return this

    Block foundChild = children.find { Block node -> node.id == blockId }

    if (foundChild) {
      return foundChild
    } else {
      for (Block subChild : children) {
        foundChild = subChild.find(blockId)
        if (foundChild) return foundChild
      }
    }
    return null
  }

  List<Block> find(BlockType type) {
    List<Block> nodes = children.findAll { Block node -> node.type == type }

    for (Block subChild : children) nodes.addAll(subChild.find(type))

    return nodes
  }
  
  Boolean notFinished() {
    return !isFinished()
  }

  @JsonIgnore
  Boolean isFinished() {
    boolean childrenStatus = children.every { Block child ->
      child.type == ACTION ? true : child.nextActions.empty
    }

    return children.empty && childrenStatus
  }
}
