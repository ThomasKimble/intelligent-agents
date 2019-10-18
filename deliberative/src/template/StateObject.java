package template;

import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StateObject{
    private Vehicle vehicle;
    private City city;
    private int weightVehicle;
    private int freeWeightVehicle;
    private TaskSet tasks2do;
    private TaskSet tasksBeingDone;
    private boolean isGoal;
    private ArrayList<Action> actions2state;
    private double gCost;
    private double hCost;
    private double fCost;

    public StateObject(Vehicle vehicle, int currentWeightVehicle, int freeWeightVehicle,
                       City currentCity, ArrayList<Action> actionList,
                       TaskSet availableTasks, TaskSet transportedTasks,
                       double gCost) {
        this.vehicle = vehicle;
        this.weightVehicle = currentWeightVehicle;
        this.freeWeightVehicle = freeWeightVehicle;
        this.city = currentCity;
        this.tasks2do = availableTasks;
        this.tasksBeingDone = transportedTasks;
        this.actions2state = actionList;
        this.isGoal = this.tasks2do.isEmpty() && this.tasksBeingDone.isEmpty();
        this.gCost = gCost;

        // Heuristic computation
        double hCostMax = 0;
        for (Task task : tasks2do) {
            double hCostTempo = (currentCity.distanceTo(task.pickupCity) + task.pathLength())*vehicle.costPerKm();
            if(hCostTempo > hCostMax) {
                hCostMax = hCostTempo;
            }
        }
        for(Task task: tasksBeingDone){
            double hCostTempo = currentCity.distanceTo(task.deliveryCity)*vehicle.costPerKm();
            if(hCostTempo > hCostMax) {
                hCostMax = hCostTempo;
            }
        }
        this.hCost = hCostMax;
        this.fCost = hCost + gCost;
    }


    public List<StateObject> getNextStates(){
        LinkedList<StateObject> nextStates = new LinkedList<StateObject>();

        // Pickup action
        for(Task taskAvailable : tasks2do) {
            if(taskAvailable.weight > freeWeightVehicle) //Overweight
                // --> Next taskAvailable
                continue;

            TaskSet nextAvailable = tasks2do.clone();
            nextAvailable.remove(taskAvailable);
            TaskSet nextTransported = tasksBeingDone.clone();
            nextTransported.add(taskAvailable);

            City nextCity = taskAvailable.pickupCity;

            double nextCost = gCost + city.distanceTo(nextCity)*vehicle.costPerKm();
            int nextWeightVehicle = weightVehicle + taskAvailable.weight;
            int nextFreeWeightVehicle = freeWeightVehicle - taskAvailable.weight;

            ArrayList<Action> actions2nextState = new ArrayList<Action>(actions2state);
            if (city == nextCity){
                actions2nextState.add(new Pickup(taskAvailable));
            } else {
                for (City city : city.pathTo(nextCity)) {
                    actions2nextState.add(new Move(city));
                }
                actions2nextState.add(new Pickup(taskAvailable));
            }
            nextStates.add(new StateObject(vehicle, nextWeightVehicle, nextFreeWeightVehicle, nextCity, actions2nextState, nextAvailable, nextTransported, nextCost));
        }

        // Deliver action
        for (Task taskTransported : tasksBeingDone) {
            TaskSet nextTransported = tasksBeingDone.clone();
            nextTransported.remove(taskTransported);

            City nextCity = taskTransported.deliveryCity;

            double nextCost = gCost + city.distanceTo(nextCity)*vehicle.costPerKm();
            int nextWeightVehicle = weightVehicle - taskTransported.weight;
            int nextFreeWeightVehicle = freeWeightVehicle + taskTransported.weight;

            ArrayList<Action> actions2nextState = new ArrayList<Action>(actions2state);
            if (city == nextCity){
                actions2nextState.add(new Delivery(taskTransported));
            } else {
                for (City city : city.pathTo(nextCity)) {
                    actions2nextState.add(new Move(city));
                }
                actions2nextState.add(new Delivery(taskTransported));
            }
            nextStates.add(new StateObject(vehicle, nextWeightVehicle, nextFreeWeightVehicle, nextCity, actions2nextState, tasks2do, nextTransported, nextCost));
        }
        return nextStates;
    }


    //Getter
    public City getCity() {
        return city;
    }

    public ArrayList<Action> getActions2state() {
        return actions2state;
    }

    public boolean isGoal() {
        return isGoal;
    }

    public double getGCost() {
        return gCost;
    }

    public double getFCost() {
        return fCost;
    }

    public double getHCost() {
        return hCost;
    }


    // For contains method for ClosedList
    @Override
    public boolean equals(Object otherObject) {
        if (getClass() != otherObject.getClass())
            return false;
        StateObject otherState = (StateObject) otherObject;
        if (!city.equals(otherState.city))
            return false;
        if (!tasks2do.equals(otherState.tasks2do))
            return false;
        if (!tasksBeingDone.equals(otherState.tasksBeingDone))
            return false;
        if (weightVehicle != otherState.weightVehicle)
            return false;
        if (freeWeightVehicle != otherState.freeWeightVehicle)
            return false;
        if (isGoal != otherState.isGoal)
            return false;
        return true;
    }
}
