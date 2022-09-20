package informationRetrieval;
import java.util.Set;
import java.util.HashSet;
import java.io.*;
public class StopWords {
    public static void main(String args[]){

        String path="/Users/SaiNitesh/Hackathon/stopWords.txt";

        Set<String> stopWords= getStoprWords(path);
        System.out.println("stop words are::");
        for(String word:stopWords){
             System.out.println(word);
        }

    }


    public static Set<String> getStoprWords(String path){
        Set<String> words = new HashSet<String>();
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

}
