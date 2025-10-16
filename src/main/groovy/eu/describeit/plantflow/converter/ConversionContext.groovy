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
  final BlockNode rootBlock = new BlockNode(type: SEQ, idx: 0)
  int counter = 1

  void start(BlockType type) {
    BlockNode currentBlock = blockStack.empty() ? rootBlock : blockStack.peek()

    BlockNode newBlock = new BlockNode(type: type, idx: counter++)
    currentBlock.addChildren(newBlock)
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
    if (contextId) assert blockStack.last.id == contextId

    if (blockStack) blockStack.peek().addAction(name)
    else            rootBlock.addAction(name)
  }

  @Override
  String toString() {
    return toJson(false)
  }

  String toJson(boolean pretty = true) {
    ObjectMapper mapper = new ObjectMapper()

    if (pretty) mapper.enable(INDENT_OUTPUT)

    return mapper.writeValueAsString(rootBlock)
  }
}
