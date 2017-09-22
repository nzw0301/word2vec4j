package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArrayNegativeSampler implements NegativeSampler{
    private final int NEGATIVE_TABLE_SIZE = 10000000;
    private final List<Integer> negativeTable;
    private final Random rand = new Random();

    public ArrayNegativeSampler(Vocab vocab, double power){
        this.negativeTable = new ArrayList<>(NEGATIVE_TABLE_SIZE);
        final int numVocab = vocab.getNumVocab();

        double denom = 0.;
        for (int i = 0; i < numVocab; i++){
            denom += Math.pow(vocab.getFreq(i), power);
        }

        double numerator;
        int negativeTableIndex = 0;

        for (int wordId = 0; wordId < numVocab; wordId++){
            numerator = Math.pow(vocab.getFreq(wordId), power);
            for (int j = 0; j < numerator*NEGATIVE_TABLE_SIZE/denom; j++){
                negativeTable.add(negativeTableIndex, wordId);
                negativeTableIndex++;
            }
        }
    }

    public int sample(){
        return negativeTable.get(rand.nextInt(NEGATIVE_TABLE_SIZE));
    }
}

