package template;

//the list of imports
import java.io.File;
import java.util.*;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;


    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }


    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        //***** SLS algorithm parameters *****
        final String INITIALISATION_METHOD = "fairlyDistributed"; // Choice between "toTheBiggest", "randomly" and "fairlyDistributed".
        final double PROBABILITY = 0.75; // Exploitation probability
        final int CRITERIA_1 = 800; // Maximum iteration to compute without improvement of a solution in a sub-part before to compute the next solution of this sub-part
        final int CRITERIA_2 = 3; // Number of solution to find in a sub-part.
        final int CRITERIA_3 = 3; // Number of part without improvement (stopCriteria)
        final int NB_FINALSOLUTIONS = 5;


        //***** Run SLS algorithm several times and choose the best solution among the final solutions computed *****
        ArrayList<SolutionObject> finalSolutions = new ArrayList<>();
        for (int i = 0; i < NB_FINALSOLUTIONS; i++){
            finalSolutions.add(SLS(vehicles, tasks, INITIALISATION_METHOD, PROBABILITY, CRITERIA_1, CRITERIA_2, CRITERIA_3));
        }
        System.out.println("\n--------------------------------------------------------------------\n");

        int count = 1;
        double bestCost = Double.MAX_VALUE;
        SolutionObject bestSolution = null;
        for (SolutionObject solution: finalSolutions){
            System.out.println("\nPOSSIBLE BEST SOLUTION "+ count);
            solution.display();
            if (solution.getTotalCost(false) < bestCost){
                bestCost = solution.getTotalCost(false);
                bestSolution = new SolutionObject(solution);
            }
            count ++;
        }


        System.out.println("\n--------------------------------------------------------------------\n");

        //***** Generate plan for agents(vehicles) *****
        System.out.println("\nFINAL SOLUTION");
        bestSolution.display();
        ArrayList<Plan> plans = new ArrayList<Plan>(bestSolution.generatePlan());

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;


        //**********************************************************************
        //************************** Naive algorithm ***************************
        //**********************************************************************
