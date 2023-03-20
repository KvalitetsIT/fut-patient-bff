package dk.kvalitetsit.fut.hello;

import dk.kvalitetsit.fut.hello.model.HelloServiceOutput;
import dk.kvalitetsit.fut.hello.model.HelloServiceInput;

public interface HelloService {
    HelloServiceOutput helloServiceBusinessLogic(HelloServiceInput input);
}
