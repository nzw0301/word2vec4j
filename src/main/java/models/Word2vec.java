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
    private final int MAX_SIGMOID = 6;
    private final int SIGMOID_TABLE_SIZE = 1000;
    private final double[] t_sigmoid = new double[SIGMOID_TABLE_SIZE];
    private final double power = 0.75;
    private final int size;
    private final String train;
    private final String output;
    private final double startingAlpha;
    private final int window;
    private final double sample;
    private final int negative;
    private final int minCount;
    private final int numVocab;
    private final int numIteration = 1;
    private final Vocab vocab;
    private final int trainWords;
    private final Random rand = new Random();
    private final NegativeSampler negativeSampler;
    private double[][] W;
    private double[][] C;

    public Word2vec(int size,
                    String train,
                    double alpha,
                    int window,
                    double sample,
                    int negative,
                    int minCount,
                    boolean useAlias4NS) throws IOException {

        this.size = size;
        this.train = train;
        this.startingAlpha = alpha;
        this.window = window;
        this.sample = sample;
        this.negative = negative;
        this.minCount = minCount;
        this.rand.setSeed(7);

        this.output = train + ".vec";
        this.vocab = new Vocab(this.train, this.minCount, this.sample);
        this.numVocab = this.vocab.getVocabSize();
        this.trainWords = this.vocab.getTrainWords();

        this.initSigmoidTable();
        initNet();

        if(useAlias4NS){
            this.negativeSampler = new AliasNegativeSampler(this.vocab, this.power);
        }else{
            this.negativeSampler = new ArrayNegativeSampler(this.vocab, this.power);
        }
    }


    private void initSigmoidTable(){
        double x;
        for (int i = 0; i < this.SIGMOID_TABLE_SIZE; i++){
            x = ((double) i / SIGMOID_TABLE_SIZE * 2 - 1) * MAX_SIGMOID;
            this.t_sigmoid[i] = 1. / (Math.exp(-x)+1.);
        }
    }

    private void initNet(){
        this.C = new double[this.numVocab][this.size];
        this.W = new double[this.numVocab][this.size];

        for (int i = 0; i < this.numVocab; i++){
            for (int j = 0; j < this.size; j++) {
                this.W[i][j] = (this.rand.nextDouble()-0.5) / this.size;
            }
        }
    }

    @Override
    public void fit() throws IOException{
        int target;
        int label;
        int b;
        int wordCount = 0;
        int lastWordCount = 0;
        int wordCountActual = 0;
        int sentenceLength;
        int w, c;
        int lastWord;
        double grad;
        double alpha = this.startingAlpha;
        double[] wDelta = new double[this.size];
        List<Integer> sentence = new ArrayList<>();


        for (int t = 0; t < this.numIteration; t++){
            PushbackReader reader = new PushbackReader(new FileReader(this.train));

            while(true) {
                // update learning rate
                if (wordCount-lastWordCount > 10000){
                    wordCountActual += wordCount - lastWordCount;
                    lastWordCount = wordCount;

                    alpha = Math.max(
                            this.startingAlpha * (1 - wordCountActual / (double)(this.trainWords + 1)),
                            this.startingAlpha * 0.0001
                    );
                    System.out.printf("%.5f %.5f", (double)wordCount/this.trainWords, alpha);
                }

                // EOF check
                if ((lastWord = reader.read()) == -1) break;
                reader.unread(lastWord);

                // get sentence (list of word id)
                wordCount += this.vocab.readLine(reader, sentence, this.rand);
                sentenceLength = sentence.size();

                if (sentenceLength == 0) continue;

                for (int sentencePosition = 0; sentencePosition < sentenceLength; sentencePosition++){
                    b = this.rand.nextInt(this.window);
                    w = sentence.get(sentencePosition);

                    for (int a = b; a < this.window*2+1-b; a++){
                        if (a == this.window) continue;
                        c = sentencePosition - this.window + a;
                        if (c < 0) continue;
                        if (c >= sentenceLength) continue;

                        for (int i = 0; i < this.size; i++){
                            wDelta[i] = 0.;
                        }

                        for (int d = 0; d < this.negative + 1; d++){
                            if (d == 0) { // true context word
                                target = sentence.get(c);
                                label = 1;
                            } else { // negative sampling
                                target = this.negativeSampler.sample();
                                if (target == w)continue; // skip negative sampled word if it equals input word
                                label = 0;
                            }

                            // dot
                            double f = 0;
                            for (int i = 0; i < this.size; i++){
                                f += this.W[w][i]*this.C[target][i];
                            }

                            // gradient
                            if (f > this.MAX_SIGMOID){
                                grad = (label - 1) * alpha;
                            }else if (f < -this.MAX_SIGMOID){
                                grad = label * alpha;
                            }else {
                                grad = (label - t_sigmoid[(int)((f + MAX_SIGMOID) * (SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2))])*alpha;
                            }

                            for (int i = 0; i < this.size; i++){
                                wDelta[i]         += grad * this.C[target][i];
                                this.C[target][i] += grad * this.W[w][i];
                            }
                        }
                        for (int i = 0; i < this.size; i++){
                            this.W[w][i] += wDelta[i];
                        }
                    }
                }
            }
        }
    }

    @Override
    public void output() throws IOException{
        PrintWriter writer = new PrintWriter(this.output);
        writer.println(this.numVocab + " " + this.size);
        double[] vec;
        for(int wordId = 0; wordId < numVocab; wordId++){
            writer.print(this.vocab.getWord(wordId));
            vec = this.W[wordId];
            for(double v: vec){
                writer.print(" " + v);
            }
            writer.println();
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        Word2vec w2v = new Word2vec(100, "src/main/resources/text8",
                         0.025, 5, 1e-3, 5, 5, true);
         w2v.fit();
         w2v.output();
    }
}
