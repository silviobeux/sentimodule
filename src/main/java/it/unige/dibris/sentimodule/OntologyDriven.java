package it.unige.dibris.sentimodule;

//Module for sentiment analysis 
import it.unige.dibris.adm.ClassifierObject;
import it.unige.dibris.adm.ModuleOutput;
import it.unige.dibris.adm.TCModule;
import it.unige.dibris.adm.TCOutput;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelPointer;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.jlt.util.Language;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;




//import unige.mymood.TagObject;
import edu.mit.jwi.item.POS;
//import org.annolab.tt4j.TokenHandler;
//import org.annolab.tt4j.TreeTaggerWrapper;


public class OntologyDriven implements TCModule {

	private final static String pathToSWN = "resources/SentiWordNet_3.0.0_20130122.txt";
	private static Language lang;
	private  ArrayList<String> negativeKeywords = new ArrayList<String>();
	private  ArrayList<String> lemmas = new ArrayList<String>(); 
	
	public static void loadNegativeKeywords() throws FileNotFoundException{
		File file = new File("./negation/" + lang.getName());
		Scanner input = new Scanner(file);
		ArrayList<String> negativeKeywords = new ArrayList<String>();
		while(input.hasNext()) {
			negativeKeywords.add(input.next());
		}
		input.close();
	}
	
	public boolean isNegativeKeyword(String word){
		return negativeKeywords.contains(word);
	}
	
	public static boolean isStopToken(String word){
		return word.matches("[.!,?';:]");
	}
	
	public static POS getPOS(String word){
		/*
		TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<String>();
		switch (this.textLang) {
		case EN: tt.setModel("english-utf8.par"); break;
		case ES: tt.setModel("spanish-utf8.par"); break;
		case DE: tt.setModel("german-utf8.par"); break;
		case FR: tt.setModel("french-utf8.par"); break;
		case IT: tt.setModel("italian-utf8.par"); break;
		default: break;
		}
		
		tt.setHandler(new TokenHandler<String>() {
			public void token(String token, String pos, String lemma) {
		        System.out.println(token + "\t" + pos + "\t" + lemma);
		        String val = corr.get(pos.toLowerCase());
		        if (val != null) {
					switch (val) {
					case "n": tagObj.add(new TagObject(token,lemma,POS.NOUN)); break;
					case "a": tagObj.add(new TagObject(token,lemma,POS.ADJECTIVE)); break;
					case "r": tagObj.add(new TagObject(token,lemma,POS.ADVERB)); break;
					case "v": tagObj.add(new TagObject(token,lemma,POS.VERB)); break;
					default:break;
					}
		        }
			}
		});
		//System.out.println(words);
		tt.process(words);
		*/
		return POS.ADJECTIVE;
	}
	
	public String getAntonym (String word) throws IOException{
		BabelNet bn = BabelNet.getInstance();
		//Assume only with adjective POS
		//Retrieve all word synsets
		for (BabelSynset syn : bn.getSynsets(lang, word, POS.ADJECTIVE)){
			//For each word synset, retrieve all the sysnets that are in antonym relation with it 
			List<BabelSynset> antonym = syn.getRelatedMap().get(BabelPointer.ANTONYM);
			//If exist at least one antonym synset
			if(antonym != null){
				//Retrieve the first sense of the first antonym synset
				BabelSense antonymSense = antonym.get(0).getSenses(lang).get(0);
				//Return its lemma, that is the antonym of the input word
				return antonymSense.getLemma();
			}
		}
		return null;
	}

	/** Negation step must be implemented **/
	public String preProcessing(String text) {
		/*File file = new File("./reviews/" + textInput);
		Scanner input = new Scanner(file);
		ArrayList<String> words = new ArrayList<String>();
		while(input.hasNext()) {
			words.add(input.next());
		}
		input.close();
		//Extract token from the input text
		List<String> words = null;
		boolean stop = false;
		String antonym = null;
		//Pattern pattern = Pattern.compile("[.!,?';:]");
		for (int i = 0; i < words.size(); i++){
			if(isNegativeKeyword(words.get(i))) {
				int negIndex = i;
				while (antonym == null && i< words.size() && !stop) {
					String token = words.get(++i);
					if (isStopToken(token)){
						stop = true;
						break;
					}
					POS pos = getPOS(token);
					if (pos == POS.ADJECTIVE){
						try {
							antonym = getAntonym(token);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(antonym != null){
					words.remove(i);
					words.remove(negIndex);
					words.add(i, antonym);
				}
				stop = false;
			}
		}

		StringBuilder output = null;
		for (String word : words){
			output.append(word).append(" ");
		}
		return output.toString();
		*/
		return text;
	}

	public static String getSentiPOS (POS pos) {
		switch (pos) {
		case NOUN: return "n";
		case ADJECTIVE: return "a";
		case VERB: return "v";
		case ADVERB: return "r";
		default: return null;
		}
	}

	public static double fixScore (double score, String root) {
		if (root.equals("positive-opinion")){
			if (score < 0) 
				score = -score;
			else if (score == 0.0)
				score = 0.2;
		}
		else if (root.equals("negative-opinion")) {
			if (score > 0)
				score = -score;
			else if (score == 0.0)
				score = -0.2;
		}
		else
			score = 0.0;
		return score;
	}
		
	public ModuleOutput postProcessing(TCOutput output) {
		int size = 0;
		double sum = 0.0;
		double score = 0.0;
		int positive = 0;
		int negative = 0;
		Object columnNames[] = new Object[]{"Lemma word","Ontology word","Polarity","Root"};
		Object rowData[][] = new Object[output.getInfo().size()][4];
		String out = "";
		DecimalFormat df = new DecimalFormat("##.##");
		df.setRoundingMode(RoundingMode.DOWN);
		try {
			SentiWordNetDemoCode sentiwordnet = new SentiWordNetDemoCode(pathToSWN);
			ArrayList<ClassifierObject> info = (ArrayList<ClassifierObject>) output.getInfo();
			double tmp = 0.0;
			for (int i = 0; i < info.size()-1; i++) {
				ClassifierObject c = info.get(i);
				ArrayList<String> tree = (ArrayList<String>) c.getOntologyTree();
				String root = tree.get(0);
				if (root.equals("positive-opinion"))
					positive++;
				else if (root.equals("negative-opinion"))
					negative++;
				tmp = fixScore(Math.floor((sentiwordnet.extract(c.getOntologyWord(), getSentiPOS(c.getPos())))*100)/100,root)*c.getNumberOfOcc();
				System.out.println("POLARITY: " + tmp);
				size += c.getNumberOfOcc();
				sum += tmp;
				rowData[i] = new Object[]{c.getLemmaWord(),c.getOntologyWord(),df.format(tmp),root};
			}
			//System.out.println("TOTAL POSITIVE: " + positive);
			//System.out.println("TOTAL NEGATIVE: " + negative);
			//System.out.println("\nAVERAGE SCORE: " + score/(positive+negative));
			//System.out.println("AVERAGE SCORE LIST SIZE: " + score/info.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		score = sum/(positive+negative);
		//return negative>positive? (score/size)-0.3 : score/size;
		//return score/size;
		//return score;
		if (score >= 0.1)
			out = "POSITIVE REVIEW";
		else
			out = "NEGATIVE REVIEW";
		//System.out.println(out);
		return new ModuleOutput(columnNames,rowData,out);
	}	
}
