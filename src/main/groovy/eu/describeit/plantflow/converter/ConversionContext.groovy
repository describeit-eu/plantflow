package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@Slf4j
@ToString(includePackage=false)
@CompileStatic
class ConversionContext {
  Stack<ConversionBlock> blocks = new Stack<>()
  List<String> actionNames = []
  int counter = 0

  void start(ConversionBlock.Type type) {
    ConversionBlock newBlock = blocks.push(new ConversionBlock(type: type, idx: counter++))
    log.info('start() - new block:{}', newBlock)
  }

  void check(ConversionBlock.Type type) {
    assert blocks.last.type == type
  }

  void end(ConversionBlock.Type type) {
    check(type)

    ConversionBlock lastBlock = blocks.pop()
    log.info('end() - last block:{}', lastBlock)
  }

  String geCurrentId() {
    return blocks ? blocks.last?.id : null
  }

  void addAction(String contextId, String name) {
    if (contextId) assert blocks.last.id == contextId
    actionNames.add(name)
  }
}
