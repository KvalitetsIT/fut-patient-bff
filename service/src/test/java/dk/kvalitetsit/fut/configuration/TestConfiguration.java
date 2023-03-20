package dk.kvalitetsit.fut.configuration;

import dk.kvalitetsit.fut.hello.HelloDao;
import dk.kvalitetsit.fut.hello.HelloDaoImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class TestConfiguration {
    // Configure beans used for test

    @Bean
    public HelloDao helloDao() {
        return new HelloDaoImpl();
    }
}
