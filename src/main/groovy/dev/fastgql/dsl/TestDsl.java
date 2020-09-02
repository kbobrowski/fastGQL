package dev.fastgql.dsl;

import groovy.lang.*;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;

public class TestDsl {

  public static void main(String[] args) throws IOException {
    CompilerConfiguration config = new CompilerConfiguration();
    config.setScriptBaseClass(RolesPermissionsConfig.class.getName());
    GroovyShell shell = new GroovyShell(new Binding(), config);
    PermissionsSpec result = (PermissionsSpec) shell.evaluate(new File("src/main/resources/permissions.groovy"));
    OpSpec op = result.getTable("test").getRole("default").getOp(RoleSpec.OpType.select);
    System.out.println(op);
  }
}
