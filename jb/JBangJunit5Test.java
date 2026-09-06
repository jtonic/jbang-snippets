//DEPS org.junit.jupiter:junit-jupiter:5.11.4
//DEPS org.junit.platform:junit-platform-launcher:1.11.4

package jb;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SuppressWarnings("DuplicatedCode")
public abstract class JBangJunit5Test {

    protected LauncherDiscoveryRequest discoveryRequest() {
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(getClass()))
                .build();
    }

    public static void run(Class<? extends JBangJunit5Test> testClass) {
        JBangJunit5Test instance;
        try {
            instance = testClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not instantiate test runner " + testClass.getName(), e);
        }

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(instance.discoveryRequest());

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));

        long failed = summary.getTotalFailureCount();
        int exitCode = failed > 0 ? 1 : 0;
        System.out.printf("Tests found: %d, succeeded: %d, failed: %d%n",
                summary.getTestsStartedCount(), summary.getTestsSucceededCount(), failed);
        System.exit(exitCode);
    }
}