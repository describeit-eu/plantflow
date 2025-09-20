package eu.describeit.plantflow

import eu.describeit.plantflow.engine.PlantFlowDelegate
import org.codehaus.groovy.control.CompilerConfiguration

static void main(String[] args) {

  CompilerConfiguration cc = new CompilerConfiguration();
  cc.setScriptBaseClass(DelegatingScript.class.getName());

  GroovyShell sh = new GroovyShell(new Binding(), cc);
  DelegatingScript script = (DelegatingScript) sh.parse(new File("/home/kovax/workspace/describe-it/plantflow/src/test/resources/forkEndMerge.puml").text)
  script.setDelegate(new PlantFlowDelegate());

  try {
    script.run();
  } catch (Exception e) {
    e.printStackTrace()
  }

}
