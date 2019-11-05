package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class CentralizedTask {
	
	private boolean delivery;
	private int id;
	private City city;
	private int weight;
	
	public CentralizedTask(Task task, boolean delivery) {
		this.id = task.id;
		this.delivery = delivery;
		if (delivery) {
			this.weight = -task.weight;
			this.city = task.deliveryCity;
		}
		else {
			this.weight = task.weight;
			this.city = task.pickupCity;
		}
	}
	
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public CentralizedTask() {
		this.id = 0;
		this.delivery = false;
		this.city = null;		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isDelivery() {
		return delivery;
	}

	public void setDelivery(boolean delivery) {
		this.delivery = delivery;
	}

	public City getCity() {
		return city;
	}

	public void setCity(City city) {
		this.city = city;
	}

}