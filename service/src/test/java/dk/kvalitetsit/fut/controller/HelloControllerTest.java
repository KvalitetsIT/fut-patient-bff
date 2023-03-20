package dk.kvalitetsit.fut.controller;

import dk.kvalitetsit.fut.hello.HelloController;
import dk.kvalitetsit.fut.hello.HelloService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HelloControllerTest {
    private HelloController helloController;
    private HelloService helloService;

    @Before
    public void setup() {
        helloService = Mockito.mock(HelloService.class);

        helloController = new HelloController(helloService);
    }

    @Test
    public void testCallController() {
        assertTrue(true);
    }
}
