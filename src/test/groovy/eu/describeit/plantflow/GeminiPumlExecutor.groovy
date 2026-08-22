package eu.describeit.plantflow

class WorkflowContext {
  Map data = [:]
}

interface WorkflowNode {
  void execute(WorkflowContext ctx, Map registry)
}

// Represents a standalone task
class TaskNode implements WorkflowNode {
  String taskName

  void execute(WorkflowContext ctx, Map registry) {
    println "Processing Task: [${taskName}]"
    if (registry[taskName]) {
      registry[taskName](ctx) // Execute the registered Closure
    } else {
      println "  Warning: No implementation found for task '${taskName}'"
    }
  }
}

// Represents an If/Else structural routing
class ConditionalNode implements WorkflowNode {
  String conditionKey
  List<WfNode> trueBranch = []
  List<WfNode> falseBranch = []

  void execute(WorkflowContext ctx, Map registry) {
    println "Evaluating Condition: Does '${conditionKey}' evaluate to true?"
    // Simple resolution: look up the key in the workflow context data
    Boolean result = ctx.data[conditionKey] == true
    println "  Condition result: ${result}"

    def branchToExecute = result ? trueBranch : falseBranch
    branchToExecute.each { node -> node.execute(ctx, registry) }
  }
}

class PlantUmlEngine {
  // Basic parser that looks for tasks and if/else configurations
  List<WfNode> parse(String plantUmlText) {
    List<WfNode> nodes = []
    List<String> lines = plantUmlText.readLines().collect { it.trim() }

    int i = 0
    while (i < lines.size()) {
      String line = lines[i]

      if (line.startsWith(":") && line.endsWith(";")) {
        // Extract task name: :Validate Order; -> Validate Order
        String taskName = line.substring(1, line.length() - 1)
        nodes << new TaskNode(taskName: taskName)
      }
      else if (line.startsWith("if") && line.contains("then")) {
        // Extract condition key from syntax: if (Stock Available?) then (yes)
        def condMatcher = (line =~ /\(([^)]+)\)/)
        String conditionKey = condMatcher ? condMatcher[0][1] : "defaultCond"

        ConditionalNode condNode = new ConditionalNode(conditionKey: conditionKey)

        // Parse True Branch
        i++
        while (i < lines.size() && !lines[i].startsWith("else")) {
          if (lines[i].startsWith(":") && lines[i].endsWith(";")) {
            String tName = lines[i].substring(1, lines[i].length() - 1)
            condNode.trueBranch << new TaskNode(taskName: tName)
          }
          i++
        }

        // Parse False Branch (if else exists)
        if (i < lines.size() && lines[i].startsWith("else")) {
          i++
          while (i < lines.size() && !lines[i].startsWith("endif")) {
            if (lines[i].startsWith(":") && lines[i].endsWith(";")) {
              String tName = lines[i].substring(1, lines[i].length() - 1)
              condNode.falseBranch << new TaskNode(taskName: tName)
            }
            i++
          }
        }
        nodes << condNode
      }
      i++
    }
    return nodes
  }
}

// ==========================================
// RUNTIME DEMO
// ==========================================

// 1. Define the workflow using script text
def puml = """
@startuml
start
:ValidateOrder;
if (isStockAvailable) then (yes)
  :ChargeCreditCard;
  :ShipOrder;
else (no)
  :NotifyCustomer;
endif
stop
@enduml
"""

// 2. Register functional implementations for our tasks using Groovy Closures
def serviceRegistry = [
    "ValidateOrder"   : { ctx -> ctx.data['isStockAvailable'] = true; println "  -> Order Validated successfully." },
    "ChargeCreditCard": { ctx -> println "  -> Credit card charged successfully." },
    "ShipOrder"       : { ctx -> println "  -> Items handed over to shipping." },
    "NotifyCustomer"  : { ctx -> println "  -> Out of stock notification email sent." }
]

// 3. Initialize engine and run a simulation
def engine = new PlantUmlEngine()
def executionPlan = engine.parse(puml)

println "--- Starting Workflow Execution ---"
def context = new WorkflowContext()
executionPlan.each { node ->
  node.execute(context, serviceRegistry)
}
println "--- Workflow Execution Finished ---"
