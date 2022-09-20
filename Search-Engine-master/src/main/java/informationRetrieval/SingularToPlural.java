package informationRetrieval;
import simplenlg.features.Feature;
import simplenlg.features.NumberAgreement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.realiser.english.Realiser;

//REf https://github.com/simplenlg/simplenlg/blob/master/src/test/java/simplenlg/syntax/english/TutorialTest.java
public class SingularToPlural{
    public static void main(String args[]){
        String plural="apples";
        
        String result=getSingular(plural);
        System.out.println(result);

        plural="always";
        result=getSingular(plural);
        System.out.println(result);

        plural="girls";
        result=getSingular(plural);
        System.out.println(result);


        plural="boys";
        result=getSingular(plural);
        System.out.println(result);


    }


    public static String getSingular(String plural){
        final Lexicon lexicon = Lexicon.getDefaultLexicon();
        String singular="";
        NLGFactory nlgFactory = new NLGFactory(lexicon);
		Realiser realiser = new Realiser(lexicon);
        NPPhraseSpec subject = nlgFactory.createNounPhrase(plural); 
        subject.setFeature(Feature.NUMBER, NumberAgreement.SINGULAR); 
        singular=realiser.realiseSentence(subject);
        System.out.println(singular);
        return singular;
    }


    /*
     * 
     * SPhraseSpec p = nlgFactory.createClause();
		p.setSubject("my dog");
		p.setVerb("is");  // variant of be
		p.setObject("George");

		String output = realiser.realiseSentence(p);
		assertEquals("My dog is George.", output);

		p = nlgFactory.createClause();
		p.setSubject("my dog");
		p.setVerb("chases");  // variant of chase
		p.setObject("George");

		output = realiser.realiseSentence(p);
		assertEquals("My dog chases George.", output);

		p = nlgFactory.createClause();
		p.setSubject(nlgFactory.createNounPhrase("the", "dogs"));   // variant of "dog"
		p.setVerb("is");  // variant of be
		p.setObject("happy");  // variant of happy
		output = realiser.realiseSentence(p);
		assertEquals("The dog is happy.", output);

		p = nlgFactory.createClause();
		p.setSubject(nlgFactory.createNounPhrase("the", "children"));   // variant of "child"
		p.setVerb("is");  // variant of be
		p.setObject("happy");  // variant of happy
		output = realiser.realiseSentence(p);
		assertEquals("The child is happy.", output);

		// following functionality is enabled
		p = nlgFactory.createClause();
		p.setSubject(nlgFactory.createNounPhrase("the", "dogs"));   // variant of "dog"
		p.setVerb("is");  // variant of be
		p.setObject("happy");  // variant of happy
		output = realiser.realiseSentence(p);
		assertEquals("The dog is happy.", output); //corrected automatically
     */
}
    
