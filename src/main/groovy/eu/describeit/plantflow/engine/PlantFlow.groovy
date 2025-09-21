package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

@CompileStatic
class PlantFlow {
  List<PlantFlowAction> pflowActions
  DelegatingScript pflowScript
  PlantFlowDelegate delegate

  PlantFlow(String pflow, List<PlantFlowAction> actions) {
    pflowActions = actions
    delegate = new PlantFlowDelegate(pflowActions: pflowActions)
    initPflowScript(pflow)
  }

  private void initPflowScript(String pflow) {
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setScriptBaseClass(DelegatingScript.class.getName());

    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    pflowScript = (DelegatingScript) engine.createScript(pflow, new Binding())
    pflowScript.setDelegate(delegate);
  }

  List<PlantFlowAction> calculateNext() {
    pflowScript.run();
    return []
  }

  void dryRun() {
    pflowScript.run();
  }
}
