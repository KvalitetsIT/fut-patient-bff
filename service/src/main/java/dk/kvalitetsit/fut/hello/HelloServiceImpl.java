package dk.kvalitetsit.fut.hello;

import dk.kvalitetsit.fut.hello.model.HelloServiceInput;
import dk.kvalitetsit.fut.hello.model.HelloServiceOutput;

import java.time.ZonedDateTime;

public class HelloServiceImpl implements HelloService {
    @Override
    public HelloServiceOutput helloServiceBusinessLogic(HelloServiceInput input) {
        return new HelloServiceOutput(input.name(), ZonedDateTime.now());
    }
}
