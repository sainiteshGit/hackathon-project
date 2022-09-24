package informationRetrieval;

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
	

	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		File inputFolder = new File(args[0]);
		String outputDir = args[1];
		File outputFolder = new File(outputDir);


		IndexConstruction bm25 = new IndexConstruction();
		File[] files = inputFolder.listFiles();
		bm25.setNumDocs(files.length);
		String tempPath="D:/hackathon/Search-Engine-master/temp";
		FileUtils.deleteDirectory(new File(tempPath));
		FileUtils.deleteDirectory(new File(outputDir));

		Files.createDirectories(Paths.get(tempPath));
		Files.createDirectories(Paths.get(outputDir));

		for(int i=0;i<files.length;i++) {
			if(files[i].isFile()) {
				Document doc = Jsoup.parse(files[i], "utf-8");
				doc = new Cleaner(Whitelist.simpleText()).clean(doc);
				if(doc.body()!=null) {
					String text=doc.body().text();
					//comment below line, instead handling in createTokenFrequencyMap()

					//text=text.replaceAll("[0-9]","");
					text=text.replaceAll("\\u000B","");
					text=text.replaceAll("\\u0001","");
					HashMap<String, Integer> freqMapForFile = createTokenFrequencyMap(text);
					//writeTempTokenFile(freqMapForFile,tempPath+"/"+ String.valueOf(i+1) +".properties");
					writeTempTokenFile(freqMapForFile,tempPath+"/"+ findFileName(String.valueOf(files[i])) +".properties");
				}
			}
		}
		bm25.setAvgDocSize();	
		BM25scoreForFile(tempPath,outputFolder);
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
		PrintWriter pwriter=null;
		PrintWriter dwriter=null;
		int location=1;
		try {
			pwriter = new PrintWriter(outputDir+"/postings.txt", "UTF-8");
			dwriter = new PrintWriter(outputDir+"/dictionary.txt", "UTF-8");

		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		for(String term_key : sortedIndexMap.keySet()) {
			List<DocIdAndScore> posting = sortedIndexMap.get(term_key);
			if(term_key.length()>45) {
				continue;
			}
			for(DocIdAndScore post : posting) {
				pwriter.println(post.docId+SEPERATOR_COMMA+post.score);
			}
			dwriter.println(term_key);
			dwriter.println(invertedIndexCountMap.get(term_key)); //signify how many documents has this term
			dwriter.println(location);
			location += posting.size();
		}
		pwriter.close();
		dwriter.close();
	}


	private void setAvgDocSize() {
		this.avg_doc_size=this.total_words/this.num_docs;
	}

	private static void writeTempTokenFile(HashMap<String,Integer> freqMapForFile, String filePath) {
		Properties properties = new Properties();

		for(Entry<String, Integer> entry: freqMapForFile.entrySet()) {
			properties.put(entry.getKey(), entry.getValue().toString());
		}
		try {
			properties.store(new OutputStreamWriter(new FileOutputStream(filePath),"utf-8"), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static void BM25scoreForFile(String tempPath, File outputFolder) {
		File dir = new File(tempPath);
		File[] propListing = dir.listFiles();
		double max_wt = Double.MIN_VALUE;
		double min_wt = Double.MAX_VALUE;

		for (File prop : propListing) {
			InputStream propStream = null;
			try {
				propStream = new FileInputStream(prop.toString());
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
			writeTokenFile(scoreMapPerFile,tempPath+"/"+prop.getName());
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

		File dir = new File(outputFolder);
		File[] propListing = dir.listFiles();
		for (File prop : propListing) {
			InputStream propStream = null;
			Properties properties = new Properties();
			try {
				propStream = new FileInputStream(prop.toString());


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
						invertedIndexMap.get(key).add(new DocIdAndScore(prop.getName().replace(".properties",""), normScore));
					}else {
						ArrayList<DocIdAndScore> list = new ArrayList<>();
						list.add(new DocIdAndScore(prop.getName().replace(".properties",""), normScore));
						invertedIndexMap.put(key, list);
					}
				}
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void writeTokenFile(HashMap<String,Double> freqMapForFile, String filePath) {
		Properties properties = new Properties();

		for(Entry<String, Double> entry: freqMapForFile.entrySet()) {
			properties.put(entry.getKey(), entry.getValue().toString());
		}
		try {
			properties.store(new OutputStreamWriter(new FileOutputStream(filePath),"utf-8"), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		String path = "D:/hackathon/Search-Engine-master/src/main/java/informationRetrieval/stopWords.txt";
       HashSet<String> words = new HashSet<String>();
       try(BufferedReader reader = new BufferedReader(new FileReader(path))){
         for(String line = reader.readLine(); line != null; line = reader.readLine()) {
             line=line.replaceAll("\\s", "");
             words.add(line);
          }
        }
        catch(Exception e){
             System.out.println("error while reading file");
        }
        return words;
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
