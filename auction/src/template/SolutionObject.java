package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import javax.annotation.processing.SupportedSourceVersion;
import java.util.ArrayList;
import java.util.List;

public class SolutionObject{
    private List<Vehicle> vehicles;
    private List<Task> tasks;
    private int worldNbTask;
    private ArrayList<ArrayList> worldPlan;

    //**********************************************************************
    //*************************** Constructors *****************************

    // By default
    public SolutionObject(List<Vehicle> vehicles, List<Task> tasks) {
        this.vehicles = vehicles;
        this.worldPlan = new ArrayList<>();
        this.tasks = tasks;

        int id_max = Integer.MIN_VALUE;
        for (Task t: tasks){
            if (t.id > id_max){
                id_max = t.id;
            }
        }
        this.worldNbTask = id_max + 1;
        for(Vehicle vehicle: vehicles) {
            this.worldPlan.add(vehicle.id(), new ArrayList<Integer>()); //Initialisation
        }
    }

    // For cloning SolutionObject
    public SolutionObject(SolutionObject another) {
        this.worldPlan = new ArrayList<>(); //ArrayList must be clone as well
        for (ArrayList vehiclePlan: another.worldPlan){
            ArrayList vehiclePlan_clone = new ArrayList<>(vehiclePlan); //ArrayList must be clone as well
            this.worldPlan.add(vehiclePlan_clone);
        }
        this.tasks = another.tasks;
        this.worldNbTask = another.worldNbTask;
        this.vehicles = another.vehicles;
    }


    //**********************************************************************
    //***************************** Methods ********************************

    // ** ON WORLD PLAN **
    //Compute the cost of the company
    public double getTotalCost(){
        double worldDistanceCost = 0.0;

        for(Vehicle vehicle: vehicles) {
            // Distance from home to the first action task
            City vehicleHomeCity = vehicle.homeCity();
            Task firstTaskVehicle = getVehicleFirstTask(vehicle.id());
            double vehicleFirstCost = 0.0;
            if (firstTaskVehicle != null) {
                vehicleFirstCost = vehicleHomeCity.distanceTo(firstTaskVehicle.pickupCity);
            }

            // Distance of the plan
            ArrayList<Integer> vehiclePlan = getVehiclePlan(vehicle.id());
            double distancePlan = 0.0;
            for (int i = 0; i < vehiclePlan.size() - 1; i++) {
                int taskID_from = vehiclePlan.get(i);
                int taskID_to = vehiclePlan.get(i + 1);
                Task task_from = getTask(taskID_from);
                Task task_to = getTask(taskID_to);
                if (isPickup(taskID_from)) {
                    if (isPickup(taskID_to)) {
                        distancePlan += task_from.pickupCity.distanceTo(task_to.pickupCity);
                    } else {
                        distancePlan += task_from.pickupCity.distanceTo(task_to.deliveryCity);
                    }
                } else {
                    if (isPickup(taskID_to)) {
                        distancePlan += task_from.deliveryCity.distanceTo(task_to.pickupCity);
                    } else {
                        distancePlan += task_from.deliveryCity.distanceTo(task_to.deliveryCity);
                    }
                }
            }

            double distance = (vehicleFirstCost + distancePlan); //Distance of the whole plan
            worldDistanceCost += distance * vehicle.costPerKm(); // Cost of the distance of all vehicles
        }
        return worldDistanceCost;
    }

    //Check if the world plan respects constraints
    public boolean isValid() {
        ArrayList<Integer> worldTaskIDPickup = new ArrayList<Integer>();
        ArrayList<Integer> worldTaskIDDelivery = new ArrayList<Integer>();

        for (Vehicle vehicle: vehicles) {
            int weightVehicle = 0;

            ArrayList<Integer> vehicleTasksIDPickup = new ArrayList<Integer>();
            ArrayList<Integer> vehicleTasksID = getVehiclePlan(vehicle.id());

            for (int taskID : vehicleTasksID) {
                if(isPickup(taskID)) { //Pickup action
                    if(worldTaskIDPickup.contains(taskID)){
                        return false; //Action task already doing by an other vehicle
                    }

                    double weightTask = getTask(taskID).weight;
                    weightVehicle += weightTask;
                    if(weightVehicle > vehicle.capacity()) {
                        return false; // Overload
                    }

                    // Action validated
                    worldTaskIDPickup.add(taskID);
                    vehicleTasksIDPickup.add(taskID);

                } else { //Delivery action
                    if(worldTaskIDDelivery.contains(taskID)) {
                        return false; // Action task already doing by an other vehicle
                    }
                    if (!vehicleTasksIDPickup.contains(taskID-worldNbTask)){
                        return false; // Vehicle has not picked up the package yet
                    }

                    // Action validated
                    worldTaskIDDelivery.add(taskID);
                    double weightTask = getTask(taskID).weight;
                    weightVehicle -= weightTask;
                }
            }
        }
        return true;
    }

