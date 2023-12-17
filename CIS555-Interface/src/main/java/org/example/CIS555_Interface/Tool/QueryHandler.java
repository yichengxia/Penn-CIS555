package org.example.CIS555_Interface.Tool;

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.stemmer.PorterStemmer;

public class QueryHandler {
    public static String[] normalizeInputStr(String input) {
    	
        StringBuilder sb = new StringBuilder();
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (StopWord.punctuations.contains(c)) {
                continue;
            }

            // not a letter or digit or space
            if (!Character.isLetter(c) && !Character.isDigit(c) && !Character.isSpaceChar(c)) {
                continue;
            }

            // if none-English letter
            if (Character.isLetter(c)) {
                if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
                    continue;
                }
            }

            // convert to lower case
            if (Character.isUpperCase(c)) {
                c = Character.toLowerCase(c);
            }

            sb.append(c);
        }
        return sb.toString().split("\\s+");
    }
    
    public static List<String> stemTheWords(String[] strArr, PorterStemmer stemmer) {
    	List<String> stemmedWords = new ArrayList<>();
        // stemming
        for (String word: strArr) {

            // remove stop words before stemming
            if (StopWord.stopWords.contains(word)) {
                continue;
            }
            String stemmedWord = word;
            
            // remove stop words after stemming
            if (StopWord.stopWords.contains(stemmedWord)) {
                continue;
            }
            stemmedWords.add(stemmedWord);
        }
        return stemmedWords;
    }
}
