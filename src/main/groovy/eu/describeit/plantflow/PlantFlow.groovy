package eu.describeit.plantflow

import eu.describeit.plantflow.engine.CalculateNextContext
import eu.describeit.plantflow.engine.PlantFlowAction
import eu.describeit.plantflow.engine.PlantFlowScript
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer

import static Utility.getResourceText

@CompileStatic
class PlantFlow {
  Map<String, PlantFlowAction> pflowActions
  PlantFlowScript pflowScript
  Binding pflowBinding

  PlantFlow(String pflowName, List<PlantFlowAction> actions, Binding binding) {
    pflowActions = actions.collectEntries  { PlantFlowAction action -> [(action.name): action]}
    binding.setVariable('actions', pflowActions)
    pflowBinding = binding
    initPflowScript(pflowName)
  }

  PlantFlow(String pflowName, List<PlantFlowAction> actions) {
    this(pflowName, actions, new Binding())
  }

  private void initPflowScript(String pflowName) {
    CompilerConfiguration cc = new CompilerConfiguration()
    cc.setScriptBaseClass(PlantFlowScript.class.getName())

    // method calls are statically bound to PlantFlowScript, while internal evaluation remains mockable
    cc.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic))

    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    pflowScript = (PlantFlowScript) engine.createScript(pflowName+'.pflow', pflowBinding)
    pflowScript.setDelegate(pflowScript)

    pflowScript.executionContext = new CalculateNextContext(getResourceText(pflowName+'Context.json'))
    pflowScript.actions = pflowActions
  }

  List<PlantFlowAction> calculateNext() {
    pflowScript.run()
    return pflowScript.nextActions
  }
}
