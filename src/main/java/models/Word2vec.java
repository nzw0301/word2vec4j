package models;

import utils.AliasNegativeSampler;
import utils.ArrayNegativeSampler;
import utils.NegativeSampler;
import utils.Vocab;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public abstract class Word2Vec {
    private final static double power = 0.75;
    private final static int MAX_SIGMOID = 6;
    private final static int SIGMOID_TABLE_SIZE = 1000;
    private final double[] sigmoidTable = new double[SIGMOID_TABLE_SIZE];
    protected final int dimEmbeddings;
    protected final String trainFileName;
    private final String outputFileName;
    protected final double startingAlpha;
    protected final int maxWindowSize;
    protected final int negative;
    protected final int numVocab;
    protected final int numIteration;
    protected final Vocab vocab;
    private final int numTrainWords;
    protected final static Random rand = new Random();
    protected final NegativeSampler negativeSampler;
    protected double[][] inputEmbeddings;
    protected double[][] contextEmbeddings;
    private int lastWordCount;

    public Word2Vec(int dimEmbeddings,
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

        this.outputFileName = trainFileName + ".vec";
        this.vocab = new Vocab(trainFileName, minCount, sample);
        this.numVocab = vocab.getNumVocab();
        this.numTrainWords = vocab.getNumTrainWords();
        this.numIteration = iter;

        this.initSigmoidTable();
        initNet(shareHidden);

        if (useAlias4NS) {
                this.negativeSampler = new AliasNegativeSampler(vocab, power);
        } else {
                this.negativeSampler = new ArrayNegativeSampler(vocab, power);
        }

        this.lastWordCount = 0;
    }

    private void initSigmoidTable(){
        double x;
        for (int i = 0; i < SIGMOID_TABLE_SIZE; i++){
            x = ((double) i / SIGMOID_TABLE_SIZE * 2 - 1) * MAX_SIGMOID;
            this.sigmoidTable[i] = 1. / (Math.exp(-x)+1.);
        }
    }

    private void initNet(boolean shareHidden){
        this.inputEmbeddings = new double[numVocab][dimEmbeddings];

        for (int i = 0; i < numVocab; i++){
            for (int j = 0; j < dimEmbeddings; j++) {
                this.inputEmbeddings[i][j] = (rand.nextDouble()-0.5) / dimEmbeddings;
            }
        }

        if (shareHidden){
            this.contextEmbeddings = inputEmbeddings;
        }else{
            this.contextEmbeddings = new double[numVocab][dimEmbeddings];
        }
    }

    protected static double dot(final double[] v1, final double[] v2, final int dim){
        double dotValue = 0.;
        for (int i = 0; i < dim; i++){
            dotValue += v1[i]*v2[i];
        }
        return dotValue;
    }

    protected double sigmoid(double v){
        if (v > MAX_SIGMOID){
            return 1.;
        }else if (v < -MAX_SIGMOID){
            return 0.;
        }else {
            return sigmoidTable[(int) ((v + MAX_SIGMOID) * (SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2))];
        }
    }

    protected double updateLearingRate(final int wordCount, final double alpha){
        if (wordCount-lastWordCount > 10000){
            this.lastWordCount = wordCount;
            System.out.printf("%.7f %.7f", (double) wordCount/(numTrainWords*numIteration + 1), alpha);

            return startingAlpha * Math.max(
                    (1 - wordCount / (double) (numIteration * numTrainWords + 1)),
                    0.0001
            );

        }else{
            return alpha;
        }
    }

    protected abstract void fit() throws IOException;

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
}