//        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
//
//        List<Plan> plans = new ArrayList<Plan>();
//        plans.add(planVehicle1);
//        while (plans.size() < vehicles.size()) {
//            plans.add(Plan.EMPTY);
//        }
//
//        long time_end = System.currentTimeMillis();
//        long duration = time_end - time_start;
//        System.out.println("The plan was generated in " + duration + " milliseconds.");
//
//        return plans;
    }



    // SLS algorithm
    private SolutionObject SLS(List<Vehicle> vehicles, TaskSet tasks,
                               final String INITIALISATION_METHOD, final double PROBABILITY,
                               final int CRITERIA_1, final int CRITERIA_2, final int CRITERIA_3) {
        //**********************************************************************
        //************************** SLS algorithm *****************************
        //**********************************************************************

        //***** 1st step : SelectInitialSolution *****
        SolutionObject initialSolution = selectInitialSolution(vehicles, tasks, INITIALISATION_METHOD);
        SolutionObject currentSolution = new SolutionObject(initialSolution);
        SolutionObject bestSolution = new SolutionObject(initialSolution);


        //***** 2nd step : Repeat until termination condition met *****
        int noImprovementSubSolutionCounter = 0;
        int noImprovementBestSolutionCounter = 0;
        int nbOfBestSubSolutionFound = 0;

        int iterationsCounter = 0;
        boolean stopCondition = false;
        while (!stopCondition) {
            //** 1st sub-step : ChooseNeighbours **
            // This function provides a set of candidate assignment that are close to the current one and could possibly improve it.
            ArrayList<SolutionObject> neighbourSolutions = chooseNeighbours(currentSolution, vehicles, tasks);


            //** 2nd sub-step : LocalChoice **
            // The probability p is a parameter of the algorithm. If p is close to 1, the algorithm converges faster
            // but it is easily trapped into a local minima.
            currentSolution = localChoice(currentSolution, neighbourSolutions, PROBABILITY);


            //** 3rd sub-step : Check conditions to update bestSolution and to start again a sub-part or start the next one. **
            // Save this sub-solution if this is the best one.
            if (currentSolution.getTotalCost(false) < bestSolution.getTotalCost(false)) {
                bestSolution = new SolutionObject(currentSolution);
                noImprovementSubSolutionCounter = 0;
            }

            // Start again the sub-part if there is no improvement of the cost of the sub-solution found.
            if (noImprovementSubSolutionCounter == CRITERIA_1) {
                currentSolution = new SolutionObject(initialSolution);
                noImprovementSubSolutionCounter = 0;
                nbOfBestSubSolutionFound += 1;
            }

            // Switch to the next part if we have computed this sub-part enough and check if there is an improvement of the bestSolution since last parts.
            if (nbOfBestSubSolutionFound == CRITERIA_2) {
                double previousBestCost = currentSolution.getTotalCost(false);
                double currentBestCost = bestSolution.getTotalCost(false);
                if (currentBestCost == previousBestCost) {
                    noImprovementBestSolutionCounter += 1;
                } else {
                    noImprovementBestSolutionCounter = 0;
                }

                initialSolution = new SolutionObject(bestSolution);
                currentSolution = new SolutionObject(initialSolution);
                nbOfBestSubSolutionFound = 0;
            }


            //** 4th sub-step : Repeat until termination condition met **
            if (noImprovementBestSolutionCounter == CRITERIA_3) {
                stopCondition = true;
            }
            noImprovementSubSolutionCounter++;
            iterationsCounter++;
        }
        System.out.println("--> Number of iterations to find a solution: " + iterationsCounter);
        return currentSolution;
    }


    // How to initialise ? --> 3 ways: All tasks to the biggest vehicle OR tasks are distributed (randomly) among the vehicles
    private SolutionObject selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks, String initialisationMethod) {
        SolutionObject initialSolution = new SolutionObject(vehicles, tasks);

        switch (initialisationMethod) {
            case "randomly":
                ArrayList<SolutionObject> possibleInitialSolutions = new ArrayList<SolutionObject>();
                for (int c = 0; c < 100; c++) {
                    Random random = new Random();
                    for (Task task : tasks) {
                        int vehicleID = random.nextInt(vehicles.size());
                        if (task.weight > vehicles.get(vehicleID).capacity()) {
                            return null;
                        } else {
                            initialSolution.addTask(vehicleID, task.id, false);
                        }
                    }
                    possibleInitialSolutions.add(new SolutionObject(initialSolution));
                }
                double bestCostInitial = Double.MAX_VALUE;
                SolutionObject bestInitialSolution = null;
                for (SolutionObject possibleInitialSolution : possibleInitialSolutions) {
                    if (possibleInitialSolution.getTotalCost(false) < bestCostInitial) {
                        bestCostInitial = possibleInitialSolution.getTotalCost(false);
                        bestInitialSolution = new SolutionObject(possibleInitialSolution);
                    }
                }
                initialSolution = new SolutionObject(bestInitialSolution);
                break;

            case "fairlyDistributed":
                for (Task task : tasks) {
                    int vehicleID = task.id % vehicles.size();
                    if (task.weight > vehicles.get(vehicleID).capacity()) {
                        return null;
                    } else {
                        initialSolution.addTask(vehicleID, task.id, false);
                    }
                }
                break;

            case "toTheBiggest":
                // Looking for the biggest vehicle
                int maxCapacity = 0;
                int bigVehicleID = 0;
                for(Vehicle vehicle: vehicles) {
                    if(vehicle.capacity() > maxCapacity) {
                        maxCapacity = vehicle.capacity();
                        bigVehicleID = vehicle.id();
                    }
                }
                // Create the initial solution
                for(Task task : tasks) {
                    if(task.weight > maxCapacity){
                        return null;
                    } else{
                        initialSolution.addTask(bigVehicleID, task.id, false);
                    }
                }
                break;
        }
        System.out.println("\nINITIALISATION PLAN");
        initialSolution.display();
        return initialSolution;
    }


    // Create all neighbours from the current solution by calling operators
    private ArrayList<SolutionObject> chooseNeighbours(SolutionObject currentSolution, List<Vehicle> vehicles, TaskSet tasks){
        ArrayList<SolutionObject> neighbours = new ArrayList<SolutionObject>();

        // Choose a random vehicle that has task(s)
        int vi_ID;
        Random random = new Random();
        do {
            vi_ID = random.nextInt(vehicles.size());
        } while(currentSolution.getVehicleFirstTask(vi_ID) == null);

        // Applying the changing vehicle operator :
        for(Vehicle vj: vehicles) {
            if (vi_ID != vj.id()) {
                Task nextTask = currentSolution.getVehicleFirstTask(vi_ID);
                if (nextTask != null) {
                    if (nextTask.weight <= vj.capacity()) {
                        SolutionObject nextPossibleSolution = new SolutionObject(currentSolution); //clone the current one
                        nextPossibleSolution.changingVehicle(vi_ID, vj.id()); //apply the operator
                        neighbours.add(nextPossibleSolution);
                    }
                }
            }
        }


        // Applying the changing task order operator :
        int length = currentSolution.getVehicleNbTask(vi_ID);  // compute the number of tasks of the vehicle chosen
        if (length >= 2) {
            for (int ind1 = 0; ind1 < length - 1; ind1++) {
                for (int ind2 = ind1 + 1; ind2 < length; ind2++) {
                    SolutionObject nextPossibleSolution = new SolutionObject(currentSolution); //clone the current one
                    nextPossibleSolution.changingTaskOrder(vi_ID, ind1, ind2); //apply the operator
                    neighbours.add(nextPossibleSolution);
                }
            }
        }
        return neighbours;
    }


    // Choose a solution in the neighbour ones using exploitation/exploration.
    private SolutionObject localChoice(SolutionObject currentSolution, ArrayList<SolutionObject> neighbourSolutions, final double PROBABILITY){

        //** 1st step: It first selects the assignment A in the set of candidates that gives the best improvement of the
        //             objective function. If there are multiple equally good assignments, it chooses one randomly.
        ArrayList<SolutionObject> bestSolutions = new ArrayList<SolutionObject>();
        double currentCost = currentSolution.getTotalCost(false);
        double minCost = Double.MAX_VALUE; //bestCost in this case is the minimal cost

        // Sort candidates to find the best one(s).
        for(SolutionObject neighbourSolution : neighbourSolutions) {
            if(neighbourSolution.isValid()) { //Check that the solution is achievable.
                double neighbourCost = neighbourSolution.getTotalCost(false);
                if(neighbourCost != currentCost){
                    if(neighbourCost < minCost) {
                        minCost = neighbourCost;
                        bestSolutions.clear();
                        bestSolutions.add(neighbourSolution);
                    } else if(neighbourCost == minCost) {
                        bestSolutions.add(neighbourSolution);
                    }
                }
            }
        }
        // Choose one randomly.
        SolutionObject nextSolution;
        if (bestSolutions.size() >= 1){
            Random random = new Random();
            int randomIndex = random.nextInt(bestSolutions.size());
            nextSolution = new SolutionObject(bestSolutions.get(randomIndex));
        } else{
            nextSolution = new SolutionObject(currentSolution);
        }


        //** 2nd step: Then with probability p it returns A (next assignment) , with probability 1 − p it returns
        //             the current assignment Aold. The probability p is a parameter of the algorithm.
        Random random = new Random();
        if(random.nextDouble() < PROBABILITY) {
            currentSolution = new SolutionObject(nextSolution);
        } else{
            boolean isPossibleNextSolution;
            SolutionObject possibleNextSolution;
            do {
                int randomIndex = random.nextInt(neighbourSolutions.size());
                possibleNextSolution = neighbourSolutions.get(randomIndex);
                isPossibleNextSolution = possibleNextSolution.isValid();
            } while (!isPossibleNextSolution);
            currentSolution = new SolutionObject(possibleNextSolution);
        }
        return currentSolution;
    }


    // Generate naive plan
    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
