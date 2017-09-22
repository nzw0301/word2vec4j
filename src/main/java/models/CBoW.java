package models;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.List;

public final class CBoW extends Word2Vec {

    public CBoW(int dimEmbeddings,
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
        int label;
        int windowSize;
        int sentenceLength;
        int posWord, negWord, targetWord;
        int contextWordPosition;
        int lastWord;
        int numWindow;
        double grad;
        double dotValue;
        int wordCount = 0;
        double alpha = startingAlpha;
        double[] wDelta = new double[dimEmbeddings];
        double[] hiddenVector = new double[dimEmbeddings];
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
                    numWindow = 0;

                    for (int i = 0; i < dimEmbeddings; i++){
                        hiddenVector[i] = 0.;
                        wDelta[i] = 0.;
                    }

                    for (int a = windowSize; a < maxWindowSize*2+1-windowSize; a++){
                        if (a == maxWindowSize) continue;
                        contextWordPosition = inputWordPosition - maxWindowSize + a;
                        if (contextWordPosition < 0) continue;
                        if (contextWordPosition >= sentenceLength) continue;

                        posWord = sentence.get(contextWordPosition);
                        for (int i = 0; i < dimEmbeddings; i++){
                            hiddenVector[i] += inputEmbeddings[posWord][i];
                        }
                        numWindow++;
                    }

                    for (int i = 0; i < dimEmbeddings; i++){
                        hiddenVector[i] /= numWindow;
                    }

                    posWord = sentence.get(inputWordPosition);
                    for (int d = 0; d < negative + 1; d++){
                        if (d == 0) { // true context word
                            label = 1;
                            targetWord = posWord;
                        } else { // negative sampling
                            negWord = negativeSampler.sample();
                            if (posWord == negWord) continue; // skip negative sampled word if it equals input word
                            label = 0;
                            targetWord = negWord;
                        }
                        dotValue = dot(hiddenVector, contextEmbeddings[targetWord], dimEmbeddings);
                        // gradient * lr
                        grad = (label - sigmoid(dotValue)) * alpha;

                        for (int i = 0; i < dimEmbeddings; i++){
                            wDelta[i] += grad * contextEmbeddings[targetWord][i];
                            this.contextEmbeddings[targetWord][i] += grad * hiddenVector[i];
                        }
                    }

                    for (int a = windowSize; a < maxWindowSize*2+1-windowSize; a++) {
                        if (a == maxWindowSize) continue;
                        contextWordPosition = inputWordPosition - maxWindowSize + a;
                        if (contextWordPosition < 0) continue;
                        if (contextWordPosition >= sentenceLength) continue;

                        posWord = sentence.get(contextWordPosition);
                        for (int i = 0; i < dimEmbeddings; i++){
                            this.inputEmbeddings[posWord][i] += wDelta[i];
                        }
                    }
                }
            }
            reader.close();
        }
    }

    public static void main(String[] args) throws IOException {
        Word2Vec w2v = new CBoW(100, "src/main/resources/text8", 0.05,
                5, 1e-4, 5, 15, 1, true, false);
         w2v.fit();
         w2v.output();
    }
}
