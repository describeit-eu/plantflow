package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

@CompileStatic
class PlantFlowExecutor {

  def execute(String pflowFileName) {
    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setScriptBaseClass(DelegatingScript.class.getName());

//    def engine = new GroovyScriptEngine("/home/kovax/workspace/describe-it/plantflow/src/test/resources")
    def engine = new GroovyScriptEngine("src/test/resources")
    engine.setConfig(cc)

    def script = (DelegatingScript)  engine.createScript(pflowFileName, new Binding())
    script.setDelegate(new PlantFlowDelegate());

    script.run();
  }
}
