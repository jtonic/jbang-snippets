///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//SOURCES jb/JBangJunit5Test.java

import jb.JBangJunit5Test;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnit5RunnerMain extends JBangJunit5Test {

    public static void main(String[] args) {
        JBangJunit5Test.run(JUnit5RunnerMain.class);
    }

    @Test
    void additionWorks() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void stringConcatWorks() {
        assertEquals("stonic-junit5", "stonic" + "-junit5");
    }
}