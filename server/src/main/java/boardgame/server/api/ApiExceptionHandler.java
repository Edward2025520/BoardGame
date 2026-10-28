package boardgame.server.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException error) {
        return ResponseEntity.status(error.status())
                .body(Map.of("error", error.getMessage() == null ? "" : error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException error) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", error.getMessage() == null ? "" : error.getMessage()));
    }
}
