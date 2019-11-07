import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @afresne
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private int moveChoice;
	private int vX;
	private int vY;
	private int energyLevel;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgsSpace;

	// Constructor
	public RabbitsGrassSimulationAgent(int EnergyInitRabbits){
		x = -1;
		y = -1;
		energyLevel = EnergyInitRabbits;
		IDNumber++;
		ID = IDNumber;
	}

	// Define all the actions realized by an agent at each step
	public void step(){
		setVxVy(); // Define the movement randomly
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgsSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();
		if(tryMove(newX, newY)){
			energyLevel += rgsSpace.takeGrassAt(x, y);
		}
		else{
			setVxVy();
		}
		energyLevel--; // lose 1 energy level at each step
	}

	// Set randomly a movement (4 choices)
	private void setVxVy(){
		moveChoice = (int)Math.floor(Math.random() * 4);
		vX = 0;
		vY = 0;
		switch(moveChoice) {
			case 0:
				vX = -1;
				break;
			case 1:
				vX = 1;
				break;
			case 2:
				vY = -1;
				break;
			case 3:
				vY = 1;
				break;
		}
	}

	// Check if the agent can move (check collision)
	private boolean tryMove(int newX, int newY){
		return rgsSpace.moveAgentAt(x, y, newX, newY);
	}

	//Plot the agent
	public void draw(SimGraphics G){
		// TODO Auto-generated method stub
		if(energyLevel < 3)
			G.drawFastRoundRect(Color.red); //Red square if its energy is low
		else
			try { //If high try to draw the rabbit image, otherwise a blue square.
				Image picture = ImageIO.read(new File("rabbit.png"));
				G.drawImageToFit(picture);
			} catch (IOException e) {
				G.drawFastRoundRect(Color.blue);
				e.printStackTrace();
			}
	}

	//----------------------------------------------------------------------------------------------
	// Setter or getter
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgs){
		rgsSpace = rgs;
	}

	public void setXY(int newX, int newY){
		x = newX;
		y = newY;
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public int getEnergyLevel(){
		return energyLevel;
	}

	public String getID(){
		return "Rabbit(Agent)-" + ID;
	}

	public void report(){
		System.out.println(getID() + " at (" + x + ", " + y + ") has " + getEnergyLevel() + " energy.");
	}
}
