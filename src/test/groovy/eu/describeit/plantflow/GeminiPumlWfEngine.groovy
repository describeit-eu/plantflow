package eu.describeit.plantflow

import java.time.LocalDateTime

// The shared variables & data payload for a specific workflow instance
class WorkflowVariables {
  Map<String, Object> data = [:]
}

// Represents the global execution path tracking
enum InstanceStatus { ACTIVE, COMPLETED, SUSPENDED }
enum TaskStatus { PENDING, CLAIMED, COMPLETED }

class WorkflowInstance {
  String id = UUID.randomUUID().toString()
  String definitionId
  InstanceStatus status = InstanceStatus.ACTIVE
  String currentActivityId // The pointer/token location in the DAG
  WorkflowVariables variables = new WorkflowVariables()
}

// This is your Job List DB Table record
class UserTaskInstance {
  String id = UUID.randomUUID().toString()
  String instanceId
  String taskName
  String candidateGroup  // e.g., "Warehouse", "Approvers"
  String assignedTo      // e.g., "john_doe"
  TaskStatus status = TaskStatus.PENDING
  LocalDateTime createdAt = LocalDateTime.now()
}

class WorkflowEngine {
  // Mimicking a Database/Repository Layer
  Map<String, List<WfNode>> definitions = [:]
  List<WorkflowInstance> instances = []
  List<UserTaskInstance> taskDatabase = []

  // 1. Deploy/Parse a PlantUML process
  void deployDefinition(String definitionId, String plantUmlText) {
    List<WfNode> nodes = []
    String currentSwimlane = "System"

    plantUmlText.eachLine { rawLine ->
      String line = rawLine.trim()
      if (line.startsWith("|") && line.endsWith("|")) {
        currentSwimlane = line.substring(1, line.length() - 1)
      } else if (line.startsWith(":") && line.endsWith(";")) {
        String cleanLine = line.substring(1, line.length() - 1)
        boolean isUserTask = cleanLine.contains("<<UserTask>>")
        String taskName = cleanLine.replace("<<UserTask>>", "").replace("<<ServiceTask>>", "").trim()

        nodes << new WfNode(
            id: taskName.replaceAll("\\s+", "_").toLowerCase(),
            name: taskName,
            type: isUserTask ? "USER" : "SERVICE",
            candidateGroup: currentSwimlane
        )
      }
    }
    definitions[definitionId] = nodes
  }

  // 2. Start a fresh process instance
  WorkflowInstance startProcess(String definitionId, Map<String, Object> initialVariables) {
    def instance = new WorkflowInstance(
        definitionId: definitionId,
        variables: new WorkflowVariables(data: initialVariables)
    )
    instances << instance

    // Execute from the first node
    executeNext(instance, 0)
    return instance
  }

  // 3. The Execution Loop (Handles token progression)
  private void executeNext(WorkflowInstance instance, int nodeIndex) {
    List<WfNode> nodes = definitions[instance.definitionId]
    if (nodeIndex >= nodes.size()) {
      instance.status = InstanceStatus.COMPLETED
      return
    }

    WfNode currentNode = nodes[nodeIndex]
    instance.currentActivityId = currentNode.id

    if (currentNode.type == "SERVICE") {
      println "Executing background Service Task: [${currentNode.name}]"
      // Simulate instant backend execution, move immediately to next node
      executeNext(instance, nodeIndex + 1)
    }
    else if (currentNode.type == "USER") {
      println "Hit Wait-State! Creating a persistent User Task for Group: [${currentNode.candidateGroup}]"

      // Create the record in our Job Table database
      taskDatabase << new UserTaskInstance(
          instanceId: instance.id,
          taskName: currentNode.name,
          candidateGroup: currentNode.candidateGroup
      )
      // HALT execution here. Token rests on this node.
    }
  }

  // 4. THE JOB LIST CALCULATION ENGINE
  // This queries your task database to find what tasks a specific user can see
  List<UserTaskInstance> getJobListForUser(String username, List<String> userGroups) {
    return taskDatabase.findAll { task ->
      task.status != TaskStatus.COMPLETED &&
          (task.assignedTo == username || (task.assignedTo == null && userGroups.contains(task.candidateGroup)))
    }
  }

  // 5. Complete a human step and wake the engine back up
  void completeUserTask(String taskId, String username, Map<String, Object> submissionData) {
    UserTaskInstance task = taskDatabase.find { it.id == taskId }
    if (!task) throw new IllegalArgumentException("Task not found")

    task.assignedTo = username
    task.status = TaskStatus.COMPLETED

    // Hydrate back the workflow instance state
    WorkflowInstance instance = instances.find { it.id == task.instanceId }
    instance.variables.data.putAll(submissionData)

    // Find where we left off in the definition array and resume
    List<WfNode> nodes = definitions[instance.definitionId]
    int resumeIndex = nodes.findIndexOf { it.id == instance.currentActivityId }

    println "\n--- Resuming Process Instance ${instance.id} via User Completion ---"
    executeNext(instance, resumeIndex + 1)
  }
}

// Lightweight schema representing parsed metadata 
class WfNode {
  String id
  String name
  String type // "SERVICE" or "USER"
  String candidateGroup
}

// Initialize Engine
def engine = new WorkflowEngine()

// 1. Deploy our multi-role PlantUML definition 
def plantUmlScript = """
@startuml

|Manager|
:Approve Purchase Order <<UserTask>>;

|Finance|
:Release Funds <<UserTask>>;

|System|
:Send Confirmation Email <<ServiceTask>>;
stop
@enduml
"""
engine.deployDefinition("purchase_flow", plantUmlScript)

// 2. Start a workflow instance (e.g., Someone submits a $5,000 laptop request)
println "=== Starting Process ==="
def instance = engine.startProcess("purchase_flow", [amount: 5000, item: "Developer Laptop"])

// 3. Calculate John's Job List
// John is a Manager. Let's see what jobs are waiting for him.
println "\n=== Fetching Job List for John (Manager) ==="
def johnsJobs = engine.getJobListForUser("john_doe", ["Manager"])

johnsJobs.each { job ->
  println "TO-DO FOR JOHN: [ID: ${job.id}] Task: '${job.taskName}' (Waiting for group: ${job.candidateGroup})"
}

// 4. John completes his task via UI
String targetsTaskId = johnsJobs[0].id
engine.completeUserTask(targetsTaskId, "john_doe", [managerApproved: true])

// 5. Let's check John's Job list again now that he's finished his part
println "\n=== Checking John's Job List After Completion ==="
def updatedJobs = engine.getJobListForUser("john_doe", ["Manager"])
println "John's remaining active job count: ${updatedJobs.size()}"

// 6. Let's see what is waiting for the Finance group job list
println "\n=== Checking Finance Team Job List ==="
def financeJobs = engine.getJobListForUser("alice_finance", ["Finance"])
financeJobs.each { job ->
  println "TO-DO FOR FINANCE: Task: '${job.taskName}'"
}
