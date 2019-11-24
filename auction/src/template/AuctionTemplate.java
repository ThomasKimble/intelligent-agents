package template;

//the list of imports
import java.io.File;
import java.lang.reflect.Array;
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

	private int count = 0;
	private int loseCount = 0;
	private int winCount = 0;

	private ArrayList<Task> currentTasks;
	private SolutionObject currentSolution;
	private SolutionObject futureSolution;
	private predictCost estimationFunction;
    private double income = 0;

    private boolean isAboveLowerLimit = false;
    private boolean isLastTimeWon = false;
    private double adversaryBidMin = Double.MAX_VALUE;
    private double bidRatio;
    private ArrayList<ArrayList> allBids = new ArrayList<>();

	private double coefficient = 0.6;
	private double lowerLimit = 0.9;
	private double upperLimit = 2.2;


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

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		this.estimationFunction = new predictCost(topology);
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) { //WON
			currentSolution = new SolutionObject(futureSolution);
			currentTasks.add(previous);
			income += bids[agent.id()];
			winCount += 1;
            isLastTimeWon = true;

			// Retrieve the minimal bid of the adversary
            if (bids[1-agent.id()] < adversaryBidMin){
				adversaryBidMin = bids[1-agent.id()];
			}

            // Compute bidRatio = AdversaryBid / OwnBid
            if (bids[agent.id()] != 0 && bids[1-agent.id()] != 0){
            	bidRatio = (double) bids[1-agent.id()] / bids[agent.id()];
			} else if (bids[1-agent.id()] != 0){
				bidRatio = (double) bids[1-agent.id()] / 1;
			}

            // Update coefficient
            coefficient *= bidRatio;

            // Upper limit of coefficient
			if (coefficient > upperLimit){
				coefficient = upperLimit;
			}

			// Flag when the coefficient goes above the lower limit (linked with starting)
			if (coefficient > 1){
				isAboveLowerLimit = true;
			}

		} else{ //LOST
			loseCount += 1;
			isLastTimeWon = false;
			adversaryBidMin = Double.MAX_VALUE;

			// Compute bidRatio = AdversaryBid / OwnBid
			if (bids[agent.id()] != 0 && bids[1-agent.id()] != 0){
				bidRatio = (double) bids[1-agent.id()] / bids[agent.id()];
			} else if (bids[1-agent.id()] != 0){
				bidRatio = (double) bids[1-agent.id()] / 1;
			}

			// Compute the expected evolution of the bid of the adversary
			double marginalCostFuture = (double) estimationFunction.getEstimationMarginalCost(loseCount+1, false).get(loseCount);
			double marginalCostPresent = (double) estimationFunction.getEstimationMarginalCost(loseCount+1, false).get(loseCount-1);
			double marginalCostEvolution;
			if (marginalCostFuture > marginalCostPresent){
				marginalCostEvolution = 1;
			} else{
				marginalCostEvolution = marginalCostFuture / marginalCostPresent;
			}

			// Compute only if coefficient above the lower limit (linked with starting)
			if (coefficient > lowerLimit){
				coefficient *= bidRatio * marginalCostEvolution;
			}

			// Lower limit of coefficient
			if (coefficient < lowerLimit && isAboveLowerLimit){
				coefficient = lowerLimit;
			}
		}

		// Display
		double profit = income - currentSolution.getTotalCost();
		System.out.println("Agent: "+agent.id());
		System.out.println("Cost: " + currentSolution.getTotalCost());
		System.out.println("Income: " + income);
		System.out.println("Profit: " + profit);
        currentSolution.display();
		System.out.println("Coefficient: " + coefficient);
		allBids.add(new ArrayList<>(Arrays.asList(bids)));
        count += 1;
	}


	@Override
	public Long askPrice(Task task) {

		List<Task> myTasks = new ArrayList<Task>(currentTasks);
		myTasks.add(task);

		futureSolution = new SolutionObject(CentralizedPlan.centralizedSolution(vehicle, myTasks, timeout_bid-5000));

		double marginalCost = futureSolution.getTotalCost() - currentSolution.getTotalCost();

        double bid;
        if (marginalCost <= 0) {
            bid = coefficient *(estimationFunction.getDistanceMean());
        } else {
        	bid = coefficient * marginalCost;
        }

        if (isLastTimeWon){
			if (bid <= 0.7*adversaryBidMin){
				bid = 0.7*adversaryBidMin;
			}
		}

		return Math.round(bid);
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		List<Task> tasksList = new ArrayList<>(tasks);
		currentSolution = CentralizedPlan.centralizedSolution(vehicles, tasksList, timeout_plan-5000);
        double profit = income - currentSolution.getTotalCost();
        System.out.println("------------------- FINAL SOLUTION -------------------");
        System.out.println("------------------- Thomas & Jules -------------------");
        System.out.println("Agent: "+agent.id());
        currentSolution.display();
        System.out.println("Final Cost: " + currentSolution.getTotalCost());
        System.out.println("Final Income: " + income);
        System.out.println("Final Profit: " + profit);
        System.out.println(allBids);
        System.out.println("\n------------------------------------------------------\n");
		return currentSolution.generatePlan();
	}
}
