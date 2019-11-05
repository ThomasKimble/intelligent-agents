package template;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import java.io.File;

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
		} catch (Exception exc) {
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

		// Algorithm Parameters
		final double PROBABILITY = 0.3;
		final String INIT_METHOD = "biggestVehicle";

//		ArrayList<CentralizedTask> pdTasks = generateTasks(vehicles, tasks);
//		Solution A = selectInitialSolution(vehicles, pdTasks, INIT_METHOD);
//		for (Vehicle v : vehicles) {
//			orderCheck(A, v);
//		}
//		ArrayList<Plan> plans = null;

		// Runs the algorithm, gets final cost and defines plan
		Solution A = SLS(vehicles, tasks, PROBABILITY, INIT_METHOD);
		double cost = getCost(A, vehicles);
		printSolution(A, vehicles, cost);
		ArrayList<Plan> plans = SLSPlan(A, vehicles, tasks);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return plans;
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
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}

		return plan;
	}

	public ArrayList<Plan> SLSPlan(Solution A, List<Vehicle> vehicles, TaskSet tasks) {
		ArrayList<Plan> plans = new ArrayList<Plan>();

		for (Vehicle v : vehicles) {
			City homeCity = v.homeCity();
			CentralizedTask nextTask = A.getNextTask().get(v);
			City currentCity = homeCity;
			Plan plan = new Plan(v.homeCity());

			if (nextTask != null) {
				City nextCity = A.getNextTask().get(v).getCity();
				CentralizedTask t = A.getNextTask().get(v);
				int numberTasks = 0;
				while (t != null) {
					t = A.getNextTask().get(t);
					numberTasks++;
				}

				// for all tasks add sub paths to itinerary
				System.out.println("VEHICLE " + v.id() + " PATH");
				System.out.println("Start: " + homeCity.name);
				for (int i = 0; i < numberTasks; i++) {
					for (City city : currentCity.pathTo(nextCity)) {
						System.out.println("Move to: " + city.name);
						plan.appendMove(city);
					}
					if (!nextTask.isDelivery()) {
						System.out.println("Pickup Task " + nextTask.getId() + " from " + nextCity.name);
						plan.appendPickup(getTask(nextTask.getId(), tasks));
					} else {
						System.out.println("Deliver Task " + nextTask.getId() + " in " + nextCity.name);
						plan.appendDelivery(getTask(nextTask.getId(), tasks));
					}
					nextTask = A.getNextTask().get(nextTask);
					currentCity = nextCity;
					if (nextTask == null) {
						nextCity = null;
					} else
						nextCity = nextTask.getCity();
				}
				System.out.println(" ");
			}
			plans.add(plan);
		}
		return plans;

	}

	public Task getTask(int taskID, TaskSet tasks) {
		for (Task task : tasks) {
			if ((taskID == task.id)) {
				return task;
			}
		}
		return null;
	}

	public Solution SLS(List<Vehicle> vehicles, TaskSet tasks, double P, String initMethod) {
		Solution A = selectInitialSolution(vehicles, tasks, initMethod);
		boolean stop = false;
		int noImprovementCounter = 0;
		int totalIterations = 0;
		double bestCost = Double.MAX_VALUE;

		while (!stop) {
			Solution Aold = new Solution(A);
			ArrayList<Solution> N = chooseNeighboors(Aold, vehicles);
			A = localChoice(vehicles, N, Aold, P);
			double currentCost = getCost(A, vehicles);
			
			if (totalIterations == 5000)
				System.out.println("Initial " + totalIterations + " iterations done");
			if (totalIterations == 10000)
				System.out.println("Initial " + totalIterations + " iterations done");
			if (totalIterations == 15000)
				System.out.println("Initial " + totalIterations + " iterations done");
			if (totalIterations == 20000)
				System.out.println("Initial " + totalIterations + " iterations done");
			if (totalIterations > 20000) {
				if (currentCost < bestCost) {
					bestCost = currentCost;
					System.out.println("Cost improved after " + noImprovementCounter + " iterations");
					noImprovementCounter = 0;
				} else
					noImprovementCounter++;
				if (noImprovementCounter == 100)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 200)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 300)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 400)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 500)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 600)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 700)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 800)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 900)
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
				if (noImprovementCounter == 1000) {
					System.out.println("Cost not improved after " + noImprovementCounter + " iterations");
					System.out.println(" ");
					stop = true;
				}
			}
			totalIterations++;
		}

		return A;

	}

	public Solution localChoice(List<Vehicle> vehicles, ArrayList<Solution> N, Solution Aold, double P) {
		ArrayList<Solution> bestSolutions = new ArrayList<Solution>();
		Solution A = null;
		double minCost = Double.MAX_VALUE;

		for (Solution a : N) {
			double currentCost = getCost(a, vehicles);
			if (currentCost < minCost) {
				bestSolutions.clear();
				bestSolutions.add(new Solution(a));
				minCost = currentCost;
			} else if (currentCost == minCost) {
				bestSolutions.add(new Solution(a));
			}
		}

		// if we have a better solution than Aold choose a random one with the best cost
		if (bestSolutions.size() > 0) {
			Random random = new Random();
			int randomIndex = random.nextInt(bestSolutions.size());
			A = new Solution(bestSolutions.get(randomIndex));
		} else {
			A = new Solution(Aold);
		}

		// with probability p return previous a, with probability 1-p return random
		// neighbor
		Random random = new Random();
		if (random.nextDouble() > P) {
			A = new Solution(Aold);
		}

		return A;

	}

	public double getCost(Solution A, List<Vehicle> vehicles) {
		double cost = 0;

		for (Vehicle v : vehicles) {
			double distance = 0;
			City homeCity = v.homeCity();
			City currentCity = homeCity;
			CentralizedTask nextTask = A.getNextTask().get(v);
			List<City> totalPath = new ArrayList<City>();

			if (nextTask != null) {
				City nextCity = A.getNextTask().get(v).getCity();
				CentralizedTask t = A.getNextTask().get(v);
				int numberTasks = 0;
				while (t != null) {
					t = A.getNextTask().get(t);
					numberTasks++;
				}

				// for all tasks add sub paths to itinerary
				totalPath.add(homeCity);
				for (int i = 0; i < numberTasks; i++) {
					List<City> subPath = currentCity.pathTo(nextCity);
					for (int j = 0; j < subPath.size(); j++) {
						totalPath.add(subPath.get(j));
					}
					nextTask = A.getNextTask().get(nextTask);
					currentCity = nextCity;
					if (nextTask == null) {
						nextCity = null;
					} else
						nextCity = nextTask.getCity();
				}

				// final itinerary
				for (int j = 0; j < totalPath.size(); j++) {
					if (j != 0)
						distance += totalPath.get(j - 1).distanceTo(totalPath.get(j));
				}
				cost += (double) distance * v.costPerKm();
			}
		}
		return cost;

	}

	public ArrayList<Solution> chooseNeighboors(Solution Aold, List<Vehicle> vehicles) {
		ArrayList<Solution> N = new ArrayList<Solution>();
		HashMap<Object, CentralizedTask> nextTask = Aold.getNextTask();
		HashMap<CentralizedTask, Integer> time = Aold.getTime();
		Vehicle vi = null;
		boolean same = false;
		int i = 0;
		int j = 0;

		// choose random vehicle with tasks
		i = (int) (Math.random() * (vehicles.size()) + 1) - 1;
		vi = vehicles.get(i);
		if (nextTask.get(vi) == null) {
			same = true;
			while (same) {
				if (nextTask.get(vi) != null)
					same = false;
				else {
					i = (int) (Math.random() * (vehicles.size()) + 1) - 1;
					vi = vehicles.get(i);
				}
			}
		}

		// applying the changingVehicle operator
		CentralizedTask t = nextTask.get(vi);
		for (Vehicle vj : vehicles) {
			if (vi != vj) {
				if (t.getWeight() < vj.capacity()) {
					Solution A = changingVehicle(Aold, vi, vj);
					if (A != null)
						N.add(new Solution(A));
				}
			}
		}

		// applying the changingTaskOrder operator
		int length = -1; // starts at -1 to get right amount of tasks
		Object task = vi;
		while (task != null) {
			task = nextTask.get(task);
			length++;
		}
		if (length > 1) {
			for (int tIDx1 = 0; tIDx1 < length - 1; tIDx1++) {
				for (int tIDx2 = tIDx1 + 1; tIDx2 < length; tIDx2++) {
					Solution A = changingTaskOrder(Aold, vi, tIDx1, tIDx2);
					if (A != null)
						N.add(new Solution(A));
				}
			}
		}

		return N;
	}

	public void printSolution(Solution A, List<Vehicle> vehicles, double cost) {
		HashMap<Object, CentralizedTask> nextTask = A.getNextTask();
		HashMap<CentralizedTask, Integer> time = A.getTime();

		if (A == null)
			System.out.println("Solution Impossible or Already Found");
		else {
			System.out.println("SOLUTION: cost = " + cost);
			for (Vehicle v : vehicles) {
				CentralizedTask t = nextTask.get(v);
				if (t != null) {
					if (!t.isDelivery())
						System.out.print("Vehicle " + v.id() + " next tasks: Task " + t.getId() + "-P (time "
								+ time.get(t) + ")");
					else
						System.out.print("Vehicle " + v.id() + " next tasks: Task " + t.getId() + "-D (time "
								+ time.get(t) + ")");
					t = nextTask.get(t);
					while (t != null) {
						if (!t.isDelivery())
							System.out.print(", Task " + t.getId() + "-P (time " + time.get(t) + ")");
						else
							System.out.print(", Task " + t.getId() + "-D (time " + time.get(t) + ")");
						t = nextTask.get(t);
					}
					System.out.print(", " + t);
					System.out.println(" ");
				} else
					System.out.println("Vehicle " + v.id() + " next task: " + t);
			}
			System.out.println(" ");
		}

	}

	public Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet oldTasks, String initMethod) {	
		HashMap<Object, CentralizedTask> nextTask = new HashMap<Object, CentralizedTask>();
		HashMap<CentralizedTask, Vehicle> vehicle = new HashMap<CentralizedTask, Vehicle>();
		HashMap<CentralizedTask, Integer> time = new HashMap<CentralizedTask, Integer>();
		Solution initialSolution = new Solution(nextTask, vehicle, time);
		Vehicle biggestVehicle = findBiggestVehicle(vehicles);
		ArrayList<CentralizedTask> tasks = new ArrayList<CentralizedTask>();

		// Generate centralized tasks
		for (Task task : oldTasks) {
			CentralizedTask t0 = new CentralizedTask(task, false);
			tasks.add(t0);
			CentralizedTask t1 = new CentralizedTask(task, true);
			tasks.add(t1);
		}
		
		// add null to tasks of all vehicles except biggest
		for (Vehicle v : vehicles) {
			if (!v.equals(biggestVehicle))
				nextTask.put(v, null);
		}

		// add tasks to biggest vehicle
		int t = 1;
		Object previousTask = biggestVehicle;
		for (CentralizedTask task : tasks) {
			nextTask.put(previousTask, task);
			vehicle.put(task, biggestVehicle);
			time.put(task, t);
			previousTask = task;
			t++;
		}
		nextTask.put(previousTask, null);
		initialSolution.setNextTask(nextTask);
		initialSolution.setVehicle(vehicle);
		initialSolution.setTime(time);

		switch (initMethod) {

		case "biggestVehicle":
			break;

		case "fairlyDistributed":
			Solution A = new Solution(initialSolution);
			int stopNumber = 2 * (1 + (tasks.size() / 2) / vehicles.size());
			int i = 1;

			for (CentralizedTask task : tasks) {
				Vehicle v0 = vehicles.get(0);
				Vehicle vi = vehicles.get(i);

				CentralizedTask tV0 = A.getNextTask().get(v0);
				int numberTasksV0 = 0;
				while (tV0 != null) {
					tV0 = A.getNextTask().get(tV0);
					numberTasksV0++;
				}
				if (numberTasksV0 == stopNumber) {
					A = changingVehicle(A, v0, vi);
					break;
				} else {
					A = changingVehicle(A, v0, vi);
					i++;
					if (i == vehicles.size())
						i = 1;
				}
			}
			initialSolution = new Solution(A);
		}

		System.out.print("ORIGINAL ");
		printSolution(initialSolution, vehicles, getCost(initialSolution, vehicles));

		return initialSolution;

	}

	public Vehicle findBiggestVehicle(List<Vehicle> vehicles) {
		int tempCapacity = 0;
		Vehicle biggestVehicle = vehicles.get(0);
		for (Vehicle v : vehicles) {
			if (v.capacity() > tempCapacity) {
				tempCapacity = v.capacity();
				biggestVehicle = v;
			}
		}
		return biggestVehicle;
	}

	public Solution changingVehicle(Solution A, Vehicle v1, Vehicle v2) {
		Solution A1 = new Solution(A);
		CentralizedTask tP = A1.getNextTask().get(v1);
		CentralizedTask t = A1.getNextTask().get(v1);
		CentralizedTask tD = null;

		if (tP != null) {
			for (int i = 0; i < A1.getNextTask().size(); i++) {
				if (A1.getNextTask().get(t).getId() == tP.getId() && A1.getNextTask().get(t).isDelivery()) {
					tD = A.getNextTask().get(t);
					break;
				}
				t = A1.getNextTask().get(t);
			}

			A1.getNextTask().put(t, A1.getNextTask().get(tD));
			A1.getNextTask().put(tD, A1.getNextTask().get(v2));
			A1.getNextTask().put(v2, tD);
			A1.getNextTask().put(v1, A1.getNextTask().get(tP));
			A1.getNextTask().put(tP, A1.getNextTask().get(v2));
			A1.getNextTask().put(v2, tP);
			updateTime(A1, v1);
			updateTime(A1, v2);
			A1.getVehicle().put(tD, v2);
			A1.getVehicle().put(tP, v2);

			return A1;
		} else
			return null;

	}

	public Solution changingTaskOrder(Solution A, Vehicle vi, int tIDx1, int tIDx2) {
		Solution Aold = new Solution(A);
		Solution A1 = new Solution(A);
		HashMap<Object, CentralizedTask> A1nextTask = A1.getNextTask();
		HashMap<CentralizedTask, Vehicle> A1vehicle = A1.getVehicle();
		HashMap<CentralizedTask, Integer> A1time = A1.getTime();
		boolean possibleOrder = false;

		// Check number of tasks in vehicle vi
		CentralizedTask t = A1.getNextTask().get(vi);
		int numberTasks = 0;
		while (t != null) {
			t = A1.getNextTask().get(t);
			numberTasks++;
		}

		// changing order only possible with 2 or more tasks
		if (numberTasks > 2) {
			Object tPre1 = vi;
			CentralizedTask t1 = A1nextTask.get(tPre1);
			int count = 1;

			while (count < tIDx1) {
				tPre1 = t1;
				t1 = A1nextTask.get(tPre1);
				count++;
			}

			CentralizedTask tPost1 = A1nextTask.get(t1);
			Object tPre2 = t1;
			CentralizedTask t2 = A1nextTask.get(tPre2);
			count++;

			while (count < tIDx2) {
				tPre2 = t2;
				t2 = A1nextTask.get(t2);
				count++;
			}

			CentralizedTask tPost2 = A1nextTask.get(t2);

			if (tPost1 == t2) {
				A1nextTask.put(tPre1, t2);
				A1nextTask.put(t2, t1);
				A1nextTask.put(t1, tPost2);
			} else {
				A1nextTask.put(tPre1, t2);
				A1nextTask.put(tPre2, t1);
				A1nextTask.put(t2, tPost1);
				A1nextTask.put(t1, tPost2);
			}

			updateTime(A1, vi);

			A1.setNextTask(A1nextTask);
			A1.setVehicle(A1vehicle);
			A1.setTime(A1time);

			possibleOrder = taskPositionCheck(A1, t2); // check that delivery after pickup
			if (possibleOrder) {
				possibleOrder = taskPositionCheck(A1, t1); // check that delivery after pickup
				if (possibleOrder)
					possibleOrder = orderCheck(A, vi); // check that capacity is never exceeded
			}

			if (possibleOrder) {
				return A1;
			} else {
				return null;
			}

		} else {
			return null;
		}

	}

	public boolean taskPositionCheck(Solution A, CentralizedTask t) {
		boolean possibleOrder = false;
		Solution A1 = new Solution(A);
		Vehicle v = A1.getVehicle().get(t);
		CentralizedTask tNew = A1.getNextTask().get(v);
		CentralizedTask tOther = null;
		int timeD = 0;
		int timeP = 0;

		CentralizedTask task = A1.getNextTask().get(v);
		int numberTasks = 0;
		while (task != null) {
			task = A1.getNextTask().get(task);
			numberTasks++;
		}

		// check if first task is correspondent task to t
		// if not look for correspondent task
		if (tNew.getId() == t.getId() && tNew.isDelivery() != t.isDelivery())
			tOther = tNew;
		else {
			for (int i = 0; i < numberTasks; i++) {
				if (A1.getNextTask().get(tNew).getId() == t.getId()
						&& A1.getNextTask().get(tNew).isDelivery() != t.isDelivery()) {
					tOther = A1.getNextTask().get(tNew);
					break;
				}
				tNew = A1.getNextTask().get(tNew);
			}
		}

		// attribute pickup and delivery times for task t
		if (t.isDelivery()) {
			timeD = A1.getTime().get(t);
			timeP = A1.getTime().get(tOther);
		} else {
			timeP = A1.getTime().get(t);
			timeD = A1.getTime().get(tOther);
		}

		// if pickup is before delivery the order is possible
		if (timeP < timeD)
			possibleOrder = true;

		return possibleOrder;

	}

	public boolean orderCheck(Solution A, Vehicle vi) {
		boolean possibleOrder = true;
		int load = 0;
		CentralizedTask t = A.getNextTask().get(vi);
		while (t != null) {
			load += t.getWeight();
			if (load > vi.capacity()) {
				possibleOrder = false;
				break;
			}
			t = A.getNextTask().get(t);
		}
		return possibleOrder;
	}

	public void updateTime(Solution A, Vehicle vi) {
		HashMap<Object, CentralizedTask> nextTask = A.getNextTask();
		HashMap<CentralizedTask, Integer> time = A.getTime();
		CentralizedTask ti = nextTask.get(vi);
		CentralizedTask tj = nextTask.get(ti);

		if (ti != null) {
			time.put(ti, 1);
			while (tj != null) {
				tj = nextTask.get(ti);
				if (tj != null) {
					time.put(tj, time.get(ti) + 1);
					ti = tj;
				}
			}
		}
	}

}