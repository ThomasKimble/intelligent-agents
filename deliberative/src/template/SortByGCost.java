package template;

import java.util.Comparator;

public class SortByGCost implements Comparator<StateObject> {
    public int compare(StateObject a, StateObject b)
    {
        return (int)(a.getGCost() - b.getGCost());
    }
}