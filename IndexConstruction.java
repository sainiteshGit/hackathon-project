package com.bettersearch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import simplenlg.features.Feature;
import simplenlg.features.NumberAgreement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.realiser.english.Realiser;
import java.util.Set;
import java.util.HashSet;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.*;

public class IndexConstruction {

	private static final String SEPERATOR_COMMA = ",";
	static class MyComparator implements Comparator<Entry<String, List<DocIdAndScore>>> {
		public int compare(Entry<String, List<DocIdAndScore>> o1, Entry<String, List<DocIdAndScore>> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

	static class DocIdAndScore{

		private String docId;
		private double score;

		public DocIdAndScore(String id,double sc) {
			this.docId=id;	
			this.score=sc;
		}
	}
	static HashMap<String, Integer> invertedIndexCountMap= new HashMap<String, Integer>();// stores in how many docs a token occurred
	static HashMap<String, List<DocIdAndScore>> invertedIndexMap= new HashMap<String, List<DocIdAndScore>>();// term VS posting list  {deadlock: {docId: pipelene.prop; score: 1.88; docId: C#1.prop, score=0.66 }}

	public static int num_docs;
	public static int avg_doc_size;
	public static int total_words;
	public static double k = 1.2;
	public static double b = 0.75;


	public static HashMap<String,Integer> createTokenFrequencyMap(String bodyText)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyText.getBytes(StandardCharsets.UTF_8))));
		HashMap<String, Integer> freqMapForFile = new HashMap<String, Integer>();
		StringTokenizer tokens;
		String line;
		HashSet<String> stopWords=buildSetForStopWords();
		while((line=buffReader.readLine())!=null) {
			tokens = new StringTokenizer(line, " \t\n\r\f,.:;?![]{}()|%#$/<>@\\'*_+-=&\"“”~—`'’‘");
			while(tokens.hasMoreTokens()) {
				total_words++;
				String token=tokens.nextToken().toLowerCase();
				if(token.length()>2 && token.endsWith("s")){
					token = getSingular(token);
					System.out.println(token);
				}
				if(token.length()>3 && token.endsWith("ing")){
					token = token.substring(0, token.length()-3);
				}
				//operations for map respective to one file
				if(freqMapForFile.containsKey(token)) {
					int cnt = freqMapForFile.get(token);
					freqMapForFile.put(token, cnt+1);
				}else if(stopWords.contains(token) || !isValid(token)) {
					//do nothing
				}else {
					freqMapForFile.put(token,1);
					if(invertedIndexCountMap.containsKey(token)) {
						int cnt = invertedIndexCountMap.get(token);
						invertedIndexCountMap.put(token, cnt+1);
					}else {
						invertedIndexCountMap.put(token, 1);
					}
				}

			}
		}
		return freqMapForFile;
	}


	public static boolean isValid(String text) {
        for (char entry : text.toCharArray()) {
            if (!Character.isLetter(entry)) {
				//System.out.println(text);
                return false;
            }
        }
        return true;
    }
	

	public static void deleteDir(String path){
		String storageConnectionString="DefaultEndpointsProtocol=https;AccountName=hacktranscriptstorage;AccountKey=C14dvwE6bePyof3aCgLNk1xL9OtPSyMExRzJB8EalswgfQdtHJilD5Q8XVb07FM210X1LDnLC6xa+AStsRBc+g==;EndpointSuffix=core.windows.net";
        String containerName="hackblob";
        try{
            CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient serviceClient = account.createCloudBlobClient();

            // Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            CloudBlockBlob blob=container.getBlockBlobReference(path);
			blob.deleteIfExists();
        }
        catch(Exception e){
            System.out.print("EXception e:::"+e);
        }
        return;

	}

	public static HashMap<String,File> getListOfFiles(String dirPath){

		HashMap<String,File> files=new HashMap<String,File>();

		String storageConnectionString="DefaultEndpointsProtocol=https;AccountName=hacktranscriptstorage;AccountKey=C14dvwE6bePyof3aCgLNk1xL9OtPSyMExRzJB8EalswgfQdtHJilD5Q8XVb07FM210X1LDnLC6xa+AStsRBc+g==;EndpointSuffix=core.windows.net";
        String containerName="hackblob";
        try{
            CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient serviceClient = account.createCloudBlobClient();

            // Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            CloudBlockBlob blob=container.getBlockBlobReference(dirPath);
			CloudBlobDirectory blobDirectory = container.getDirectoryReference(dirPath);
			Iterable<ListBlobItem> listBlobItem = blobDirectory.listBlobs();
			for (ListBlobItem blobItem : listBlobItem) {
				if (blobItem instanceof CloudBlockBlob) {
						CloudBlockBlob blockBlob = (CloudBlockBlob) blobItem;
						String fileName=blobItem.getUri().toString();
						String fileText=blockBlob.downloadText();
						files.put(fileName,new File(fileText));
				}
			}
        }
        catch(Exception e){
            System.out.print("EXception e:::"+e);
        }
		return files;

	}

	public static void indexConstruct() throws IOException {

		long startTime = System.currentTimeMillis();
		String inputDir="input/";
		String outputDir = "output/";


		IndexConstruction bm25 = new IndexConstruction();
		HashMap<String,File> fileMap = getListOfFiles(inputDir);
		List<String> fileNames=new ArrayList<>(fileMap.keySet());

		bm25.setNumDocs(fileNames.size());
		String tempPath="temp/";
		deleteDir(tempPath);
		deleteDir(outputDir);

		for(int i=0;i<fileNames.size();i++) {

			Document doc = Jsoup.parse(fileMap.get(fileNames.get(i)), "utf-8");
			doc = new Cleaner(Whitelist.simpleText()).clean(doc);
			if(doc.body()!=null) {
				String text=doc.body().text();
				//comment below line, instead handling in createTokenFrequencyMap()

				//text=text.replaceAll("[0-9]","");
				text=text.replaceAll("\\u000B","");
				text=text.replaceAll("\\u0001","");
				HashMap<String, Integer> freqMapForFile = createTokenFrequencyMap(text);
				//writeTempTokenFile(freqMapForFile,tempPath+"/"+ String.valueOf(i+1) +".properties");
				writeTempTokenFile(freqMapForFile,tempPath+"/"+ findFileName(fileNames.get(i)) +".properties");
			}
		}
		bm25.setAvgDocSize();	
		BM25scoreForFile(tempPath,outputDir);
		LinkedHashMap<String, List<DocIdAndScore>> sortedIndexMap = sortInvertedIndexMapAlphabetically();

		//FileUtils.forceDeleteOnExit(new File(tempPath));

		createPostingAndDictionary(outputDir,sortedIndexMap);

		long endTime = System.currentTimeMillis();
		System.out.println("Took "+(endTime - startTime) + " ms"); 
	}

	private static String findFileName(String name){
		int stopIdx = 0;
		
		for(int i = name.length()-1; i>=0; i--){
			if(name.charAt(i)=='\\')
			{
				stopIdx = i;
				break;
			}
		}
		return name.substring(stopIdx+1, name.length()).replace(".txt", "");
	}
	
	private static void createPostingAndDictionary(String outputDir, LinkedHashMap<String, List<DocIdAndScore>> sortedIndexMap) {
		String postingFilePath=outputDir+"/postings.txt";
		String dictionaryFilePath=outputDir+"/dictionary.txt";
		StringBuilder postingSb=new StringBuilder();
		StringBuilder dictionaySb=new StringBuilder();

		int location=1;

		for(String term_key : sortedIndexMap.keySet()) {
			List<DocIdAndScore> posting = sortedIndexMap.get(term_key);
			if(term_key.length()>45) {
				continue;
			}
			for(DocIdAndScore post : posting) {
				postingSb.append(post.docId+SEPERATOR_COMMA+post.score+"\n");
			}
			dictionaySb.append(term_key+"\n");
			dictionaySb.append(invertedIndexCountMap.get(term_key)+"\n");
			dictionaySb.append(location);
			location += posting.size();
		}
		upload(postingSb.toString().getBytes(), postingFilePath);
		upload(dictionaySb.toString().getBytes(), dictionaryFilePath);

	}


	private void setAvgDocSize() {
		this.avg_doc_size=this.total_words/this.num_docs;
	}

	private static void writeTempTokenFile(HashMap<String,Integer> freqMapForFile, String filePath) {
		StringBuilder sb=new StringBuilder();

		for(Entry<String, Integer> entry: freqMapForFile.entrySet()) {
			sb.append(entry.getKey()+"="+entry.getValue().toString()+"\n");
		}
		upload(sb.toString().getBytes(), filePath);
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


	private static void BM25scoreForFile(String tempPath, String outputFolder) {

		HashMap<String,File> fileMap=getListOfFiles(tempPath);
		List<String> files=new ArrayList<>(fileMap.keySet());
		double max_wt = Double.MIN_VALUE;
		double min_wt = Double.MAX_VALUE;

		for (String fileName : files) {
			InputStream propStream = null;
			try {
				propStream = new FileInputStream(fileMap.get(fileName));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			HashMap<String, Double> scoreMapPerFile = new HashMap<>();
			Properties properties = new Properties();
			try {
				properties.load(propStream);
				propStream.close();
				for(String key : properties.stringPropertyNames()) {
					String freq = properties.getProperty(key);
					int word_freq = Integer.parseInt(freq);
					double word_freq_sqrt = Math.sqrt(word_freq);
					int docFreq=1;
					if(invertedIndexCountMap.containsKey(key)) {
						docFreq = invertedIndexCountMap.get(key);
					}else {
						continue;
						//	   System.out.println("NOT FOUND"+key);
					}
					double idf = calculateIDF(docFreq);
					double tf = calculateTF(word_freq_sqrt,properties.size());
					double bm25score = tf*idf;
					if(bm25score>max_wt) {
						max_wt=bm25score;
					}
					if(bm25score<min_wt) {
						min_wt= bm25score;
					}

					scoreMapPerFile.put(key, bm25score);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//write into one file at a time
			writeTokenFile(scoreMapPerFile,tempPath+"/"+fileName);
			try {
				propStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		normalizeScores(max_wt,min_wt,tempPath);
	}

	private static LinkedHashMap<String, List<DocIdAndScore>> sortInvertedIndexMapAlphabetically() {
		List<Entry<String, List<DocIdAndScore>>> list = new LinkedList<Entry<String, List<DocIdAndScore>>>(
				invertedIndexMap.entrySet());

		Collections.sort(list, new MyComparator());

		LinkedHashMap<String, List<DocIdAndScore>> sortedMap = new LinkedHashMap<>();
		for (Entry<String, List<DocIdAndScore>> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}	
		return sortedMap;
	}


	private static void normalizeScores(double max_wt, double min_wt, String outputFolder) {

		HashMap<String,File> fileMap=getListOfFiles(outputFolder);
		List<String> files=new ArrayList<>(fileMap.keySet());
		for (String fileName:files) {
			InputStream propStream = null;
			Properties properties = new Properties();
			try {
				propStream = new FileInputStream(fileMap.get(fileName));


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				properties.load(propStream);
				propStream.close();
				for(String key : properties.stringPropertyNames()) {
					String scoreStr = properties.getProperty(key);
					double score = Double.parseDouble(scoreStr);
					double normScore = (score - min_wt)/(max_wt-min_wt);

					if(invertedIndexMap.containsKey(key)) {
						invertedIndexMap.get(key).add(new DocIdAndScore(fileName.replace(".properties",""), normScore));
					}else {
						ArrayList<DocIdAndScore> list = new ArrayList<>();
						list.add(new DocIdAndScore(fileName.replace(".properties",""), normScore));
						invertedIndexMap.put(key, list);
					}
				}
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void writeTokenFile(HashMap<String,Double> freqMapForFile, String filePath) {
		StringBuilder sb=new StringBuilder();
		for(Entry<String, Double> entry: freqMapForFile.entrySet()) {
			sb.append(entry.getKey()+"="+entry.getValue().toString());
		}
		upload(sb.toString().getBytes(), filePath);
	}

	private static double calculateTF(double word_freq, int doc_size) {
		double tf = (word_freq * (k + 1)) / (word_freq + k * (1 - 
				b + b * doc_size / avg_doc_size));
		return tf;
	}

	private static double calculateIDF(int docFreq) {
		return Math.log(num_docs/(docFreq+1));
	}

	public void setNumDocs(int num) {
		this.num_docs=num;
	}
	public int getNumDocs() {
		return num_docs;
	}
	
	public static HashSet<String> buildSetForStopWords(){
		String path = "stopWords.txt";
		List<String> wordLines=readFileFromBlob(path);
       HashSet<String> words = new HashSet<String>();
	   for(String line:wordLines) {
			line=line.replaceAll("\\s", "");
			words.add(line);
	 	}  
        return words;
    }

	public static List<String> readFileFromBlob(String fileName){

		String connectStr = "DefaultEndpointsProtocol=https;AccountName=hackathonrgb442;AccountKey=/XQmCiIzXPph0lR/e6xUl2QuYa277SJcoReYC1FF2g5Vww4nYdl6gLLnLgBsBygkkzU2tUy5p32t+ASt7fvYdg==;EndpointSuffix=core.windows.net";
		String containerName = "hackblob";
		List<String> result = new ArrayList<>();
        try{

            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            // Create a local file in the ./data/ directory for uploading and downloading
            //String localPath = ".data/";
            //String fileName = "quickstart"+ "testFile.txt";
            // Get a reference to a blob
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            System.out.println("\nReading to Blob storage as blob:\n\t" + blobClient.getBlobUrl());
            // Upload the blob
            //StringBuilder result = new StringBuilder();
			
            try (BlobInputStream blobIS = blobClient.openInputStream()) {
                InputStreamReader inputStream = new InputStreamReader(blobIS, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStream);
                for (String line; (line = reader.readLine()) != null; ) {
                    result.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
				throw e;
            }
            System.out.println("Blob found: "+result);

            return result;

        }
        catch(Exception e){
			result.clear();
            e.printStackTrace();
			
        }
        return result;
    }
	
	public static String getSingular(String plural){
    
		if(plural.equals("dashboards") || plural.equals("deadlocks")){
			return plural.substring(0, plural.length()-2);
		}
        final Lexicon lexicon = Lexicon.getDefaultLexicon();
        String singular="";
        NLGFactory nlgFactory = new NLGFactory(lexicon);
		Realiser realiser = new Realiser(lexicon);
        NPPhraseSpec subject = nlgFactory.createNounPhrase(plural); 
        subject.setFeature(Feature.NUMBER, NumberAgreement.SINGULAR); 
        singular=realiser.realiseSentence(subject);
        //System.out.println(singular);
        if(singular!=null && singular.length()>1){
            singular=singular.substring(0,singular.length()-1);
        }
        return singular.toLowerCase();
    }
}
