package models;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import utils.AliasNegativeSampler;
import utils.ArrayNegativeSampler;
import utils.NegativeSampler;
import utils.Vocab;


abstract class Model{
    protected abstract void fit() throws IOException;
    protected abstract void output() throws IOException;
}

public final class Word2vec extends Model {
    private final static double power = 0.75;
    private final static int MAX_SIGMOID = 6;
    private final static int SIGMOID_TABLE_SIZE = 1000;
    private final double[] sigmoidTable = new double[SIGMOID_TABLE_SIZE];
    private final int dimEmbeddings;
    private final String trainFileName;
    private final String outputFileName;
    private final double startingAlpha;
    private final int maxWindowSize;
    private final int negative;
    private final int numVocab;
    private final int numIteration;
    private final Vocab vocab;
    private final int numTrainWords;
    private final static Random rand = new Random();
    private final NegativeSampler negativeSampler;
    private double[][] inputEmbeddings;
    private double[][] contextEmbeddings;

    public Word2vec(int dimEmbeddings,
                    String trainFileName,
                    double alpha,
                    int maxWindowSize,
                    final double sample,
                    int negative,
                    int minCount,
                    int iter,
                    boolean useAlias4NS,
                    boolean shareHidden) throws IOException {

        this.dimEmbeddings = dimEmbeddings;
        this.trainFileName = trainFileName;
        this.startingAlpha = alpha;
        this.maxWindowSize = maxWindowSize;
        this.negative = negative;
        rand.setSeed(7);

        this.outputFileName = this.trainFileName + ".vec";
        this.vocab = new Vocab(this.trainFileName, minCount, sample);
        this.numVocab = this.vocab.getNumVocab();
        this.numTrainWords = this.vocab.getNumTrainWords();
        this.numIteration = iter;

        this.initSigmoidTable();
        initNet(shareHidden);

        if(useAlias4NS){
            this.negativeSampler = new AliasNegativeSampler(this.vocab, power);
        }else{
            this.negativeSampler = new ArrayNegativeSampler(this.vocab, power);
        }
    }


    private void initSigmoidTable(){
        double x;
        for (int i = 0; i < SIGMOID_TABLE_SIZE; i++){
            x = ((double) i / SIGMOID_TABLE_SIZE * 2 - 1) * MAX_SIGMOID;
            this.sigmoidTable[i] = 1. / (Math.exp(-x)+1.);
        }
    }

    private void initNet(boolean shareHidden){
        this.inputEmbeddings = new double[this.numVocab][this.dimEmbeddings];

        for (int i = 0; i < this.numVocab; i++){
            for (int j = 0; j < this.dimEmbeddings; j++) {
                this.inputEmbeddings[i][j] = (rand.nextDouble()-0.5) / this.dimEmbeddings;
            }
        }

        if (shareHidden){
            this.contextEmbeddings = inputEmbeddings;
        }else{
            this.contextEmbeddings = new double[this.numVocab][this.dimEmbeddings];
        }
    }

    @Override
    public void fit() throws IOException{
        int targetWord;
        int label;
        int windowSize;
        int wordCount = 0;
        int lastWordCount = 0;
        int wordCountActual = 0;
        int sentenceLength;
        int inputWord;
        int contextWordPosition;
        int lastWord;
        double grad;
        double dotValue;
        double alpha = this.startingAlpha;
        double[] wDelta = new double[this.dimEmbeddings];
        List<Integer> sentence = new ArrayList<>();

        for (int t = 0; t < numIteration; t++){
            PushbackReader reader = new PushbackReader(new FileReader(this.trainFileName));

            while(true) {
                // update learning rate
                if (wordCount-lastWordCount > 10000){
                    wordCountActual += wordCount - lastWordCount;
                    lastWordCount = wordCount;

                    alpha = Math.max(
                            this.startingAlpha * (1 - wordCountActual / (double)(numIteration * numTrainWords + 1)),
                            this.startingAlpha * 0.0001
                    );
                    System.out.printf("%.5f %.5f", (double)wordCount/(this.numTrainWords*numIteration + 1), alpha);
                }

                // EOF check
                if ((lastWord = reader.read()) == -1) break;
                reader.unread(lastWord);

                // get sentence (list of word id)
                wordCount += this.vocab.readLine(reader, sentence, rand);
                sentenceLength = sentence.size();

                if (sentenceLength == 0) continue;

                for (int inputWordPosition = 0; inputWordPosition < sentenceLength; inputWordPosition++){
                    windowSize = rand.nextInt(this.maxWindowSize);
                    inputWord = sentence.get(inputWordPosition);

                    for (int a = windowSize; a < this.maxWindowSize*2+1-windowSize; a++){
                        if (a == this.maxWindowSize) continue;
                        contextWordPosition = inputWordPosition - this.maxWindowSize + a;
                        if (contextWordPosition < 0) continue;
                        if (contextWordPosition >= sentenceLength) continue;

                        for (int i = 0; i < this.dimEmbeddings; i++){
                            wDelta[i] = 0.;
                        }

                        for (int d = 0; d < this.negative + 1; d++){
                            if (d == 0) { // true context word
                                targetWord = sentence.get(contextWordPosition);
                                label = 1;
                            } else { // negative sampling
                                targetWord = this.negativeSampler.sample();
                                if (targetWord == inputWord) continue; // skip negative sampled word if it equals input word
                                label = 0;
                            }

                            dotValue = dot(this.inputEmbeddings[inputWord], this.contextEmbeddings[targetWord], this.dimEmbeddings);

                            // gradient
                            grad = (label - sigmoid(dotValue)) * alpha;

                            for (int i = 0; i < this.dimEmbeddings; i++){
                                wDelta[i] += grad * this.contextEmbeddings[targetWord][i];
                                this.contextEmbeddings[targetWord][i] += grad * this.inputEmbeddings[inputWord][i];
                            }
                        }
                        for (int i = 0; i < this.dimEmbeddings; i++){
                            this.inputEmbeddings[inputWord][i] += wDelta[i];
                        }
                    }
                }
            }
            reader.close();
        }
    }

    private static double dot(final double[] v1, final double[] v2, final int dim){
        double dotValue = 0.;
        for (int i = 0; i < dim; i++){
            dotValue += v1[i]*v2[i];
        }
        return dotValue;
    }

    private double sigmoid(double v){
        if (v > MAX_SIGMOID){
            return 1.;
        }else if (v < -MAX_SIGMOID){
            return 0.;
        }else {
            return sigmoidTable[(int)((v + MAX_SIGMOID) * (SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2))];
        }
    }

    private static class Trainer implements Runnable {
        // TODO: implementation of distributed learning
        Trainer(){}

        @Override
        public void run(){ }
    }

    @Override
    public void output() throws IOException{
        PrintWriter writer = new PrintWriter(outputFileName);
        writer.println(numVocab + " " + dimEmbeddings);
        double[] vec;
        for(int wordId = 0; wordId < numVocab; wordId++){
            writer.print(vocab.getWord(wordId));
            vec = inputEmbeddings[wordId];
            for(double v: vec){
                writer.print(" " + v);
            }
            writer.println();
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        Word2vec w2v = new Word2vec(100, "src/main/resources/text8", 0.025,
                5, 1e-4, 5, 15, 1,true, true);
         w2v.fit();
         w2v.output();
    }
}
