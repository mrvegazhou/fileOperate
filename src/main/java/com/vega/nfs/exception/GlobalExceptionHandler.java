package com.vega.nfs.exception;

import com.vega.nfs.utils.JSONResult;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


@RestController
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = MultipartException.class)
    @ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE)
    @ResponseBody
    public Map<String, Object> handleMultipartException(MultipartException ex) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (ex instanceof MultipartException) {
            final MultipartException mEx = (MultipartException) ex;
            if (ex.getCause() instanceof FileUploadBase.FileSizeLimitExceededException) {
                final FileUploadBase.FileSizeLimitExceededException flEx = (FileUploadBase.FileSizeLimitExceededException) mEx.getCause();
                map.put("size", flEx.getPermittedSize());
            } else if (ex.getCause() instanceof FileUploadBase.SizeLimitExceededException) {
                final FileUploadBase.SizeLimitExceededException flEx = (FileUploadBase.SizeLimitExceededException) mEx.getCause();
                map.put("size", flEx.getPermittedSize());
            } else {
                map.put("error", ex.getMessage());
            }
        } else {
            map.put("error", ex.getMessage());
        }
        map.put("status",HttpStatus.BAD_REQUEST.toString());
        return map;
    }


    @ExceptionHandler(value = RuntimeException.class)
    @ResponseBody
    public String handleDefaultException(HttpServletRequest req, Exception e) throws Exception {
        return JSONResult.reResultString(HttpStatus.BAD_REQUEST.toString(), e.getMessage(), null);
    }
}