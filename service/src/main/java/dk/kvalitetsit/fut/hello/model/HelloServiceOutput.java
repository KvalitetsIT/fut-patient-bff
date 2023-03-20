package dk.kvalitetsit.fut.hello.model;

import java.time.ZonedDateTime;

public record HelloServiceOutput(String name, ZonedDateTime now) {
}
