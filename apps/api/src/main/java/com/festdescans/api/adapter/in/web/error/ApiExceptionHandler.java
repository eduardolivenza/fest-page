package com.festdescans.api.adapter.in.web.error;

import com.festdescans.api.application.port.in.CaptchaFailedException;
import com.festdescans.api.application.port.in.InvalidCredentialsException;
import com.festdescans.api.domain.order.UnknownProductsException;
import com.festdescans.api.domain.product.ProductInUseException;
import com.festdescans.api.domain.product.SlugAlreadyUsedException;
import com.festdescans.api.domain.shared.InvalidDataException;
import com.festdescans.api.domain.shared.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps business and framework errors to the api's status codes and {@code { message }} bodies. */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    static final String INVALID_DATA = "Invalid data";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        Map<String, List<String>> fields = new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields
                .computeIfAbsent(topLevel(error.getField()), k -> new java.util.ArrayList<>())
                .add(error.getDefaultMessage()));
        List<String> form = e.getBindingResult().getGlobalErrors().stream().map(g -> g.getDefaultMessage()).toList();
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(INVALID_DATA, new ValidationErrorResponse.Errors(form, fields)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ValidationErrorResponse> unreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(INVALID_DATA,
                new ValidationErrorResponse.Errors(List.of("Malformed or mistyped JSON body"), Map.of())));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class, InvalidDataException.class})
    ResponseEntity<ErrorResponse> invalidInput(Exception e) {
        return status(HttpStatus.BAD_REQUEST, INVALID_DATA);
    }

    @ExceptionHandler(UnknownProductsException.class)
    ResponseEntity<ErrorResponse> unknownProducts(UnknownProductsException e) {
        return status(HttpStatus.BAD_REQUEST, "One or more products not found");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> invalidCredentials(InvalidCredentialsException e) {
        return status(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(CaptchaFailedException.class)
    ResponseEntity<ErrorResponse> captchaFailed(CaptchaFailedException e) {
        return status(HttpStatus.FORBIDDEN, "Captcha verification failed");
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> notFound(Exception e) {
        return status(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(ProductInUseException.class)
    ResponseEntity<ErrorResponse> productInUse(ProductInUseException e) {
        return status(HttpStatus.CONFLICT, "No se puede eliminar: el producto tiene pedidos asociados");
    }

    @ExceptionHandler(SlugAlreadyUsedException.class)
    ResponseEntity<ErrorResponse> slugTaken(SlugAlreadyUsedException e) {
        return status(HttpStatus.CONFLICT, "Ya existe un producto con este slug");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return status(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return status(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Unhandled error", e);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }

    private static ResponseEntity<ErrorResponse> status(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    /** "items[0].quantity" -> "items", like Zod's flatten(). */
    private static String topLevel(String field) {
        int cut = field.length();
        int dot = field.indexOf('.');
        int bracket = field.indexOf('[');
        if (dot >= 0) {
            cut = Math.min(cut, dot);
        }
        if (bracket >= 0) {
            cut = Math.min(cut, bracket);
        }
        return field.substring(0, cut);
    }
}
