package template;

import java.util.HashMap;

import logist.simulation.Vehicle;

class Solution {
	private HashMap<Object, CentralizedTask> nextTask;
	private HashMap<CentralizedTask, Vehicle> vehicle;
	private HashMap<CentralizedTask, Integer> time;

	public Solution(HashMap<Object, CentralizedTask> nextTask, HashMap<CentralizedTask, Vehicle> vehicle, HashMap<CentralizedTask, Integer> time) {
		this.nextTask = nextTask;
		this.vehicle = vehicle;
		this.time = time;
	}
	
	public Solution() {
		this.nextTask = null;
		this.vehicle = null;
		this.time =  null;
	}
	
	// for cloning
	@SuppressWarnings("unchecked")
	public Solution(Solution another) {
        this.nextTask = (HashMap<Object, CentralizedTask>) another.nextTask.clone();
        this.vehicle = (HashMap<CentralizedTask, Vehicle>) another.vehicle.clone();
        this.time = (HashMap<CentralizedTask, Integer>) another.time.clone();
    }

	public HashMap<Object, CentralizedTask> getNextTask() {
		return nextTask;
	}

	public void setNextTask(HashMap<Object, CentralizedTask> nextTask) {
		this.nextTask = nextTask;
	}

	public HashMap<CentralizedTask, Vehicle> getVehicle() {
		return vehicle;
	}

	public void setVehicle(HashMap<CentralizedTask, Vehicle> vehicle) {
		this.vehicle = vehicle;
	}

	public HashMap<CentralizedTask, Integer> getTime() {
		return time;
	}

	public void setTime(HashMap<CentralizedTask, Integer> time) {
		this.time = time;
	}

}