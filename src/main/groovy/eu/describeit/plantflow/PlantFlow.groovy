package eu.describeit.plantflow

import eu.describeit.plantflow.engine.PlantFlowAction
import eu.describeit.plantflow.engine.PlantFlowScript
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

@CompileStatic
class PlantFlow {
  List<PlantFlowAction> pflowActions
  PlantFlowScript pflowScript

  PlantFlow(String pflowName, List<PlantFlowAction> actions) {
    pflowActions = actions
    initPflowScript(pflowName)
  }

  private void initPflowScript(String pflowName) {
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setScriptBaseClass(PlantFlowScript.class.getName());

    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    pflowScript = (PlantFlowScript) engine.createScript(pflowName, new Binding())
    pflowScript.setDelegate(pflowScript);
    pflowScript.pflowActions = pflowActions
  }

  List<PlantFlowAction> calculateNext() {
    pflowScript.run();
    return pflowScript.nextActions
  }

  void dryRun() {
    pflowScript.run();
  }
}
