package template;

/* import table */
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, NAIVE }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case NAIVE:
			plan = naivePlan(vehicle, tasks);
			break;
		case ASTAR:
			plan = astarPlan(vehicle, tasks);
			break;
		case BFS:
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}

	
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
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}


	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks2do) {
		// Initial state
		City currentCity = vehicle.getCurrentCity();
		int weightVehicle = 0;
		for (Task taskTransported : vehicle.getCurrentTasks()) { //Adding the task in progress
			weightVehicle += taskTransported.weight;
		}
		int freeWeightVehicle = vehicle.capacity() - weightVehicle;
		StateObject currentState = new StateObject(vehicle, weightVehicle, freeWeightVehicle, currentCity, new ArrayList<Action>(), tasks2do, vehicle.getCurrentTasks(), 0);

		// BFS algorithm optimized to find the optimal plan by minimizing iteration
		ArrayList<StateObject> Q = new ArrayList<StateObject>();
		ArrayList<StateObject> nextLayerQ = new ArrayList<StateObject>();
		ArrayList<StateObject> C = new ArrayList<StateObject>();
		ArrayList<StateObject> goalState = new ArrayList<StateObject>();
		Q.add(currentState);

		int count = 0;
		double timeStart = System.currentTimeMillis();
		while(!(Q.isEmpty() && nextLayerQ.isEmpty())){
			if (Q.isEmpty()){
				nextLayerQ.sort(new SortByGCost());
				Q = (ArrayList<StateObject>) nextLayerQ.clone();
				nextLayerQ = new ArrayList<StateObject>();
			}
			StateObject state = Q.remove(0);
			if(state.isGoal()){
				goalState.add(state);
			} else{
				if (!C.contains(state) || (state.getGCost() < C.get(C.indexOf(state)).getGCost())){
					C.add(state);
					nextLayerQ.addAll(state.getNextStates());
				}
			}
			count = count + 1;
		}

		// Plan = actions of the state with minimal cost among final states available
		Plan plan = new Plan(currentState.getCity());
		double CostMin=goalState.get(0).getGCost();
		StateObject bestGoalState = goalState.get(0);
		for (StateObject state : goalState){
			double tempoCost = state.getGCost();
			if (tempoCost < CostMin){
				CostMin = tempoCost;
				bestGoalState = state;
			}
		}
		double executionTime = (System.currentTimeMillis() - timeStart)/1000;
		System.out.println("BFS algorithm");
		System.out.println("Number of iteration : "+count);
		System.out.println("Execution time : " + executionTime + " seconds");
		System.out.println("Plan cost : "+(bestGoalState.getGCost()));
		for (Action action : bestGoalState.getActions2state()){
			plan.append(action);
		}
		return plan;
	}


	private Plan astarPlan(Vehicle vehicle, TaskSet tasks2do){
		// Initial state
		City currentCity = vehicle.getCurrentCity();

		int weightVehicle = 0;
		for (Task taskTransported : vehicle.getCurrentTasks()) {
			weightVehicle += taskTransported.weight;
		}
		int freeWeightVehicle = vehicle.capacity() - weightVehicle;

		StateObject currentState = new StateObject(vehicle, weightVehicle, freeWeightVehicle, currentCity, new ArrayList<Action>(), tasks2do, vehicle.getCurrentTasks(), 0);

		// A* algorithm
		ArrayList<StateObject> Q = new ArrayList<>();
		ArrayList<StateObject> C = new ArrayList<StateObject>();
		Q.add(currentState);

		int count = 0;
		double timeStart = System.currentTimeMillis();
		while(!Q.isEmpty()){
			StateObject state = Q.remove(0);
			if (state.isGoal()) {
				double executionTime = (System.currentTimeMillis() - timeStart)/1000;
				System.out.println("A* algorithm");
				System.out.println("Number of iteration : "+count);
				System.out.println("Execution time : " + executionTime + " seconds");
				System.out.println("Plan cost : "+(state.getGCost()));
				Plan plan = new Plan(currentState.getCity());
				for (Action action : state.getActions2state()){
					plan.append(action);
				}
				return plan;
			} else if(!C.contains(state) || (state.getFCost() < C.get(C.indexOf(state)).getFCost())){
				C.add(state);
				Q.addAll(state.getNextStates());
				Q.sort(new SortByFCost());
			}
			count = count + 1;
		}
		return null;
	}


	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
