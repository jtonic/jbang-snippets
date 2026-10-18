//DEPS org.junit.platform:junit-platform-console:1.8.2

package kt.jb

import org.junit.platform.console.options.CommandLineOptions
import org.junit.platform.console.tasks.ConsoleTestExecutor
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.reflect.KClass

abstract class JBangJunit5Test {

    companion object {
        fun run(testClass: KClass<out JBangJunit5Test>) {
            val options = CommandLineOptions().apply {
                selectedClasses = listOf(testClass.java.name)
                setReportsDir(Path(System.getProperty("user.dir")))
            }
            ConsoleTestExecutor(options).execute(PrintWriter(System.out))
        }
    }
}
