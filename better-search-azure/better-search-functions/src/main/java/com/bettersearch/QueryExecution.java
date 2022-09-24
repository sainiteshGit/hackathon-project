package com.bettersearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.io.*;
import java.util.Map.Entry;
import simplenlg.features.Feature;
import simplenlg.features.NumberAgreement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.realiser.english.Realiser;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.specialized.BlobInputStream;
/**
 * Term-at-a-time query execution strategy
 * Query term weights are also taken into account
 * 
 * @author Rishabh
 *
 */
public class QueryExecution {

	static HashMap<String, Double> queryTermWeightsMap= new HashMap<String, Double>();// maintain weights of query terms

	HashMap<String, Double> docScoreMap = new HashMap<>(); // doc vs score
	static boolean queryWeightFlag = false;

	public List<Map.Entry<String, Double>> execute(String[] queryTokens) {
		//BufferedReader reader;
		//String dictionaryFile = "D:/hackathon/Search-Engine-master/dictsAndPostings/dictionary.txt";
		
		List<String> dictionary = readFileFromBlob("dictionary.txt");
		System.out.println(queryTokens.length);
		//iterate : term at a time
		for(String term : queryTokens) {
			term = term.toLowerCase();
			term = getSingular(term);
			
			for(int i=0; i<dictionary.size(); i+=3){
				String termInDict = dictionary.get(i);
				if(termInDict.equals(term)){
					int freq = Integer.parseInt(dictionary.get(i+1));
					int start = Integer.parseInt(dictionary.get(i+2));
					readSpecificLinesOfPostings(start, start+freq-1, term);
					break;
				}
			}
			
			/*
			try {
				//reader = new BufferedReader(new FileReader(dictionaryFile));
				//String line = reader.readLine();
				while (line !=null) {
					if(line.equals(term)) {
						int freq = Integer.parseInt(reader.readLine());
						int start = Integer.parseInt(reader.readLine());
						readSpecificLinesOfPostings(start, start+freq-1, term);
						break;
					}
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/

		}
		HashMap<String, Double> sortedMap = sortMapByValue();

		System.out.println("TOP RESULTS");
		displayResults(sortedMap);
		
		List<Map.Entry<String, Double>> results = new LinkedList<Map.Entry<String, Double>>(sortedMap.entrySet()); 
		return results;
	}


	private void displayResults(Map<String,Double> sortedMap) {

		if(sortedMap.size()==0) {
			System.out.println("This Search Engine has no relevant documents for query provided.");
		}
		Iterator it = sortedMap.entrySet().iterator();
		int cnt=0;
		while(it.hasNext()) {
			Entry pair = (Entry) it.next();
			System.out.println("Doc: "+pair.getKey()+".txt"+" Score: "+pair.getValue());
			if(++cnt ==10) {
				break;
			}
		}
	}

	private HashMap<String, Double> sortMapByValue() {
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(docScoreMap.entrySet()); 
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() { 
			public int compare(Map.Entry<String, Double> o1,  
					Map.Entry<String, Double> o2) 
			{ 
				return (o2.getValue()).compareTo(o1.getValue()); 
			} 
		}); 

		HashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>(); 
		for (Map.Entry<String, Double> entry : list) { 
			sortedMap.put(entry.getKey(), entry.getValue()); 
		} 
		return sortedMap; 
	}

	private void readSpecificLinesOfPostings(int start, int end, String term) {
		List<String> postings = readFileFromBlob("postings.txt");
		
		for(int i=start; i<=end; i++){
			String docAndScore	= postings.get(i-1);
			String doc=docAndScore.split(",")[0];
			float score = Float.parseFloat(docAndScore.split(",")[1]);
			updateScores(doc, score, term);
		}
		/*
		for(int i= start;i<=end;i++) {
			try {
				String docAndScore	= Files.readAllLines(Paths.get("D:/hackathon/Search-Engine-master/dictsAndPostings/postings.txt")).get(i-1);
				String doc=docAndScore.split(",")[0];
				float score = Float.parseFloat(docAndScore.split(",")[1]);
				updateScores(doc, score, term);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
	}

	private void updateScores(String doc, float score, String term) {
		double queryWeight = queryTermWeightsMap.get(term);
		double upd_score = queryWeight * score;
		if(docScoreMap.containsKey(doc)) {
			docScoreMap.put(doc,docScoreMap.get(doc)+upd_score);
		}else {
			docScoreMap.put(doc, upd_score);
		}
	}


	private static void fetchQueryTems(String[] args, String[] queryTerms, Double[] weights) {
		int j=0;
		int k=0;
		for(int i=1;i<args.length;i++) {
			if(i%2==0) {
				queryTerms[j]=args[i];	
				j++;
			}else {
				weights[k]=Double.parseDouble(args[i]);
				k++;
			}
		}
	}
	private static void createQueryTermWeightMap(String[] queryTerms, Double[] weights) {
		for(int i=0;i<queryTerms.length;i++) {
			queryTerms[i] = getSingular(queryTerms[i]);
			if(queryWeightFlag) {
				queryTermWeightsMap.put(queryTerms[i].toLowerCase(),weights[i]);
			}else {
				queryTermWeightsMap.put(queryTerms[i].toLowerCase(),1.0);
			}
		}
	}


	public static void main(String args[]) {

		QueryExecution obj = new QueryExecution();
		String[] queryTerms=null;
		Double[] weights = null;
		if(args[0].equalsIgnoreCase("wt")) {
			queryWeightFlag=true;
			queryTerms = new String[args.length/2];
			weights = new Double[args.length/2];
			fetchQueryTems(args,queryTerms,weights);
		}else {
			queryTerms=args;
		}
		createQueryTermWeightMap(queryTerms,weights);
		obj.execute(queryTerms);
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
	
	public static List<Map.Entry<String, Double>> search(String query){
		QueryExecution obj = new QueryExecution();
		String[] queryTerms = query.split("\\s");
		Double[] weights = new Double[queryTerms.length/2];
		
		createQueryTermWeightMap(queryTerms,weights);
		List<Map.Entry<String, Double>> results = obj.execute(queryTerms);
		
		return results;
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
    
		
}
