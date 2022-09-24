package com.bettersearch;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.file.CloudFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }
	
	@FunctionName("searchVideos")
    public HttpResponseMessage run2(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("searchQuery");
        final String searchQuery = request.getBody().orElse(query);

        if (searchQuery == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
			System.out.println("Executing Query: "+searchQuery);
			List<Map.Entry<String, Double>>  results = QueryExecution.search(searchQuery);
			StringBuilder sb = new StringBuilder();
			System.out.println("# of results: "+results.size());
			for(Map.Entry<String, Double> entry : results){
				sb.append(entry.getKey() + ":" + entry.getValue());
				sb.append("\n");
			}
			System.out.println(sb.toString());
            return request.createResponseBuilder(HttpStatus.OK).body(sb.toString()).build();
        }
    }


    @FunctionName("fileUpload")
    public HttpResponseMessage fileUpload(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET,HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,dataType = "binary")
                HttpRequestMessage<Optional<Byte[]>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        String contentType=request.getHeaders().get("content-type");
        int reqIndex = contentType.indexOf("boundary");
        
        final String fileName = request.getQueryParameters().get("fileName");
        Byte[] bs=request.getBody().get();
        InputStream in = new ByteArrayInputStream(toPrimitives(bs));
        context.getLogger().info(contentType+"cccccccccccccc");

        System.out.print(contentType+"cccccccccccccc");
        String boundary=contentType.substring(reqIndex + "boundary=".length());
        boundary = boundary.replaceAll("\"", "").trim();
        context.getLogger().info(boundary+"---boundary");

        int buffSize=1024;

        try{

            upload(toPrimitives(bs), fileName);
            context.getLogger().info("Java HTTP file upload ended. Length: " + bs.length);    
        }
        catch(Exception e){
            System.out.println("FIle upload exception:::"+e);
        }
        return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + bs.length).build();
    }


    public static String extractFileName(String header) {
        final String FILENAME_PARAMETER = "filename=";
        final int FILENAME_INDEX = header.indexOf(FILENAME_PARAMETER);
        String name = header.substring(FILENAME_INDEX + FILENAME_PARAMETER.length(), header.lastIndexOf("\""));
        String fileName = name.replaceAll("\"" , "").replaceAll(" ", "");
        return fileName;
    }

    byte[] toPrimitives(Byte[] oBytes)
    {
        byte[] bytes = new byte[oBytes.length];

        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }

      public static void upload(byte[] content, String fileName){
        String storageConnectionString="DefaultEndpointsProtocol=https;AccountName=hacktranscriptstorage;AccountKey=C14dvwE6bePyof3aCgLNk1xL9OtPSyMExRzJB8EalswgfQdtHJilD5Q8XVb07FM210X1LDnLC6xa+AStsRBc+g==;EndpointSuffix=core.windows.net";
        String containerName="hackblob";
        try{
            CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient serviceClient = account.createCloudBlobClient();

            // Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            CloudBlockBlob blob=container.getBlockBlobReference("input/"+fileName);
            blob.uploadFromByteArray(content, 0, content.length-1);   
        }
        catch(Exception e){
            System.out.print("EXception e:::"+e);
        }
        return;
      }
}
