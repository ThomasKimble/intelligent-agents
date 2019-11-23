package template;

import logist.topology.Topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class predictCost {
    private Topology topology;
    private HashMap<ArrayList, Double> routes;
    private int nbCities;
    private int nbRoutes;
    private double connexionCoeff;
    private double distanceMean;
    private double distanceStd;
    private ArrayList<Double> parameters = new ArrayList(Arrays.asList(-12.160929555662563, 65.30283396772586, -8.82729341890556, 0.6676127963866255, -0.027690324507348565, 0.0006319244960415956, -0.000006003492732232537));

    public predictCost(Topology topology){
        this.topology = topology;
        this.nbCities = topology.size();
        this.routes = findRoutes();
        this.nbRoutes = routes.size();
        this.connexionCoeff = (double) nbCities / nbRoutes;
        this.distanceMean = computeMeanDistances();
        this.distanceStd = computeStdDistances();
    }

    private HashMap findRoutes(){
        HashMap <ArrayList, Double> routes = new HashMap<ArrayList, Double>();
        for(Topology.City city: topology.cities()){
            for (Topology.City neighborsCity :city.neighbors()){
                ArrayList<String> ID = new ArrayList<String>();
                ArrayList<String> IDbis = new ArrayList<String>();
                ID.add(city.name);
                IDbis.add(neighborsCity.name);
                ID.add(neighborsCity.name);
                IDbis.add(city.name);
                if (!routes.containsKey(ID) && !routes.containsKey(IDbis)){
                    routes.put(new ArrayList<String>(ID), city.distanceTo(neighborsCity));
                }
            }
        }
        return routes;
    }

    private Double computeMeanDistances(){
        double total = 0.0;
        for (Double v : routes.values()) {
            total += v;
        }
        return total / nbRoutes;
    }

    private Double computeStdDistances(){
        double total = 0.0;
        for (Double v : routes.values()) {
            total += Math.pow((v - distanceMean), 2);
        }
        return Math.sqrt(total / nbRoutes);
    }

    public ArrayList getEstimationCost(int time, boolean getNormalized){
        ArrayList<Double> estimation = new ArrayList<Double>();
        for(int i=1; i<=time; i++){
            double y_normalized = 0.0;
            for (int j=0; j<parameters.size(); j++){
                y_normalized += Math.pow(i, j) * parameters.get(j);
            }
            if (!getNormalized) {
                estimation.add(y_normalized*connexionCoeff*distanceStd + distanceMean);
            } else{
                estimation.add(y_normalized);
            }
        }
        return estimation;
    }

    public void setBiais(double value, boolean alreadyNormalized){
        if (!alreadyNormalized){
            value = (value - distanceMean) / (connexionCoeff * distanceStd);
        }
        this.parameters.set(0, value);
    }


}
