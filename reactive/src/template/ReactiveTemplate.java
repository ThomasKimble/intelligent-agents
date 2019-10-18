package template;

import java.util.List;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {
	private int numActions;
	private Agent myAgent;
	private double decreaseRate;
	private List<City> cityList;
	private TaskDistribution TD;

	private StateObj [] [] stateMatrix;
	private ActionObj [] actionMatrix;
	private double [] [] vectorS;
	private ActionObj [] [] tempoPolicyMatrix;
	private double [] [] bestPolicyValueMatrix;
	private ActionObj [] [] bestPolicyMatrix;

	// Creation of the state matrix
	public void stateMatrixInit (List<City> cityList) {
		stateMatrix = new StateObj [cityList.size()] [cityList.size()]; //Initialisation
		for (City currentCity : cityList){ //Filling
			StateObj noPackageInCity = new StateObj(currentCity);
			stateMatrix [currentCity.id] [currentCity.id] = noPackageInCity;
			for (City destineCity : cityList){
				if (destineCity.id != currentCity.id){
					StateObj state = new StateObj(currentCity, destineCity);
					stateMatrix [currentCity.id] [destineCity.id] = state;
				}
			}
		}
		System.out.println("stateMatrixInit Done");
	}

	// Creation of the action matrix
	public void actionMatrixInit (List<City> cityList) {
		actionMatrix = new ActionObj [cityList.size()+1]; //Initialisation
		ActionObj actionA = new ActionObj(); //Filling
		actionMatrix[actionMatrix.length-1] = actionA;
		for(City city : cityList) {
			ActionObj actionB = new ActionObj(city);
			actionMatrix[city.id] = actionB;
		}
		System.out.println("actionMatrixInit Done");
	}

	// Return the probability for a current state, an action and a next state given
	public double probability(StateObj state, ActionObj action, StateObj nextState) {
		if (action.isTakeDelivery() && !state.isHasPackage()) {
			return 0;
		} else if(action.isTakeDelivery() && state.getDestineCity().id == nextState.getCurrentCity().id) {
			return TD.probability(nextState.getCurrentCity(), nextState.getDestineCity());
		} else if (!action.isTakeDelivery() && action.getNextCity().id == nextState.getCurrentCity().id) {
			return TD.probability(nextState.getCurrentCity(), nextState.getDestineCity());
		} else return 0;
	}

	// Return the reward for a state and an action given.
	public double reward(StateObj state, ActionObj action){
		if (!state.isHasPackage() && !action.isTakeDelivery()){ //No package available
			if(action.getNextCity().hasNeighbor(state.getCurrentCity())) { //Only choose a neighboring city
				return -70*state.getCurrentCity().distanceTo(action.getNextCity());
			} else return -99999999; //-inf

		} else if (state.isHasPackage() && !action.isTakeDelivery()){ //Agent does't want to take the package
			if(action.getNextCity().hasNeighbor(state.getCurrentCity())) { //Only choose a neighboring city
				return -70*state.getCurrentCity().distanceTo(action.getNextCity());
			} else return -99999999; //-inf

		} else if(state.isHasPackage() && action.isTakeDelivery()) { //Agent takes the package
			return TD.reward(state.getCurrentCity(), state.getDestineCity()) - 70*state.getCurrentCity().distanceTo(state.getDestineCity());
		} else return -99999999; //-inf
	}

	// Initialisation of the matrix used to compute the policy
	public void PolicyMatrixsInit(){
		vectorS = new double [cityList.size()] [cityList.size()];
		tempoPolicyMatrix = new ActionObj [cityList.size()] [cityList.size()];
		bestPolicyValueMatrix =  new double [cityList.size()] [cityList.size()];
		bestPolicyMatrix = new ActionObj [cityList.size()] [cityList.size()];
		for (int i = 0; i < stateMatrix.length; ++i){
			for(int j = 0; j < stateMatrix[i].length; ++j){
				vectorS[i][j] = 0.0;
				bestPolicyValueMatrix[i][j] = 0.0;
				tempoPolicyMatrix[i][j] = null;
				bestPolicyMatrix[i][j] = null;
			}
		}
		System.out.println("PolicyMatrixsInit Done");
	}

	// Computing the best policy
	public void policyInit() {
		double quantityMax;
		double quantity;
		double tempoSum;
		int count = 0;

		stateMatrixInit(cityList);
		actionMatrixInit(cityList);
		PolicyMatrixsInit();
		while(true) {
			for (int i = 0; i < stateMatrix.length; i++){
				for(int j = 0; j < stateMatrix[i].length; j++){
					quantityMax = 0;
					for (int h = 0; h < actionMatrix.length; h++){
						tempoSum = 0;
						for (int ni = 0; ni < stateMatrix.length; ni++){
							for(int nj = 0; nj < stateMatrix[ni].length; nj++){
								tempoSum = tempoSum + probability(stateMatrix[i][j], actionMatrix[h], stateMatrix[ni][nj]) * vectorS[ni][nj];
							}
						}
						quantity = reward(stateMatrix[i][j], actionMatrix[h]) + tempoSum*decreaseRate;
						if (quantity > quantityMax){
							quantityMax = quantity;
							tempoPolicyMatrix[i][j] = actionMatrix[h];
						}
					}
					vectorS[i][j] = quantityMax;
				}
			}
			if ((bestPolicyValueMatrix == vectorS) && count>100) {
				break;
			}
			count++;
			bestPolicyMatrix = tempoPolicyMatrix;
			bestPolicyValueMatrix = vectorS;
		}
	}


	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.decreaseRate = discount;
		this.numActions = 0;
		this.myAgent = agent;
		this.cityList = topology.cities();
		this.TD = td;

		this.policyInit();
		System.out.println("Setup Done");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();

		if (availableTask == null) { //No package
			System.out.println("NO PACKAGE");
			action = new Move(bestPolicyMatrix[currentCity.id][currentCity.id].getNextCity());
		} else {
			if(bestPolicyMatrix[currentCity.id][availableTask.deliveryCity.id].isTakeDelivery()) { //Take
				System.out.println("TAKEN");
				action = new Pickup(availableTask);
			} else { // Not taken
				System.out.println("NOT TAKEN");
				action = new Move(bestPolicyMatrix[currentCity.id][currentCity.id].getNextCity());
			}
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions for vehicule "+myAgent.id()+" is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		return action;
	}
}
