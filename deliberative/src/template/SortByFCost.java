package template;

import java.util.Comparator;

public class SortByFCost implements Comparator<StateObject> {
    public int compare(StateObject a, StateObject b)
    {
        return (int)(a.getFCost() - b.getFCost());
    }
}
