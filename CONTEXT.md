# PlantFlow Domain Model

Coloured Petri Net (CPN) engine generated from PlantUML activity diagrams to execute, trace, and verify state transitions.

## Language

**Activity Diagram**:
A workflow specification written in PlantUML syntax defining sequential actions, decision branches, and execution boundaries.
_Avoid_: Flowchart, process diagram

**Place**:
A named state container in the net that holds a collection of coloured tokens.
_Avoid_: State node, queue, buffer

**Start Place**:
The dedicated initial Place created from the `start` node in a diagram, pre-populated with an initial seed token.
_Avoid_: Entry node, root place

**End Place**:
The dedicated sink Place created from the `end` node in a diagram with no outgoing arcs.
_Avoid_: Exit node, terminal state

**Decision Place**:
A Place preceding a conditional branch where a token awaits guard evaluation by competing branch transitions.
_Avoid_: Condition place, choice node, fork place

**Branch Place**:
An intermediate Place holding tokens routed along an active conditional branch (`then` or `else`) prior to executing branch actions.
_Avoid_: Path place, conditional buffer

**Merge Place**:
The Place representing the convergence point (`endif`) where conditional branch paths rejoin before continuing execution.
_Avoid_: Join place, endif place, synchronization place

**Transition**:
An executable step in the net that consumes tokens from input places, optionally evaluates guard conditions or executes registered action handlers, and produces tokens to output places.
_Avoid_: Event, task, command

**Action Transition**:
A Transition that executes a registered Action Handler when fired to transform tokens.
_Avoid_: Task transition, operation step

**Branch Transition**:
A structural Transition carrying a Guard condition that routes tokens from a Decision Place to a Branch Place without executing an action handler.
_Avoid_: Decision transition, conditional arc, gate transition

**Structural Transition**:
A Transition that routes tokens across diagram control structures without executing an action handler.
_Avoid_: Control transition, dummy transition, routing step

**Arc**:
A directed connection between a Place and a Transition (or Transition and Place) represented in the incidence matrix.
_Avoid_: Edge, link, arrow

**Incidence Matrix**:
The algebraic matrix representation encoding input and output arc relations and weights between Places and Transitions.
_Avoid_: Graph topology table, connectivity matrix

**Token**:
An immutable record value carrying payload data, unique identifier, and timestamp attributes residing in Places.
_Avoid_: Item, message, context map

**Marking**:
The complete distribution and assignment of Tokens across all Places in the net, indexed by matrix place positions.
_Avoid_: Global state, snapshot

**Execution Context**:
A mutable context object passed into Guard and Action Handlers to read or update workflow variables.
_Avoid_: Environment, session state

**Enabled Transition**:
A Transition whose input places hold required tokens and whose guard condition evaluates to true with the current token and execution context.
_Avoid_: Active transition, runnable step

**Firing**:
The atomic step of consuming input tokens from input places according to the incidence matrix, executing any registered action handler, and placing resulting tokens into output places.
_Avoid_: Execution, dispatch, step run

**Guard**:
A boolean condition bound via the Handler Registry that must evaluate to true for input tokens before a Transition is enabled.
_Avoid_: Precondition, check, filter

**Action Handler**:
A registered function executed upon transition firing that consumes input token data and produces transformed token data for output places.
_Avoid_: Job, task callback, processor

**Handler Registry**:
The central registry mapping diagram expression strings and labels to executable Guard predicates and Action handlers.
_Avoid_: Script engine, dispatcher

**Activity Diagram Parser**:
The top-level parser and facade that transforms PlantUML activity diagram syntax into an executable Petri Net structure.
_Avoid_: Diagram importer, workflow parser
