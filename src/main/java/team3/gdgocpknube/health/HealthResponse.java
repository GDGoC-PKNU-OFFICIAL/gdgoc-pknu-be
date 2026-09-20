package team3.gdgocpknube.health;

public record HealthResponse(String status, String db) {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    public static HealthResponse up() {
        return new HealthResponse(UP, UP);
    }

    public static HealthResponse dbDown() {
        return new HealthResponse(DOWN, DOWN);
    }
}
