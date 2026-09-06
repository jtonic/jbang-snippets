# Short java/kotlin/scala snippets

## Scope

It is mainly to get conformable with jbang.

## Snippets

1. Deserialize jsonc (JSON with comments) using jackson databind: [v3](JsoncJacksonV3Main.java) & [v2](JsoncJacksonV2Main.java)
2. jbang for [JUnit 5](JUnit5RunnerMain.java) 
3. jbang for [SpringBoot 4.1 app](SbMain.java) 
4. jbang for [SpringBoot 4.1 app](stonic/sb/SbAppMain.java) along with the [SpringBoot IT](stonic/sb/SbTestsMain.java)

## How to run the jbang snippets

```shell
## Run a simple java script
jbang JsoncJacksonV3Main.java

## Run SpringBootTest integration tests
jbang stonic/sb/SbTestsMain.java

## Run SpringBootApplication app
jbang stonic/sb/SbAppMain.java

## Run a simple Groovy script
jbang SimpleReadFileMain.groovy

## Run a Kotlin script with Arrow Effects (Raise) and Context Parameters
jbang SimpleArrowMain.kt
```

## More snippets

See [TODO](./TODO.md)

## References

- [jbang directives](https://www.jbang.dev/documentation/jbang/latest/script-directives.html)
- [jbang file organization](https://www.jbang.dev/documentation/jbang/latest/organizing.html)
- [jbang multiple languages - kotlin](https://www.jbang.dev/documentation/jbang/latest/multiple-languages.html#kotlin-scripts-kt-experimental)
- [jbang JVM options](https://www.jbang.dev/documentation/jbang/latest/execution-options.html#jvm-options-and-flags)

