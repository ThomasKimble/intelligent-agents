import java.awt.Color;
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

 * @ThomasKimble
 */

public class RabbitsGrassSimulationAgent implements Drawable
{
	
		// ---------------------------------------------- variables -----------------------------------------------
		
		private int x;
		private int y;
		private int vX;
		private int vY;
		private int energy;
		private int ID;
		private static int IDNumber = 0;
		private RabbitsGrassSimulationSpace rgsSpace;
	
		
		// -------------------------------------- rabbit agent constructor  ---------------------------------------
		
		// agent parameters
		public RabbitsGrassSimulationAgent(int minEnergy, int maxEnergy)
		{
			x  	   = -1;
			y	   = -1;
			energy = (int)((Math.random() * (maxEnergy - minEnergy)) + minEnergy);
			IDNumber++;
			ID = IDNumber;
			setVxVy();
		}
		
		// set agent velocity for random NESW movement
		public void setVxVy()
		{
			vX = 0;
			vY = 0;
			while((vX == 0) && ( vY == 0))
			{
				int direction = (int)Math.floor(Math.random() * 4);
				if(direction == 0)
				{
					vX = 1;
					vY = 0;
				}
				else if(direction == 1)
				{
					vX = -1;
					vY = 0;
				}
				else if(direction == 2)
				{
					vX = 0;
					vY = 1;
				}
				else if(direction == 3)
				{
					vX = 0;
					vY = -1;
				}
			}
		}
		
		
		// --------------------------------------- rabbit report and draw -----------------------------------------
		
		public void draw(SimGraphics arg0)
		{
			// TODO Auto-generated method stub
			if(energy > 30)
				try { //If high try to draw the rabbit image, otherwise a blue square.
					Image picture = ImageIO.read(new File("rabbit.png"));
					arg0.drawImageToFit(picture);
				} catch (IOException e) {
					arg0.drawCircle(Color.pink);
					e.printStackTrace();
				}
			else
				arg0.drawCircle(Color.red);
		}
		
		public void report() {
			System.out.println(getID() + " at ("  + x + ", " + y + ") has "  +
                    		   getEnergy() + " energy.");
		}
		
	
		// --------------------------------------------- step actions ---------------------------------------------
		
		// each step, a new position for each rabbit is determined from previous position and velocity
		public void step()
		{
			setVxVy();

			int newX = x + vX;
			int newY = y + vY;

			Object2DGrid grid = rgsSpace.getCurrentRabbitSpace();
			// torus map
		    newX = (newX + grid.getSizeX()) % grid.getSizeX();
		    newY = (newY + grid.getSizeY()) % grid.getSizeY();
		    
		    // gets 5 energy from eating 1 grass
		    if(tryMove(newX, newY))
		    {
		    	energy += rgsSpace.eatGrassAt(x, y)*5;
		    }
		    // loses 1 energy when moving 1 step
			energy--;
		}
		
		private boolean tryMove(int newX, int newY)
		{
			return rgsSpace.moveRabbitAt(x, y, newX, newY);
		}
		
		public void receiveGrass(int amount)
		{
			energy += amount;
		}
		
		
		// ------------------------------------------ get and set methods -----------------------------------------
		
		public String getID()
		{
			return "Rabbit-" + ID;
		}
		
		
		public int getEnergy()
		{
			return energy;
		}
		
		public void setEnergy(int newEnergy)
		{
			energy = newEnergy;
		}
		
		public void setXY(int newX, int newY)
		{
			x = newX;
			y = newY;
		}
		
		public int getX()
		{
			// TODO Auto-generated method stub
			return x;
		}
	
		public int getY()
		{
			// TODO Auto-generated method stub
			return y;
		}
		
		public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgss)
		{
			rgsSpace = rgss;
		}

}
