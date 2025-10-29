package eu.describeit.plantflow.calculate.engine

import eu.describeit.plantflow.block.Block
import groovy.transform.CompileStatic

@CompileStatic
class StopCalculateNext extends RuntimeException {
  Block stoppingBlock

  StopCalculateNext(String action, Block block) {
    super("Action '$action' in $block")
    stoppingBlock = block
  }

  StopCalculateNext(Block block) {
    super("Stopping block:$block")
    stoppingBlock = block
  }
}
