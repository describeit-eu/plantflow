package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class ConversionContext {
  Stack<ConversionBlock> blocks = new Stack<>()
  int counter = 0

  void start(ConversionBlock.Type type) {
    def newBlock = blocks.push(new ConversionBlock(type: type, idx: counter++))
    log.info('start() - new block:{}', newBlock)
  }

  void check(ConversionBlock.Type type) {
    assert blocks.last.type == type
  }

  void end(ConversionBlock.Type type) {
    check(type)

    def lastBlock = blocks.pop()
    log.info('end() - last block:{}', lastBlock)
  }

  String getId() {
    return blocks ? blocks.last?.getId() : null
  }
}
