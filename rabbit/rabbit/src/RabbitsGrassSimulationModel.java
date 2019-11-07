import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @afresne
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {
	// Default Values
	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 10;
	private static final int ENERGYINITRABBITS = 10;
	private static final int NUMINITGRASS = 30;
	private static final int ENERGYGRASS = 4;
	private static final int GRASSGROWTHRATE = 10;
	private static final int BIRTHTHRESHOLD = 20;


	private int GridSize = GRIDSIZE;
	private int NumInitRabbits = NUMINITRABBITS;
	private int EnergyInitRabbits = ENERGYINITRABBITS;
	private int NumInitGrass = NUMINITGRASS;
	private int EnergyGrass = ENERGYGRASS;
	private int GrassGrowthRate = GRASSGROWTHRATE;
	private int BirthThreshold = BIRTHTHRESHOLD;

	private Schedule schedule;

	private RabbitsGrassSimulationSpace rgsSpace;

	private ArrayList agentList;

	private DisplaySurface displaySurf;

	private OpenSequenceGraph numberOfRabbitAndGrassEnergyInSpace;


	//For displaying graph
	class rabbitsInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (int)countLivingAgents();
		}
	}
	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}

		public double getSValue() {
			return (int)countGrassEnergy();
		}
	}

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurf.display();
		numberOfRabbitAndGrassEnergyInSpace.display();
	}

	public void setup() {
		System.out.println("Running setup");
		rgsSpace = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;

		if (numberOfRabbitAndGrassEnergyInSpace != null){
			numberOfRabbitAndGrassEnergyInSpace.dispose();
		}
		numberOfRabbitAndGrassEnergyInSpace = null;

		//Create display
		displaySurf = new DisplaySurface(this, "Carry Drop Model Window 1");
		numberOfRabbitAndGrassEnergyInSpace = new OpenSequenceGraph("Number Of Rabbits and Grass Energy In Space",this);

		//Register display
		registerDisplaySurface("Carry Drop Model Window 1", displaySurf);
		this.registerMediaProducer("Plot", numberOfRabbitAndGrassEnergyInSpace);

	}

	public void buildModel(){
		System.out.println("Running BuildModel");
		rgsSpace = new RabbitsGrassSimulationSpace(GridSize, GridSize);
		rgsSpace.spreadGrass(NumInitGrass, EnergyGrass);

		for(int i = 0; i < NumInitRabbits; i++){
			addNewAgent();
		}
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
			rgsa.report();
		}
	}

	public void buildSchedule(){
		System.out.println("Running BuildSchedule");

		class RabbitsGrassSimulationStep extends BasicAction {
			public void execute() {
				rgsSpace.spreadGrass(GrassGrowthRate, EnergyGrass); // to have grass at the beginning
				SimUtilities.shuffle(agentList);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
					rgsa.step();
					if (rgsa.getEnergyLevel() > BirthThreshold){ //Rabbits birth
						addNewAgent();
					}
				}
				reapDeadAgents();
				displaySurf.updateDisplay();
			}
		}
		schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());

		class RabbitsGrassSimulationCountLiving extends BasicAction {
			public void execute(){
				countLivingAgents();
			}
		}
		schedule.scheduleActionAtInterval(5, new RabbitsGrassSimulationCountLiving());

		class CarryDropUpdateRabbitsAndGrassEnergyInSpace extends BasicAction {
			public void execute(){
				numberOfRabbitAndGrassEnergyInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(5, new CarryDropUpdateRabbitsAndGrassEnergyInSpace());
	}

	public void buildDisplay(){
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		for(int i = 1; i<1000; i=i+1){
			map.mapColor(i, Color.green);
		}
		map.mapColor(0, Color.black);

		Value2DDisplay displayGrass = new Value2DDisplay(rgsSpace.getCurrentGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(rgsSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass, the field");
		displaySurf.addDisplayableProbeable(displayAgents, "Agents");

		numberOfRabbitAndGrassEnergyInSpace.addSequence("Rabbit In Space", new rabbitsInSpace());
		numberOfRabbitAndGrassEnergyInSpace.addSequence("Grass Energy In Space", new grassInSpace());
	}

	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(EnergyInitRabbits);
		agentList.add(a);
		rgsSpace.addAgent(a);
	}

	private void reapDeadAgents(){
		for(int i = (agentList.size() - 1); i >= 0 ; i--){
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(rgsa.getEnergyLevel() == 0){
				rgsSpace.removeAgentAt(rgsa.getX(), rgsa.getY());
				agentList.remove(i);
			}
		}
	}

	private int countLivingAgents(){
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(rgsa.getEnergyLevel() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	private int countGrassEnergy(){
		int grassEnergyInSpace = 0;
		for(int i = 0; i < GridSize; i++) {
			for (int j = 0; j < GridSize; j++) {
				grassEnergyInSpace += rgsSpace.getGrassAt(i, j);
			}
		}
		System.out.println("Number of grass energy available is: " + grassEnergyInSpace);

		return grassEnergyInSpace;
	}

	//---------------------------------------------------------------------------
	//Getter and setter

	public String[] getInitParam() {
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = { "GridSize", "NumInitRabbits", "EnergyInitRabbits","NumInitGrass", "EnergyGrass","GrassGrowthRate", "BirthThreshold"};
		return params;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public String getName(){
		return "Rabbit Model";
	}

	public int getGridSize() {
		return GridSize;
	}

	public void setGridSize(int gridSize) {
		GridSize = gridSize;
	}

	public int getNumInitRabbits() {
		return NumInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		NumInitRabbits = numInitRabbits;
	}

	public int getNumInitGrass() {
		return NumInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		NumInitGrass = numInitGrass;
	}

	public double getGrassGrowthRate() {
		return GrassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		GrassGrowthRate = grassGrowthRate;
	}

	public double getBirthThreshold() {
		return BirthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		BirthThreshold = birthThreshold;
	}

	public int getEnergyInitRabbits() {
		return EnergyInitRabbits;
	}

	public void setEnergyInitRabbits(int energyInitRabbits) {
		EnergyInitRabbits = energyInitRabbits;
	}

	public int getEnergyGrass() {
		return EnergyGrass;
	}

	public void setEnergyGrass(int energyGrass) {
		EnergyGrass = energyGrass;
	}
}
