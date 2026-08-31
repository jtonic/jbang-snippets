///usr/bin/env jbang "$0" "$@" ; exit $?
//GROOVY 5.1.1
//DEPS org.apache.groovy:groovy:5.1.1
//DEPS org.apache.groovy:groovy-ant:5.1.1
//DEPS org.apache.commons:commons-lang3:3.20.0
//FILES organization.jsonc

import groovy.ant.AntBuilder
import org.apache.commons.lang3.StringUtils

def file = new File("organization.jsonc")
println StringUtils.upperCase("reading: ${file.path}")
println file.getText("UTF-8")

def ant = new AntBuilder()
ant.copy(file: "organization.jsonc", tofile: "organization.jsonc.bak")
println "copied to organization.jsonc.bak"