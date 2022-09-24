# hackathon-project



ls -ltr /Users/SaiNitesh/.m2/repository/uk/ac/abdn/SimpleNLG/4.5.0/SimpleNLG-4.5.0.jar


java -cp /Users/SaiNitesh/.m2/repository/informationRetrieval/assignment-rishabh/0.0.1-SNAPSHOT/assignment-rishabh-0.0.1-SNAPSHOT.jar:/Users/SaiNitesh/.m2/repository/uk/ac/abdn/SimpleNLG/4.5.0/SimpleNLG-4.5.0.jar::/Users/SaiNitesh/.m2/repository/org/jsoup/jsoup/1.11.3/jsoup-1.11.3.jar informationRetrieval.BM25 input output

java -cp /Users/SaiNitesh/.m2/repository/informationRetrieval/assignment-rishabh/0.0.1-SNAPSHOT/assignment-rishabh-0.0.1-SNAPSHOT.jar:/Users/SaiNitesh/.m2/repository/uk/ac/abdn/SimpleNLG/4.5.0/SimpleNLG-4.5.0.jar::/Users/SaiNitesh/.m2/repository/org/jsoup/jsoup/1.11.3/jsoup-1.11.3.jar informationRetrieval.BM25 input output



java -cp /Users/SaiNitesh/.m2/repository/informationRetrieval/assignment-rishabh/0.0.1-SNAPSHOT/assignment-rishabh-0.0.1-SNAPSHOT.jar:/Users/SaiNitesh/.m2/repository/uk/ac/abdn/SimpleNLG/4.5.0/SimpleNLG-4.5.0.jar informationRetrieval.SingularToPlural



curl --location --request POST 'http://localhost:7071/api/fileUpload?fileName=testSample.txt' --header 'Content-Type: application/octet-stream' --data-binary '@/Users/SaiNitesh/Hackathon/stopWords.txt'