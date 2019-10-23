import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenHistogram;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @ThomasKimble
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {	
	
		// ---------------------------------------------- variables -----------------------------------------------
	
		// Default Values
		private static final int GRIDSIZE 		 = 20;
		private static final int NUMINITRABBITS  = 5;
		private static final int NUMINITGRASS 	 = 100;
		private static final int GRASSGROWTHRATE = 5;
		private static final int BIRTHTHRESHOLD  = 175;
		private static final int MIN_ENERGY 	 = 50;
		private static final int MAX_ENERGY 	 = 100;
			
		private int gridSize 		= GRIDSIZE;
		private int numInitRabbits 	= NUMINITRABBITS;
		private int numInitGrass 	= NUMINITGRASS;
		private int grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold 	= BIRTHTHRESHOLD;
		private int minEnergy 		= MIN_ENERGY;
		private int maxEnergy		= MAX_ENERGY;
		
		private Schedule schedule;
		
		private RabbitsGrassSimulationSpace rgsSpace;
		
		private ArrayList rabbitList;
		
		private DisplaySurface displaySurf;
		
		private OpenSequenceGraph amountOfGrassAndRabbitsInSpace;
		private OpenHistogram rabbitEnergyDistribution;
		
		
		// ------------------------------------------- data and graphs --------------------------------------------
	
		class grassInSpace implements DataSource, Sequence
		{
		    public Object execute()
		    {
		      return new Double(getSValue());
		    }

		    public double getSValue()
		    {
		      return (double)rgsSpace.getTotalGrass();
		    }
		}
		
		
		class livingRabbits implements DataSource, Sequence
		{
			public Object execute()
			{
				return new Double(getSValue());
			}
			
			public double getSValue()
			{
				return (double)countLivingRabbits();
			}
		}
		
		
		class rabbitEnergy implements BinDataSource
		{
		    public double getBinValue(Object o)
		    {
		    	RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)o;
		    	return (double)rgsa.getEnergy();
		    }
		}
		
		
		// --------------------------------------------- setup method ---------------------------------------------

		public void setup()
		{
			// TODO Auto-generated method stub
			System.out.println("Running setup");
			rgsSpace = null;
			rabbitList = new ArrayList();
			schedule = new Schedule(1);
			
			// Tear down Displays
		    if (displaySurf != null){
				displaySurf.dispose();
			}
		    displaySurf = null;
		    
		    if (amountOfGrassAndRabbitsInSpace != null){
				amountOfGrassAndRabbitsInSpace.dispose();
			}
		    amountOfGrassAndRabbitsInSpace = null;
		    
		    if (rabbitEnergyDistribution != null) {
		    	rabbitEnergyDistribution.dispose();
		    }
		    rabbitEnergyDistribution = null;
		    
		    // Create Displays
		    displaySurf = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 1");
		    amountOfGrassAndRabbitsInSpace  = new OpenSequenceGraph("Amount Of Grass and Rabbits In Space",this);
		    rabbitEnergyDistribution = new OpenHistogram("Rabbit Energy", 8, 0);
		    
		    // Register Displays
		    registerDisplaySurface("Rabbit Grass Simulation Model Window ", displaySurf);
		    this.registerMediaProducer("Plot", amountOfGrassAndRabbitsInSpace);
		}
		
		
		// ----------------------------------- begin method to call others ---------------------------------------

		public void begin()
		{
			// TODO Auto-generated method stub
			// calls build methods
			buildModel();
			buildSchedule();
			buildDisplay();
			
			// starts displays
			displaySurf.display();
			amountOfGrassAndRabbitsInSpace.display();
			rabbitEnergyDistribution.display();
		}
		
		
		// ---------------------------------------- build model methods ------------------------------------------
		
		public void buildModel()
		{
			System.out.println("Running BuildModel");
			rgsSpace = new RabbitsGrassSimulationSpace(gridSize);
			rgsSpace.spreadGrass(numInitGrass);
			
			for(int i = 0; i < numInitRabbits; i++)
				addNewRabbit();
			
			for(int i = 0; i < rabbitList.size(); i++)
			{
			      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
			      rgsa.report();
			}
		}
		
		// Adds new rabbit to list and to rgs space
		private void addNewRabbit()
		{
			RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(minEnergy, maxEnergy);
			if(rgsSpace.addRabbit(a)){
				rabbitList.add(a);
			}
		}
		
		
		// --------------------------------------- build schedule methods ----------------------------------------

		public void buildSchedule()
		{
			System.out.println("Running BuildSchedule");
			
			
			// Shuffles rabbitList every step and adds a new rabbit when one dies
		    class RabbitsGrassSimulationStep extends BasicAction
		    {
		        public void execute()
		        {
		        	SimUtilities.shuffle(rabbitList);
		        	for(int i =0; i < rabbitList.size(); i++)
		        	{
		        		RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
		        		rgsa.step();
		        	}
		        	reproduceRabbits();
		        	removeDeadRabbits();
		        	displaySurf.updateDisplay();
		        }
		    }
		    schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());
		    
		    
		    // count living rabbits in simulation
		    class RabbitsGrassSimulationCountLiving extends BasicAction
		    {
		    	public void execute()
		    	{
		    		countLivingRabbits();
		        }
		    }
		    schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());
		    
		    
		    // spreads grassGrowthRate grass randomly every 10 steps
		    class RabbitsGrassSimulationGrowGrass extends BasicAction
		    {
		    	public void execute()
		    	{
		    		rgsSpace.spreadGrass(grassGrowthRate);
		        }
		    }
		    schedule.scheduleActionAtInterval(1, new RabbitsGrassSimulationGrowGrass());
		    
		    
		    // update grass data for graph
		    class RabbitsGrassSimulationUpdateGrassInSpace extends BasicAction
		    {		
		        public void execute()
		        {		
		          amountOfGrassAndRabbitsInSpace.step();		
		        }		
		    }		
		    schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassInSpace());
		    
		    
		    // update rabbit energy data for histogram
		    class RabbitsGrassSimulationUpdateRabbitEnergy extends BasicAction
		    {
		        public void execute()
		        {
		          rabbitEnergyDistribution.step();
		        }
		    }
		    schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateRabbitEnergy());
		}
		
		
		private int countLivingRabbits()
		{
			int livingRabbits = 0;
			
			for(int i = 0; i < rabbitList.size(); i++)
			{
				RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
				if(rgsa.getEnergy() > 0) livingRabbits++;
			}
			return livingRabbits;
		}
		
		
		private int removeDeadRabbits()
		{
			int count = 0;
		    for(int i = (rabbitList.size() - 1); i >= 0 ; i--)
		    {
		    	RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
		    	if(rgsa.getEnergy() < 1)
		    	{
		    		rgsSpace.removeRabbitAt(rgsa.getX(), rgsa.getY());
		    		rabbitList.remove(i);
		    		count++;
		    	}
		    }
		    return count;
		}
		
		
		// after rabbit energy reaches threshold, energy resets randomly
		// and new rabbit is created in random position
		private int reproduceRabbits()
		{
			int count = 0;
		    for(int i = (rabbitList.size() - 1); i >= 0 ; i--)
		    {
		    	RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitList.get(i);
		    	if(rgsa.getEnergy() > birthThreshold)
		    	{
		    		int newEnergy = (int)((Math.random() * (maxEnergy - minEnergy)) + minEnergy);
		    		rgsa.setEnergy(newEnergy);
		    		addNewRabbit();
		    		count++;
		    	}
		    }
		    return count;
		}
		
		
		// ---------------------------------------- build display method -----------------------------------------

		public void buildDisplay()
		{
			System.out.println("Running BuildDisplay");
			ColorMap map = new ColorMap();

		    for(int i = 1; i<16; i++)
		    {
		    	map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		    }
		    map.mapColor(0, Color.black);

		    Value2DDisplay  displayGrass   = new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);
		    
		    Object2DDisplay displayRabbits = new Object2DDisplay(rgsSpace.getCurrentRabbitSpace());
		    displayRabbits.setObjectList(rabbitList);

		    displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		    displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");
		    
		    amountOfGrassAndRabbitsInSpace.addSequence("Grass In Space (*0.02)", new grassInSpace());
		    amountOfGrassAndRabbitsInSpace.addSequence("Rabbits In Space", new livingRabbits());
		    rabbitEnergyDistribution.createHistogramItem("Rabbit Energy", rabbitList, new rabbitEnergy());
		}
		
		
		// ------------------------------------------ parameters method -------------------------------------------

		public String[] getInitParam()
		{
			// TODO Auto-generated method stub
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", 
								"MinEnergy", "MaxEnergy"};
			return params;
		}
		
		
		// ----------------------------------------- get and set methods ------------------------------------------
		
		public int getGridSize()
		{
			return gridSize;
		}
		
		public void setGridSize(int gs)
		{
			gridSize = gs;
		}
		
		public int getNumInitRabbits()
		{
			return numInitRabbits;
		}
		
		public void setNumInitRabbits(int nir)
		{
			numInitRabbits = nir;
		}
		
		public int getNumInitGrass()
		{
			return numInitGrass;
		}
		
		public void setNumInitGrass(int nig)
		{
			numInitGrass = nig;
		}	
		
		public int getGrassGrowthRate()
		{
			return grassGrowthRate;
		}
		
		public void setGrassGrowthRate(int ggr)
		{
			grassGrowthRate = ggr;
		}			
		
		public int getBirthThreshold()
		{
			return birthThreshold;
		}
		
		public void setBirthThreshold(int bt)
		{
			birthThreshold = bt;
		}	
		
		public int getMinEnergy()
		{
			return minEnergy;
		}
		
		public void setMinEnergy(int min)
		{
			minEnergy = min;
		}
		
		public int getMaxEnergy()
		{
			return maxEnergy;
		}
		
		public void setMaxEnergy(int max)
		{
			maxEnergy = max;
		}
		
		public String getName()
		{
			// TODO Auto-generated method stub
			return "Rabbits Grass Simulation";
		}
		
		public Schedule getSchedule()
		{
			// TODO Auto-generated method stub
			return schedule;
		}
		
		
		// ------------------------------------------------ main --------------------------------------------------
		
		public static void main(String[] args)
		{
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
		}
}
