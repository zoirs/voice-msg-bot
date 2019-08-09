package ru.chernyshev.recognizer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

public class ErrorHandler implements ResponseErrorHandler {

    private static Logger logger = LoggerFactory.getLogger(RecognizerApplication.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        InputStream inputStream = response.getBody();
        String body = IOUtils.toString(inputStream, Charset.defaultCharset());
        logger.error("Cant get response {}; {};", response.getStatusText(), body);
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().series() == CLIENT_ERROR || response.getStatusCode().series() == SERVER_ERROR;
    }
}