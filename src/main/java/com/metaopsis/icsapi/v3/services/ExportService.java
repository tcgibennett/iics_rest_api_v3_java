package com.metaopsis.icsapi.v3.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaopsis.icsapi.v3.dom.ExportRequest;
import com.metaopsis.icsapi.v3.dom.StartResponse;
import com.metaopsis.icsapi.v3.dom.Status;
import com.metaopsis.icsapi.v3.dom.User;
import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ExportService {
    private User user = null;
    final static Logger logger = Logger.getLogger(ImportService.class);
    private ObjectMapper mapper;
    private RestTemplate rest;
    private String _INFASESSIONID;

    public ExportService(User user)
    {
        this.user = user;
        logger.debug(user.toString());
        this._INFASESSIONID = user.getUserInfo().getSessionId();

        // Set ObjectMapper
        this.mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Set RestTemplate
        this.rest = new RestTemplate();
        this.rest.setErrorHandler(new CustomResponseErrorHandler());
    }

    public StartResponse start(ExportRequest request) throws InformaticaCloudException
    {
        logger.info(this.getClass().getName()+"::start::enter");
        Writer jsonWriter = new StringWriter();
        HttpEntity<String> requestEntity = null;
        ResponseEntity<String> responseEntity = null;
        StartResponse response = null;
        try {
            mapper.writeValue(jsonWriter, request);
            jsonWriter.flush();
            requestEntity = new HttpEntity<String>(jsonWriter.toString(), this.buildHttpHeaders("application/json","application/json"));

            responseEntity = rest.exchange(user.getProducts()[0].getBaseApiUrl()+"/public/core/v3/export", HttpMethod.POST, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful())
            {
                logger.info("Informatica Cloud V3 Export Start 200 Success");
                response = mapper.readValue(responseEntity.getBody(), StartResponse.class);
            } else {
                logger.info("Informatica Cloud V3 Export Start " + responseEntity.getStatusCode().toString());
                logger.error(responseEntity.toString());
                throw new InformaticaCloudException(responseEntity.toString());
            }

        } catch(Exception e)
        {
            throw new InformaticaCloudException(e.getMessage());
        }
        logger.info(this.getClass().getName()+"::start::exit");
        return response;
    }

    public Status status(String id, boolean showDetail) throws InformaticaCloudException
    {
        logger.info(this.getClass().getName()+"::status::enter");

        HttpEntity<String> requestEntity = null;
        ResponseEntity<String> responseEntity = null;
        Status status = null;
        try {
            requestEntity = new HttpEntity<String>("", this.buildHttpHeaders("application/json", "application/json"));

            responseEntity = rest.exchange(user.getProducts()[0].getBaseApiUrl() + "/public/core/v3/export/" + id + (showDetail ? "?expand=objects" : ""), HttpMethod.GET, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                logger.info("Informatica Cloud V3 Export Status 200 Success");
                status = mapper.readValue(responseEntity.getBody(), Status.class);
            } else {
                logger.info("Informatica Cloud V3 Export Status " + responseEntity.getStatusCode().toString());
                logger.error(responseEntity.toString());
                throw new InformaticaCloudException(responseEntity.toString());
            }
        } catch(Exception e)
        {
            throw new InformaticaCloudException(e.getMessage());
        }

        logger.info(this.getClass().getName()+"::status::exit");
        return status;
    }

    public void statusLog(String id, File file) throws InformaticaCloudException
    {
        logger.info(this.getClass().getName()+"::statusLog::enter");
        try {
            BufferedWriter  writer = new BufferedWriter(new FileWriter(file));
            HttpEntity<String> requestEntity = null;
            ResponseEntity<String> responseEntity = null;
            requestEntity = new HttpEntity<String>("", this.buildHttpHeaders("text/plain","application/json"));

            responseEntity = rest.exchange(user.getProducts()[0].getBaseApiUrl() + "/public/core/v3/export/" + id + "/log", HttpMethod.GET, requestEntity, String.class);

            logger.info("Informatica Cloud V3 Export Status Log " + responseEntity.getStatusCode().toString());
            if (responseEntity.getStatusCode().is2xxSuccessful())
            {
                writer.write(responseEntity.getBody());
                writer.newLine();
                writer.close();
            } else {
                logger.error(responseEntity.getBody());
                throw new InformaticaCloudException(responseEntity.getBody());
            }

        } catch(Exception e) {
            throw new InformaticaCloudException(e.getMessage());
        }
        logger.info(this.getClass().getName()+"::statusLog::exit");
    }

    public void download(String id, File file) throws InformaticaCloudException
    {
        logger.info(this.getClass().getName()+"::download::enter");
        try {
            HttpEntity<String> requestEntity = null;
            ResponseEntity<byte[]> responseEntity = null;
            requestEntity = new HttpEntity<String>("", this.buildHttpHeaders("application/zip","application/json"));

            responseEntity = rest.exchange(user.getProducts()[0].getBaseApiUrl() + "/public/core/v3/export/" + id + "/package", HttpMethod.GET, requestEntity, byte[].class);
            logger.info("Informatica Cloud V3 Export Download " + responseEntity.getStatusCode().toString());
            if (responseEntity.getStatusCode().is2xxSuccessful())
            {
                Files.write(Paths.get(file.getAbsolutePath()), responseEntity.getBody());
            } else {
                throw new InformaticaCloudException("Download Package " + id + " Error");
            }

        }
        catch (Exception e) {
            throw new InformaticaCloudException(e.getMessage());
        }

        logger.info(this.getClass().getName()+"::download::exit");
    }


    private HttpHeaders buildHttpHeaders(String accept, String contentType)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", accept);
        headers.add("Content-Type", contentType);
        headers.add("INFA-SESSION-ID", this._INFASESSIONID);

        return headers;
    }
}
