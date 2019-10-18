package template;
import logist.topology.Topology.City;

public class StateObj {
    private City currentCity;
    private City destineCity;
    private boolean hasPackage;

    public StateObj(City currentCity) {
        this.currentCity = currentCity;
        this.destineCity = null;
        this.hasPackage = false;
    }

    public StateObj(City currentCity, City destineCity) {
        this.currentCity = currentCity;
        this.destineCity = destineCity;
        this.hasPackage = true;
    }

    //Getter
    public City getCurrentCity() {
        return currentCity;
    }

    public City getDestineCity() {
        return destineCity;
    }

    public boolean isHasPackage() {
        return hasPackage;
    }
}
