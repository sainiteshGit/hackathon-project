package informationRetrieval;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.specialized.BlobInputStream;

import java.io.*;
public class FileBlobRead {

    public static void main( String[] args ) throws IOException
    {

        //DefaultEndpointsProtocol=https;AccountName=hacktranscriptstorage;AccountKey=C14dvwE6bePyof3aCgLNk1xL9OtPSyMExRzJB8EalswgfQdtHJilD5Q8XVb07FM210X1LDnLC6xa+AStsRBc+g==;EndpointSuffix=core.windows.net
        String connectStr="DefaultEndpointsProtocol=https;AccountName=hacktranscriptstorage;AccountKey=C14dvwE6bePyof3aCgLNk1xL9OtPSyMExRzJB8EalswgfQdtHJilD5Q8XVb07FM210X1LDnLC6xa+AStsRBc+g==;EndpointSuffix=core.windows.net";
        String containerName="hackblob";
        uploadNewFile(connectStr,containerName);
        readAFile(connectStr,containerName);

    }


    public static void uploadNewFile(String connectStr,String containerName){

        try{

            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            // Create a local file in the ./data/ directory for uploading and downloading
            String localPath = ".data/";
            String fileName = "quickstart"+ "testFile.txt";
    
            File localFile = new File(localPath + fileName);
            // Write text to the file
            FileWriter writer = new FileWriter(localPath + fileName, true);
            writer.write("Hello, World1111!");
            writer.close();
    
            // Get a reference to a blob
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            System.out.println("\nUploading to Blob storage as blob:\n\t" + blobClient.getBlobUrl());
            // Upload the blob
            blobClient.uploadFromFile(localPath + fileName,true);
            System.out.println("Completed");
            return;

        }
        catch(Exception e){
            //e.printStackTrace();;
        }
        return;
       
    }


    public static void readAFile(String connectStr,String containerName){

        try{

            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            // Create a local file in the ./data/ directory for uploading and downloading
            String localPath = ".data/";
            String fileName = "quickstart"+ "testFile.txt";
            // Get a reference to a blob
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            System.out.println("\nReading to Blob storage as blob:\n\t" + blobClient.getBlobUrl());
            // Upload the blob
            StringBuilder result = new StringBuilder();
            try (BlobInputStream blobIS = blobClient.openInputStream()) {
                InputStreamReader inputStream = new InputStreamReader(blobIS, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStream);
                for (String line; (line = reader.readLine()) != null; ) {
                    if (result.length() > 0) {
                        result.append(line);
                    }
                    result.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(result.toString());

            System.out.println("Completed");
            return;

        }
        catch(Exception e){
            e.printStackTrace();;
        }
        return;
    }
    
}
