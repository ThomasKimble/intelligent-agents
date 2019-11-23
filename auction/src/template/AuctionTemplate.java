package template;

//the list of imports
import java.io.File;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
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
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private List<Vehicle> vehicle;
	private City currentCity;

	private long timeout_setup;
	private long timeout_plan;
	private long timeout_bid;

	private ArrayList<Task> currentTasks;
	private SolutionObject currentSolution;
	private SolutionObject futureSolution;
	private predictCost estimationFunction;
    private double income;
    private int count;


	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles();
		this.currentCity = vehicle.get(0).homeCity();

		this.currentTasks = new ArrayList<Task>();
		this.currentSolution = new SolutionObject(vehicle, currentTasks);
		this.income = 0;

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		this.estimationFunction = new predictCost(topology);
		this.count = 0;
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentSolution = new SolutionObject(futureSolution);
			currentTasks.add(previous);
			income += bids[agent.id()];

            //if (count == 0){
            double difference = currentSolution.getTotalCost() - (double) estimationFunction.getEstimationCost(count+1, false).get(count);
            estimationFunction.setBiais(difference, false);
            //}
		}
		double profit = income - currentSolution.getTotalCost();
		System.out.println("Agent: "+agent.id());
		System.out.println("Cost: " + currentSolution.getTotalCost());
		System.out.println("Income: " + income);
		System.out.println("Profit: " + profit);
        currentSolution.display();
        count += 1;
	}


	@Override
	public Long askPrice(Task task) {

		List<Task> myTasks = new ArrayList<Task>(currentTasks);
		myTasks.add(task);

		futureSolution = new SolutionObject(CentralizedPlan.centralizedSolution(vehicle, myTasks, timeout_bid-5000));

		double marginalCost = futureSolution.getTotalCost() - currentSolution.getTotalCost();

		if (count == 0){
            return Math.round(marginalCost);
        }

        double bid = (double) estimationFunction.getEstimationCost(count + 1, false).get(count);

		return Math.round(bid - currentSolution.getTotalCost());
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		List<Task> tasksList = new ArrayList<>(tasks);
		currentSolution = CentralizedPlan.centralizedSolution(vehicles, tasksList, timeout_plan-5000);
        double profit = income - currentSolution.getTotalCost();
        System.out.println("------------------- FINAL SOLUTION -------------------");
        System.out.println("Agent: "+agent.id());
        System.out.println("Final Profit: " + profit);
        System.out.println("\n------------------------------------------------------\n");
		return currentSolution.generatePlan();
	}
}
