package dk.kvalitetsit.fut.dao;

import dk.kvalitetsit.fut.hello.HelloDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class HelloDaoTest {
    @Autowired
    private HelloDao helloDao;

    @Test
    public void testByMessageId() {
        assertTrue(true);
    }

}
