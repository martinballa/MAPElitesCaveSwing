package ntuple;

/**
 * Created by Simon Lucas on 22/06/2017.
 */
public class ScoredVec {
    public int[] p;
    public double score;

    public ScoredVec(int[] p, double score) {
        this.p = p;
        this.score = score;
    }

    public ScoredVec(int[] p) {
        this.p = p;
    }

    public ScoredVec setScore(double score) {
        this.score = score;
        return this;
    }
}
