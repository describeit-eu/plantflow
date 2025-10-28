package eu.describeit.plantflow.block

import eu.describeit.plantflow.PlantFlowAction
import spock.lang.Specification

class BlockNodeTest extends Specification {

  def "getId concatenates type and idx"() {
    given:
    def node = new BlockNode(type: BlockType.ROOT_BLOCK, idx: 2)

    expect:
    node.id == 'ROOT_BLOCK2'
  }

  def "addChildren appends the child to children list"() {
    given:
    def parent = new BlockNode(type: BlockType.ROOT_BLOCK)
    def child  = new BlockNode(type: BlockType.LOOP, idx: 1)

    when:
    parent.addChildren(child)

    then:
    parent.children.size() == 1
    parent.children[0].is(child)
  }

  def "addAction creates an ACTION child with the given name"() {
    given:
    def parent = new BlockNode(type: BlockType.ROOT_BLOCK)

    when:
    parent.addAction('doIt')

    then:
    parent.children.size() == 1
    parent.children[0].type == BlockType.ACTION
    parent.children[0].name == 'doIt'
  }

  def "find(String id) finds nested node by id (via first branch recursion)"() {
    given:
    def root = new BlockNode(type: BlockType.ROOT_BLOCK)
    def loop = new BlockNode(type: BlockType.LOOP, idx: 1)
    def target = new BlockNode(type: BlockType.IF_BLOCK, idx: 5)
    loop.addChildren(target)
    root.addChildren(loop)
    root.addChildren(new BlockNode(type: BlockType.ELSE_BLOCK, idx: 2))

    expect:
    root.find('IF_BLOCK5').is(target)
  }

  def "find(BlockType) returns all nodes of the given type recursively"() {
    given:
    def root = new BlockNode(type: BlockType.ROOT_BLOCK)
    def fork = new BlockNode(type: BlockType.FORK, idx: 7)
    def loop = new BlockNode(type: BlockType.LOOP, idx: 3)
    def inLoop1 = new BlockNode(type: BlockType.FORK_BLOCK, idx: 4)
    def inLoop2 = new BlockNode(type: BlockType.FORK_BLOCK, idx: 5)

    loop.addChildren(inLoop1)
    loop.addChildren(inLoop2)
    fork.addChildren(loop)
    root.addChildren(fork)

    when:
    def forks = root.find(BlockType.FORK_BLOCK)

    then:
    forks*.type.unique() == [BlockType.FORK_BLOCK]
    forks.size() == 2
    forks.every { it.id in ['FORK_BLOCK4', 'FORK_BLOCK5'] }
  }

  def "isFinished is true only if all non-ACTION children have empty nextActions"() {
    given:
    BlockNode parent = new BlockNode(type: BlockType.ROOT_BLOCK)
    def nonActionEmpty = new BlockNode(type: BlockType.LOOP, idx: 1)
    def nonActionHasNext = new BlockNode(type: BlockType.CONDITIONAL, idx: 2)
    def actionChild = new BlockNode(type: BlockType.ACTION, name: 'act')

    // nextActions empty for first non-action
    nonActionEmpty.nextActions = []

    // nextActions non-empty for second non-action
    PlantFlowAction someAction = [getName: { 'x' }, activate: { true }] as PlantFlowAction
    nonActionHasNext.nextActions = [someAction]

    and:
    parent.addChildren(actionChild)
    parent.addChildren(nonActionEmpty)
    parent.addChildren(nonActionHasNext)

    expect:
    !parent.isFinished()

    when:
    nonActionHasNext.nextActions.clear()

    then:
    parent.isFinished()
  }
}
