package dk.kvalitetsit.hello.dao;

import dk.kvalitetsit.hello.dao.entity.HelloEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.Assert.*;

public class HelloDaoTest {
    @Autowired
    private HelloDao helloDao;

    @Test
    public void testByMessageId() {
        assertTrue(true);
    }

}
