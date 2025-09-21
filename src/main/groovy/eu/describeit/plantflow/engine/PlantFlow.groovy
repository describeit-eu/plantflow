package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

import static eu.describeit.plantflow.engine.PlantFlowDelegate.RunMode.CALCULATE_NEXT
import static eu.describeit.plantflow.engine.PlantFlowDelegate.RunMode.DRY

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
    delegate.runMode = CALCULATE_NEXT
    pflowScript.run();
    return delegate.nextActions
  }

  void dryRun() {
    delegate.runMode = DRY
    pflowScript.run();
  }
}
