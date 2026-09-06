///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//SOURCES JBangTest.java

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnit5RunnerMain extends JBangTest {

    public static void main(String[] args) {
        run(JUnit5RunnerMain.class, args);
    }

    @Test
    void additionWorks() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void stringConcatWorks() {
        assertEquals("stonic-junit5", "stonic" + "-junit5");
    }

    @Test
    void failingTest() {
        assertEquals(1, 2);
    }
}