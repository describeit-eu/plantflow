package eu.describeit.plantflow.converter

import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import static eu.describeit.plantflow.converter.ConversionBlock.Type.SEQ

@Slf4j
@ToString(includePackage=false)
@CompileStatic
class ConversionContext {
  final Stack<ConversionBlock> blocks = new Stack<>()
  final ConversionBlock root = new ConversionBlock(type: SEQ, idx: 0)
  int counter = 1

  void start(ConversionBlock.Type type) {
    ConversionBlock currentBlock = blocks.empty() ? root : blocks.peek()

    ConversionBlock newBlock = new ConversionBlock(type: type, idx: counter++)
    currentBlock.addChildren(newBlock)
    blocks.push(newBlock)

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

    if (blocks) blocks.peek().actions.add(name)
    else        root.actions.add(name)
  }

  @Override
  String toString() {
    return toJson(false)
  }

  String toJson(boolean pretty = true) {
    JsonGenerator generator = new JsonGenerator.Options()
        .excludeFieldsByName('id')
        .build()

    String json = generator.toJson(root)

    if (pretty) return JsonOutput.prettyPrint(json)
    else        return json
  }
}
