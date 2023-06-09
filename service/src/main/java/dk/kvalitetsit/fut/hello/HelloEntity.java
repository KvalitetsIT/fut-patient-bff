package dk.kvalitetsit.fut.hello;

public record HelloEntity(Long id, String name) {
    public static HelloEntity createInstance(String name) {
        return new HelloEntity(null, name);
    }
}
