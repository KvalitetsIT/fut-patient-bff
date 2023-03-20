package dk.kvalitetsit.fut.hello;

import org.openapitools.api.FutApi;
import org.openapitools.model.HelloResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
// CORS - Consider if this is needed in your application. Only here to make Swagger UI work.
@CrossOrigin(origins = "http://localhost")
public class HelloController implements FutApi {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @Override
    public ResponseEntity<HelloResponseDto> v1HelloGet() {
        var helloResponse = new org.openapitools.model.HelloResponseDto();
        helloResponse.setName("Futty");
        helloResponse.setNow(OffsetDateTime.now());

        return ResponseEntity.ok(helloResponse);
    }
}
