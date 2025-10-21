package eu.describeit.plantflow.converter

import com.fasterxml.jackson.databind.ObjectMapper
import eu.describeit.plantflow.block.BlockType
import eu.describeit.plantflow.block.BlockNode
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import static eu.describeit.plantflow.block.BlockType.SEQ

@Slf4j
@ToString(includePackage=false)
@CompileStatic
class ConversionContext {
  final Stack<BlockNode> blockStack = new Stack<>()
  int counter = 0

  void start(BlockType type) {
    BlockNode newBlock = new BlockNode(type: type, idx: counter++)

    if (blockStack) blockStack.peek().addChildren(newBlock)
    blockStack.push(newBlock)

    log.info('start() - new block:{}', newBlock)
  }

  void check(BlockType type) {
    assert blockStack.last.type == type
  }

  void end(BlockType type) {
    check(type)

    BlockNode lastBlock = blockStack.pop()
    log.info('end() - last block:{}', lastBlock)
  }

  String geCurrentId() {
    return blockStack ? blockStack.last?.id : null
  }

  void addAction(String contextId, String name) {
    if (contextId) {
      assert blockStack.last.id == contextId
      blockStack.peek().addAction(name)
    } else {
      // this case should only happen during unit test of PlantUmlConverter
      log.warn('addAction() - no contextId was provided for action:{}', name)
    }
  }

  @Override
  String toString() {
    if (blockStack) return toJson(false)
    else return null
  }

  String toJson(boolean pretty = true) {
    ObjectMapper mapper = new ObjectMapper()

    if (pretty) mapper.enable(INDENT_OUTPUT)

    return mapper.writeValueAsString(blockStack.first)
  }
}
