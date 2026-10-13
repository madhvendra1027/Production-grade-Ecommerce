package com.example.project.globalExceptionHandler;

import com.example.project.payLoad.APIresponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MyGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MyGlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e){
        Map<String, String> response = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError)err).getField();
            String message = err.getDefaultMessage();
            response.put(fieldName,message);
        });
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIresponse> myResourceNotFoundException(ResourceNotFoundException e){
        return new ResponseEntity<>(new APIresponse(e.getMessage(), false), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIexception.class)
    public ResponseEntity<APIresponse> myAPIexception(APIexception e){
        return new ResponseEntity<>(new APIresponse(e.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    /**
     * Ownership failures from the service layer.
     *
     * Needed now that services throw AccessDeniedException when a caller asks for a
     * resource belonging to someone else. Without an explicit handler this would
     * surface as a generic 500 and leak a stack trace.
     *
     * The message is deliberately fixed rather than echoing the exception, so a
     * response cannot be used to probe which ids exist.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIresponse> myAccessDeniedException(AccessDeniedException e){
        logger.warn("Access denied : {}", e.getMessage());
        return new ResponseEntity<>(
                new APIresponse("You do not have permission to perform this action.", false),
                HttpStatus.FORBIDDEN);
    }

    /** Entity-level bean validation that slipped past DTO validation. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIresponse> myConstraintViolationException(ConstraintViolationException e){
        return new ResponseEntity<>(new APIresponse(e.getMessage(), false), HttpStatus.BAD_REQUEST);
    }

    // Deliberately no @ExceptionHandler(Exception.class) catch-all here. It would also
    // intercept Spring MVC's own exceptions - unreadable request bodies, path variable
    // type mismatches, unmapped routes - and relabel those 400s and 404s as 500s.
    // Boot's default handling already returns a JSON body without a stack trace.
}
