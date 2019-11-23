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
public class CentralizedPlan {

    public static SolutionObject centralizedSolution(List<Vehicle> vehicles, List<Task> tasks, long timeLimit){
        //***** SLS algorithm parameters *****
        String INITIALISATION_METHOD = "fairlyDistributed"; // Choice between "toTheBiggest", "randomly" and "fairlyDistributed".
        final double PROBABILITY = 0.6; // Exploitation probability
        final int CRITERIA_1 = 800; // Maximum iteration to compute without improvement of a solution in a sub-part before to compute the next solution of this sub-part
        final int CRITERIA_2 = 3; // Number of solution to find in a sub-part.
        final int CRITERIA_3 = 3; // Number of part without improvement (stopCriteria)
        final int NB_FINALSOLUTIONS = 5;

        if(tasks.size() == 0){
            return new SolutionObject(vehicles, tasks);
        }


        //***** Run SLS algorithm several times and choose the best solution among the final solutions computed *****
        ArrayList<SolutionObject> finalSolutions = new ArrayList<>();
        for (int i = 0; i < NB_FINALSOLUTIONS; i++){
            if (i >= 3){
                INITIALISATION_METHOD = "randomly";
            }
            finalSolutions.add(SLS(vehicles, tasks, INITIALISATION_METHOD, PROBABILITY, CRITERIA_1, CRITERIA_2, CRITERIA_3, timeLimit/NB_FINALSOLUTIONS));
        }

        int count = 1;
        double bestCost = Double.MAX_VALUE;
        SolutionObject bestSolution = null;
        for (SolutionObject solution: finalSolutions){
            if (solution.getTotalCost() < bestCost){
                bestCost = solution.getTotalCost();
                bestSolution = new SolutionObject(solution);
            }
            count ++;
        }

        return bestSolution;
    }



    // SLS algorithm
    private static SolutionObject SLS(List<Vehicle> vehicles, List<Task> tasks,
                                      final String INITIALISATION_METHOD, final double PROBABILITY,
                                      final int CRITERIA_1, final int CRITERIA_2, final int CRITERIA_3,
                                      final long TIME) {
        //**********************************************************************
        //************************** SLS algorithm *****************************
        //**********************************************************************
        long time_start_SLS = System.currentTimeMillis();
        long time_end_SLS;
        long duration_SLS = 0;
        //***** 1st step : SelectInitialSolution *****
        SolutionObject initialSolution = selectInitialSolution(vehicles, tasks, INITIALISATION_METHOD);
        //System.out.println("Cost: "+initialSolution.getTotalCost());
        SolutionObject currentSolution = new SolutionObject(initialSolution);
        SolutionObject bestSolution = new SolutionObject(initialSolution);


        //***** 2nd step : Repeat until termination condition met *****
        int noImprovementSubSolutionCounter = 0;
        int noImprovementBestSolutionCounter = 0;
        int nbOfBestSubSolutionFound = 0;

        int iterationsCounter = 0;
        boolean stopCondition = false;
        while (!stopCondition && duration_SLS < (TIME)) {
            //** 1st sub-step : ChooseNeighbours **
            // This function provides a set of candidate assignment that are close to the current one and could possibly improve it.
            ArrayList<SolutionObject> neighbourSolutions = chooseNeighbours(currentSolution, vehicles);


            //** 2nd sub-step : LocalChoice **
            // The probability p is a parameter of the algorithm. If p is close to 1, the algorithm converges faster
            // but it is easily trapped into a local minima.
            currentSolution = localChoice(currentSolution, neighbourSolutions, PROBABILITY);


            //** 3rd sub-step : Check conditions to update bestSolution and to start again a sub-part or start the next one. **
            // Save this sub-solution if this is the best one.
            if (currentSolution.getTotalCost() < bestSolution.getTotalCost()) {
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
                double previousBestCost = currentSolution.getTotalCost();
                double currentBestCost = bestSolution.getTotalCost();
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
            time_end_SLS = System.currentTimeMillis();
            duration_SLS = time_end_SLS - time_start_SLS;
        }

        return currentSolution;
    }


    // How to initialise ? --> 3 ways: All tasks to the biggest vehicle OR tasks are distributed (randomly) among the vehicles
    private static SolutionObject selectInitialSolution(List<Vehicle> vehicles, List<Task> tasks, String initialisationMethod) {
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
                    if (possibleInitialSolution.getTotalCost() < bestCostInitial) {
                        bestCostInitial = possibleInitialSolution.getTotalCost();
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

        return initialSolution;
    }


    // Create all neighbours from the current solution by calling operators
    private static ArrayList<SolutionObject> chooseNeighbours(SolutionObject currentSolution, List<Vehicle> vehicles){
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
    private static SolutionObject localChoice(SolutionObject currentSolution, ArrayList<SolutionObject> neighbourSolutions, final double PROBABILITY){

        //** 1st step: It first selects the assignment A in the set of candidates that gives the best improvement of the
        //             objective function. If there are multiple equally good assignments, it chooses one randomly.
        ArrayList<SolutionObject> bestSolutions = new ArrayList<SolutionObject>();
        double currentCost = currentSolution.getTotalCost();
        double minCost = Double.MAX_VALUE; //bestCost in this case is the minimal cost

        // Sort candidates to find the best one(s).
        for(SolutionObject neighbourSolution : neighbourSolutions) {
            if(neighbourSolution.isValid()) { //Check that the solution is achievable.
                double neighbourCost = neighbourSolution.getTotalCost();
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
}
