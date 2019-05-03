package com.metaopsis.icsapi.v3.services;

import org.apache.log4j.Logger;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * Created by tbennett on 11/8/16.
 */
public class CustomResponseErrorHandler implements ResponseErrorHandler {
    private static final Logger logger = Logger.getLogger(CustomResponseErrorHandler.class);
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return RestUtil.isError(response.getStatusCode());
    }

    public void handleError(ClientHttpResponse response) throws IOException {
        logger.error("Response Error: " + response.getStatusCode() + " " + response.getStatusText());
    }
}
