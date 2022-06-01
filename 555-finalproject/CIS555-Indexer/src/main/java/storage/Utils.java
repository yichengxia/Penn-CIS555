package storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import opennlp.tools.stemmer.PorterStemmer;

public class Utils {
	public static String[] stopWordsArray = { "a", "about", "above", "across", "after", "afterwards", "again",
			"against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among",
			"amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway",
			"anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes", "becoming",
			"been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond",
			"bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "computer", "con", "could",
			"couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight",
			"either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone",
			"everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five",
			"for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give",
			"go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein",
			"hereupon", "hers", "herse", "him", "himse", "his", "how", "however", "hundred", "i", "ie", "if", "in",
			"inc", "indeed", "interest", "into", "is", "it", "its", "itse", "keep", "last", "latter", "latterly",
			"least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more",
			"moreover", "most", "mostly", "move", "much", "must", "my", "myse", "name", "namely", "neither", "never",
			"nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere",
			"of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our",
			"ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re",
			"same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side",
			"since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime",
			"sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them",
			"themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon",
			"these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through", "throughout",
			"thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un",
			"under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever",
			"when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon",
			"wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why",
			"will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", };

	public static Set<Character> punctuations = new HashSet<>();

	static {
		char[] puncs = { ',', '.', '?', '!', ':', ';', '"', '\\', '\'', '(', ')', '[', ']', '{', '}', '#', '&', '+',
				'-', '<', '>', '@' };
		for (char c : puncs) {
			punctuations.add(c);
		}
	}

	public static HashSet<String> stopWords = new HashSet<>(Arrays.asList(stopWordsArray));
	
	public static boolean shouldAddCount(String curTok, HashSet<String> stops) {
		String curTok_low = curTok.toLowerCase();
		if (!stops.contains(curTok_low) && curTok_low.matches("^[a-zA-Z0-9]*$")) {
			return true;
		}
		return false;
	}
	
	public static String[] normalizeInputStr(String input) {

		StringBuilder sb = new StringBuilder();
		int len = input.length();
		for (int i = 0; i < len; i++) {
			char c = input.charAt(i);
			if (punctuations.contains(c)) {
				continue;
			}

			if (!Character.isLetter(c) && !Character.isDigit(c) && !Character.isSpaceChar(c)) {
				continue;
			}

			if (Character.isLetter(c)) {
				if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
					continue;
				}
			}

			if (Character.isUpperCase(c)) {
				c = Character.toLowerCase(c);
			}

			sb.append(c);
		}
		return sb.toString().split("\\s+");
	}
	
	public static Elements getTextItems(Document doc_content) {
		
		Elements textItems = doc_content.getElementsByTag("p");
		textItems.addAll(doc_content.getElementsByTag("h1"));
		textItems.addAll(doc_content.getElementsByTag("h2"));
		textItems.addAll(doc_content.getElementsByTag("h3"));
		textItems.addAll(doc_content.getElementsByTag("h4"));
		textItems.addAll(doc_content.getElementsByTag("h5"));
		textItems.addAll(doc_content.getElementsByTag("h6"));
		
		return textItems;
	}

	public static List<String> stemTheWords(String[] strArr, PorterStemmer stemmer) {
		List<String> stemmedWords = new ArrayList<>();
		// stemming
		for (String word : strArr) {

			// remove stop words before stemming
			if (stopWords.contains(word)) {
				continue;
			}

			String stemmedWord = word;
			if (stopWords.contains(stemmedWord)) {
				continue;
			}
			stemmedWords.add(stemmedWord);
		}
		return stemmedWords;
	}
}
