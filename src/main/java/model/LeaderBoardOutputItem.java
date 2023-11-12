package model;

import lombok.Data;

import java.time.Instant;
import java.util.Comparator;

@Data
public class LeaderBoardOutputItem {
    private String repoFullName;
    private int score;
    private String lastUpdatedTime;

    public LeaderBoardOutputItem(String repoFullName, int score) {
       this.score = score;
       this.repoFullName = repoFullName;
    }

    public LeaderBoardOutputItem(String repoFullName, String lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
        this.repoFullName = repoFullName;
        this.score = -1;
    }

    @Override
    public String toString() {
        if (score == -1) {
            return "[" + "\"" + repoFullName + "\"" + "," +  "\"" + Instant.ofEpochMilli((long) Double.parseDouble(lastUpdatedTime)) + "\"" + "]";
        }
        return "[" + "\"" + repoFullName + "\"" + "," + score + "]";
    }
}
