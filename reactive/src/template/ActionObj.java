package template;
import logist.topology.Topology.City;

public class ActionObj {
    private boolean takeDelivery;
    private City nextCity;

    public ActionObj() {
        this.takeDelivery = true;
        this.nextCity = null;
    }

    public ActionObj(City nextCity) {
        this.takeDelivery = false;
        this.nextCity = nextCity;
    }

    //Getter
    public boolean isTakeDelivery() {
        return takeDelivery;
    }

    public City getNextCity() {
        return nextCity;
    }
}
