package models;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

public final class SkipGram extends Word2Vec {

    public SkipGram(int dimEmbeddings,
                    String trainFileName,
                    double alpha,
                    int maxWindowSize,
                    final double sample,
                    int negative,
                    int minCount,
                    int iter,
                    boolean useAlias4NS,
                    boolean shareHidden) throws IOException {

        super(dimEmbeddings, trainFileName, alpha, maxWindowSize, sample, negative, minCount, iter, useAlias4NS, shareHidden);
    }


    @Override
    public void fit() throws IOException{
        int targetWord;
        int label;
        int windowSize;
        int wordCount = 0;
        int sentenceLength;
        int inputWord;
        int contextWordPosition;
        int lastWord;
        double grad;
        double dotValue;
        double alpha = startingAlpha;
        double[] wDelta = new double[dimEmbeddings];
        List<Integer> sentence = new ArrayList<>();

        for (int t = 0; t < numIteration; t++){
            PushbackReader reader = new PushbackReader(new FileReader(trainFileName));

            while(true) {
                alpha = updateLearingRate(wordCount, alpha);

                // EOF check
                if ((lastWord = reader.read()) == -1) break;
                reader.unread(lastWord);

                // get sentence (list of word id)
                wordCount += vocab.readLine(reader, sentence, rand);
                sentenceLength = sentence.size();

                if (sentenceLength == 0) continue;

                for (int inputWordPosition = 0; inputWordPosition < sentenceLength; inputWordPosition++){
                    windowSize = rand.nextInt(maxWindowSize);
                    inputWord = sentence.get(inputWordPosition);

                    for (int a = windowSize; a < maxWindowSize*2+1-windowSize; a++){
                        if (a == maxWindowSize) continue;
                        contextWordPosition = inputWordPosition - maxWindowSize + a;
                        if (contextWordPosition < 0) continue;
                        if (contextWordPosition >= sentenceLength) continue;

                        for (int i = 0; i < dimEmbeddings; i++){
                            wDelta[i] = 0.;
                        }

                        for (int d = 0; d < negative + 1; d++){
                            if (d == 0) { // true context word
                                targetWord = sentence.get(contextWordPosition);
                                label = 1;
                            } else { // negative sampling
                                targetWord = negativeSampler.sample();
                                if (targetWord == inputWord) continue; // skip negative sampled word if it equals input word
                                label = 0;
                            }

                            dotValue = dot(inputEmbeddings[inputWord], contextEmbeddings[targetWord], dimEmbeddings);

                            // gradient
                            grad = (label - sigmoid(dotValue)) * alpha;

                            for (int i = 0; i < dimEmbeddings; i++){
                                wDelta[i] += grad * contextEmbeddings[targetWord][i];
                                this.contextEmbeddings[targetWord][i] += grad * inputEmbeddings[inputWord][i];
                            }
                        }
                        for (int i = 0; i < dimEmbeddings; i++){
                            this.inputEmbeddings[inputWord][i] += wDelta[i];
                        }
                    }
                }
            }
            reader.close();
        }
    }

    private static class Trainer implements Runnable {
        // TODO: implementation of distributed learning
        Trainer(){}

        @Override
        public void run(){ }
    }

    public static void main(String[] args) throws IOException {
        Word2Vec w2v = new SkipGram(100, "src/main/resources/text8", 0.025,
                5, 1e-4, 15, 15, 1, true, false);
         w2v.fit();
         w2v.output();
    }
}