    //Create a plan for each vehicle
    public ArrayList<Plan> generatePlan() {
        ArrayList<Plan> plans = new ArrayList<Plan>();
        for (Vehicle vehicle: vehicles) {
            City currentCity = vehicle.getCurrentCity();
            Plan plan = new Plan(currentCity);

            City nextCity;
            ArrayList<Integer> vehicleTasksID = getVehiclePlan(vehicle.id());
            for (int taskID : vehicleTasksID) {
                // Find to which city to move in function of the task
                if (isPickup(taskID)) {
                    nextCity = getTask(taskID).pickupCity;
                } else {
                    nextCity = getTask(taskID).deliveryCity;
                }

                // Add the journey to the plan from the current city to the next one
                for (City city : currentCity.pathTo(nextCity)) {
                    plan.appendMove(city);
                }

                // Add the action to the plan
                if (isPickup(taskID)) {
                    plan.appendPickup(getTask(taskID));
                } else {
                    plan.appendDelivery(getTask(taskID));
                }
                currentCity = nextCity;
            }
            plans.add(plan);
        }
        return plans;
    }

    //Display the solution (the world plan)
    public void display(){
        ArrayList vPlan;
        for (int vID = 0; vID < vehicles.size(); vID++){
            vPlan = getVehiclePlan(vID);
            System.out.println("Plan for vehicle " + vID + ": " + vPlan);
        }
        System.out.println("\n------------------------------------------------------\n");
    }


    // ** OPERATORS FOR MODIFYING THE WORLD PLAN **
    //Changing vehicle operator: take the first task from the tasks of one vehicle and give it to another vehicle.
    public void changingVehicle(int vehicleID1, int vehicleID2) {
        Task task2move = this.getVehicleFirstTask(vehicleID1);
        if(task2move != null) {
            this.removeTask(vehicleID1, task2move.id);
            this.addTask(vehicleID2, task2move.id, true);
        }
    }

    //Changing task order operator: change the order of two tasks in the task list of a vehicle.
    public void changingTaskOrder(int vehicleID, int idx1, int idx2) {
        int task2move2idx2 = (int) worldPlan.get(vehicleID).get(idx1);
        int task2move2idx1 = (int) worldPlan.get(vehicleID).get(idx2);
        getVehiclePlan(vehicleID).set(idx1, task2move2idx1);
        getVehiclePlan(vehicleID).set(idx2, task2move2idx2);
    }

    // Add task to a vehicle
    public void addTask(int vehicleID, int taskID, boolean atBeginning) {
        if (atBeginning){
            getVehiclePlan(vehicleID).add(0, taskID+worldNbTask); //delivery action
            getVehiclePlan(vehicleID).add(0, taskID); //pickup action
        } else{
            getVehiclePlan(vehicleID).add(taskID); //pickup action
            getVehiclePlan(vehicleID).add(taskID+worldNbTask); //delivery action
        }
    }

    // Remove task from a vehicle
    public void removeTask(int vehicleID, int taskID) {
        worldPlan.get(vehicleID).remove(Integer.valueOf(taskID)); //pickup action
        worldPlan.get(vehicleID).remove(Integer.valueOf(taskID+worldNbTask)); //delivery action
    }


    // ** ON VEHICLE **
    public Task getVehicleFirstTask(int vehicleID){
        if (!getVehiclePlan(vehicleID).isEmpty()){
            int taskID = (int) getVehiclePlan(vehicleID).get(0);
            return getTask(taskID);
        }
        return null;
    }

    public ArrayList getVehiclePlan(int vehicleID){
        return worldPlan.get(vehicleID);
    }


    // ** ON TASK **
    public Task getTask(int taskID){
        for (Task task: tasks){
            if ((taskID == task.id) || ((taskID-worldNbTask) == task.id)) {
                return task;
            }
        }
        return null;
    }

    public boolean isPickup(int taskID){
        if (taskID >= worldNbTask){
            return false;
        }else {
            return true;
        }
    }


    //**********************************************************************
    //***************************** Getter *********************************
    public ArrayList<ArrayList> getWorldPlan() {
        return worldPlan;
    }

    public int getVehicleNbTask(int vehicleID){
        return worldPlan.get(vehicleID).size();
    }
}
