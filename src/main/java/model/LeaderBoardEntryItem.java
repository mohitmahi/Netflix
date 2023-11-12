package model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Builder
@Data
public class LeaderBoardEntryItem {
    private String setName;
    private String key;
    private String score;

    public Collection<? extends String> getAsOrderedList() {
        List<String> list = new ArrayList<>(2);
        list.add(score);
        list.add(key);
        return list;
    }
}
