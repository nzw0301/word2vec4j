package utils;

import java.util.ArrayList;
import java.util.List;


public class AliasNegativeSampler implements NegativeSampler{
    private AliasSampler alias;


    public AliasNegativeSampler(Vocab vocab, double power){
        int numVocab = vocab.getVocabSize();
        this.alias = new AliasSampler(numVocab);
        List<Double> prob = new ArrayList<>(numVocab);

        for (int i = 0; i < numVocab; i++){
            prob.add(Math.pow(vocab.getFreq(i), power));
        }

        this.alias.buildSamplingTable(prob);
    }

    public int sample(){
        return this.alias.sample();
    }
}
