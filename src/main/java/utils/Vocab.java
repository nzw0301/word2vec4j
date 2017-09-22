package utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Comparator;
import java.util.Collections;

public class Vocab {
    private final int MAX_SENTENCE_LENGTH = 1000;
    private Map<String, Integer> word2index = new HashMap<>();
    private List<Word> index2word = new ArrayList<>();
    private final int minCount;
    private int numTrainWords;
    private final int numVocab;
    private final String EOS = "</s>";
    private ArrayList<Double> discardTable = new ArrayList<>();


    public Vocab(String trainFileName, int minCount) throws IllegalArgumentException, IOException {
        this.minCount = minCount;
        if(minCount <= 0) {
            throw new IllegalArgumentException("Able to set only minCount positive value: " + minCount);
        }
        PushbackReader reader;
        try {
            reader = new PushbackReader(new FileReader(trainFileName));
        }catch (IOException e){
            throw new IllegalArgumentException("Unable to load " + trainFileName, e);
        }

        System.out.println("Loading words: ");
        StringBuilder word = new StringBuilder();
        while (readWord(reader, word)){
            addVocab(word.toString());
        }

        reader.close();
        sortVocab();
        this.numVocab = index2word.size();
        System.out.println("The number of vocab is " + numVocab);
        System.out.println("The number of words is " + numTrainWords);
    }

    public Vocab(String fname, int minCount, double sample) throws IllegalArgumentException, IOException {
        this(fname, minCount);
        this.initDiscardTable(sample);
    }

    public boolean readWord(PushbackReader reader, StringBuilder word) throws IOException {
        word.setLength(0);
        int character;

        while ((character = reader.read()) != -1) {
            if (character == '\t' || character  == ' ' || character == '\n'){
                if(word.length() == 0){
                    if (character == '\n'){
                        word.append(EOS);
                        return true;
                    }
                    continue;
                }else {
                    if (character == '\n'){
                        reader.unread('\n');
                    }
                    return true;
                }
            }
            word.append((char)character);
        }
        return (word.length() != 0);
    }


    public int readLine(PushbackReader reader, List<Integer> words, Random rand) throws IOException {
        words.clear();
        StringBuilder word = new StringBuilder();
        String token;
        int wordId;
        int numProcessedTokens = 0;

        while(this.readWord(reader, word)){
            token = word.toString();
            wordId = this.getWordId(token);
            if (wordId == -1) continue;
            numProcessedTokens++;
            if (discardTable.get(wordId) < rand.nextDouble()) continue; // skip sub-sampled word
            words.add(wordId);

            if (token.equals(EOS)) break;
            if (words.size() > MAX_SENTENCE_LENGTH) break;
        }

        return numProcessedTokens;
    }

    private void sortVocab(){
        List<Word> words = new ArrayList<>(index2word);
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                return Integer.compare(o2.getFreq(), o1.getFreq());
            }
        });

        this.index2word = words;
        for (int i = 0; i < index2word.size(); i++){
            if (minCount > index2word.get(i).getFreq()){
                for (int j = index2word.size()-1; i <= j; j--){
                    word2index.remove(index2word.get(j).getWord());
                    index2word.remove(j);
                }
                break;
            }else{
                word2index.replace(index2word.get(i).getWord(), i);
                numTrainWords += index2word.get(i).getFreq();
            }
        }
    }

    private void addVocab(String word){
        int wordId = word2index.getOrDefault(word, word2index.size());
        if (wordId == word2index.size()){
            index2word.add(new Word(word));
            word2index.put(word, wordId);
        }else {
            index2word.get(wordId).incrementFreq();
        }
    }

    private void initDiscardTable(double sample){
        double f;
        for(int wordId = 0; wordId < numVocab; wordId++){
            f = (double) getFreq(wordId) / numTrainWords;
            discardTable.add(Math.sqrt(sample/f) + sample/f
            );
        }
    }

    public int getNumTrainWords(){ return numTrainWords; }

    public int getNumVocab(){ return numVocab; }

    public int getWordId(String word){
        return word2index.getOrDefault(word, -1);
    }

    public int getFreq(int wordId){
        return index2word.get(wordId).getFreq();
    }

    public String getWord(int wordId){
        if(wordId >= 0 && wordId < word2index.size()){
            return index2word.get(wordId).getWord();
        }else{
            return null;
        }
    }
}
