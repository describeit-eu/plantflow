package eu.describeit.plantflow

import eu.describeit.plantflow.calculate.engine.CalculateNextContext
import eu.describeit.plantflow.calculate.engine.CalculateNextScript
import eu.describeit.plantflow.calculate.engine.StopCalculateNext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer

import static Utility.getResourceText

@Slf4j
@CompileStatic
class PlantFlow {
  Map<String, PlantFlowAction> pflowActions
  CalculateNextScript pflowScript
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
    cc.setScriptBaseClass(CalculateNextScript.class.getName())

    // method calls are statically bound to PlantFlowScript, while internal evaluation remains mockable
    cc.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic))

    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    pflowScript = (CalculateNextScript) engine.createScript(pflowName+'.pflow', pflowBinding)
    pflowScript.setDelegate(pflowScript)

    pflowScript.context = new CalculateNextContext(getResourceText(pflowName+'Context.json'))
    pflowScript.actions = pflowActions
  }

  List<PlantFlowAction> calculateNext() {
    try {
      log.info('run() --------------------------------------------------')
      pflowScript.run()
    } catch (StopCalculateNext ex) {
      log.info('calculateNext() - stopped by {}', ex.message)
    }
    return pflowScript.nextActions
  }
}
