package com.example.todo.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Extending ResponseEntityExceptionHandler makes every standard Spring MVC error
// (malformed JSON, wrong method, ...) a Problem Details response too.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed");
        problem.setProperty("errors", fieldErrors(ex));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.UNPROCESSABLE_CONTENT, request);
    }

    @ExceptionHandler(TodoNotFoundException.class)
    ProblemDetail handleNotFound(TodoNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // {"title": ["must not be blank"]}, keyed by the JSON (snake_case) field name
    private static Map<String, List<String>> fieldErrors(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(snakeCase(error.getField()), key -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return errors;
    }

    // dueDate -> due_date, same rule as the SNAKE_CASE naming strategy in application.yaml
    private static String snakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
