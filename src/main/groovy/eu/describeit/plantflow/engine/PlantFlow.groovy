package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

@CompileStatic
class PlantFlow {
  List<PlantFlowAction> pflowActions
  DelegatingScript pflowScript
  PlantFlowDelegate delegate

  PlantFlow(String pflowName, List<PlantFlowAction> actions) {
    pflowActions = actions
    delegate = new PlantFlowDelegate(pflowActions: pflowActions)
    initPflowScript(pflowName)
  }

  private void initPflowScript(String pflowName) {
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setScriptBaseClass(DelegatingScript.class.getName());

    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    pflowScript = (DelegatingScript) engine.createScript(pflowName, new Binding())
    pflowScript.setDelegate(delegate);
  }

  List<PlantFlowAction> calculateNext() {
    pflowScript.run();
    return delegate.nextActions
  }

  void dryRun() {
    pflowScript.run();
  }
}
