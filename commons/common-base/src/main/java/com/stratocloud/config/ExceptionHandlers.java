package com.stratocloud.config;


import com.stratocloud.auth.CallContext;
import com.stratocloud.constant.ErrorCodes;
import com.stratocloud.request.ErrorResult;
import com.stratocloud.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@ControllerAdvice(annotations = {Controller.class, RestController.class})
@ResponseBody
@Slf4j
public class ExceptionHandlers {

    private static final Set<Integer> clientErrors = Set.of(
            ErrorCodes.BAD_COMMAND,
            ErrorCodes.UNAUTHORIZED,
            ErrorCodes.AUTHENTICATION_FAILURE,
            ErrorCodes.INVALID_ARGUMENTS,
            ErrorCodes.PREMIUM_ONLY
    );

    private ResponseEntity<ErrorResult> handleError(int errorCode, String errorMessage, Exception e){

        if(clientErrors.contains(errorCode)) {
            log.error(e.toString());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        } else {
            log.error(e.toString(), e);
        }
        log.error("RequestId: {}.", CallContext.requestId());
        ErrorResult result = new ErrorResult(errorCode, errorMessage);
        return ResponseEntity.status(errorCode/10).body(result);
    }



    @ExceptionHandler(ExternalAccountInvalidException.class)
    public ResponseEntity<ErrorResult> handleExternalAccountInvalidException(ExternalAccountInvalidException e){
        return handleError(ErrorCodes.INVALID_ARGUMENTS, e.getMessage(), e);
    }


    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResult> handleEntityNotFoundException(EntityNotFoundException e){
        return handleError(ErrorCodes.ENTITY_NOT_FOUND, e.getMessage(), e);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResult> handleResourceNotFoundException(ResourceNotFoundException e){
        return handleError(ErrorCodes.RESOURCE_NOT_FOUND, e.getMessage(), e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResult> handleUnauthorizedException(UnauthorizedException e){
        return handleError(ErrorCodes.UNAUTHORIZED, e.getMessage(), e);
    }

    @ExceptionHandler(StratoAuthenticationException.class)
    public ResponseEntity<ErrorResult> handleAuthenticationException(StratoAuthenticationException e){
        return handleError(ErrorCodes.AUTHENTICATION_FAILURE, e.getMessage(), e);
    }

    @ExceptionHandler(PermissionNotGrantedException.class)
    public ResponseEntity<ErrorResult> handlePermissionNotGrantedException(PermissionNotGrantedException e){
        return handleError(ErrorCodes.PERMISSION_NOT_GRANTED, e.getMessage(), e);
    }

    @ExceptionHandler(BadCommandException.class)
    public ResponseEntity<ErrorResult> handleBadCommandException(BadCommandException e){
        return handleError(ErrorCodes.BAD_COMMAND, e.getMessage(), e);
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ErrorResult> handleInvalidArgumentException(InvalidArgumentException e){
        return handleError(ErrorCodes.INVALID_ARGUMENTS, e.getMessage(), e);
    }

    @ExceptionHandler(PremiumOnlyException.class)
    public ResponseEntity<ErrorResult> handlePremiumOnlyException(PremiumOnlyException e){
        return handleError(ErrorCodes.PREMIUM_ONLY, e.getMessage(), e);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> handleIllegalArgumentException(IllegalArgumentException e){
        return handleError(ErrorCodes.INVALID_ARGUMENTS, e.getMessage(), e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResult> handleNotValidException(MethodArgumentNotValidException e){
        FieldError fieldError = Objects.requireNonNull(e.getFieldError());
        return handleError(
                ErrorCodes.INVALID_ARGUMENTS,
                fieldError.getField()+": "+fieldError.getDefaultMessage(),
                e
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResult> handleDataIntegrityException(DataIntegrityViolationException e){
        return handleError(ErrorCodes.INVALID_ARGUMENTS, "Invalid arguments, data integrity violated.", e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> handleException(Exception e){
        String errorMessage = "Unexpected error.";
        return handleError(ErrorCodes.UNEXPECTED_ERROR, errorMessage, e);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResult> handleOptimisticLockingFailureException(OptimisticLockingFailureException e){
        String errorMessage = "系统繁忙，请稍后重试。";
        return handleError(ErrorCodes.BAD_COMMAND, errorMessage, e);
    }


    @ExceptionHandler(StratoException.class)
    public ResponseEntity<ErrorResult> handleStratoException(StratoException e){
        return handleError(ErrorCodes.STRATO_ERROR, e.getMessage(), e);
    }
}
