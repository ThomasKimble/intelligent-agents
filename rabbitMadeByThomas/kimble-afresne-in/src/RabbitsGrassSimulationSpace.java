import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @ThomasKimble
 */

public class RabbitsGrassSimulationSpace
{
		private Object2DGrid grassSpace;
		private Object2DGrid rabbitSpace;

		//--------------------------------------------- grid space ------------------------------------------------
		
		public RabbitsGrassSimulationSpace(int gridSize)
		{	
			grassSpace  = new Object2DGrid(gridSize, gridSize);
			rabbitSpace = new Object2DGrid(gridSize, gridSize);
			
			for(int i = 0; i < gridSize; i++)
			{
				for(int j = 0; j < gridSize; j++)
				{
					grassSpace.putObjectAt(i, j, new Integer(0));
				}
			}
		}
		
		
		//--------------------------------------- spread grass randomly -------------------------------------------
		
		public void spreadGrass(int numInitGrass)
		{			
		    // Randomly places grass in grassSpace
		    for(int i = 0; i < numInitGrass; i++)
		    {
		
				// Choose coordinates
				int x = (int)(Math.random()*(grassSpace.getSizeX()));
				int y = (int)(Math.random()*(grassSpace.getSizeY()));
				
				// Get the value of the object at those coordinates
				int currentValue = getGrassAt(x, y);
				
				// Replace the Integer object with another one with the new value
				grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
			}
		}
		
		
		// gets amount of grass at position (x,y)
		public int getGrassAt(int x, int y)
		{
			int i;
			
			if(grassSpace.getObjectAt(x,y)!= null)
				i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
			else i = 0;
			
			return i;
		}
		
		//----------------------------------- get grass and rabbit space ------------------------------------------
		
		public Object2DGrid getCurrentGrassSpace()
		{
			return grassSpace;
		}
		
		public Object2DGrid getCurrentRabbitSpace()
		{
			return rabbitSpace;
		}
		
		//--------------------------------- check cell, add/remove rabbit -----------------------------------------
		
		// checks if cell is occupied to avoid collision
		public boolean isCellOccupied(int x, int y)
		{
			boolean retVal = false;
			
			if(rabbitSpace.getObjectAt(x, y) != null)
				retVal = true;
			
			return retVal;
		}
		
		
		// adds a rabbit randomly in the space
		public boolean addRabbit(RabbitsGrassSimulationAgent rabbit)
		{
			boolean retVal 	   = false;
			int 	count	   = 0;
			int 	countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();
			
		    while((retVal==false) && (count < countLimit))
		    {
		        int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
		        int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
		        
		        if(isCellOccupied(x,y) == false)
		        {
		          rabbitSpace.putObjectAt(x,y,rabbit);
		          rabbit.setXY(x,y);
		          rabbit.setRabbitsGrassSimulationSpace(this);
		          retVal = true;
		        }
		        count++;
		    }
		    return retVal;
		}
		
		
		// removes rabbit at position (x,y)
		public void removeRabbitAt(int x, int y)
		{
		    rabbitSpace.putObjectAt(x, y, null);
		}
		
		
		//----------------------------------------- eat grass method ----------------------------------------------
		
		public int eatGrassAt(int x, int y)
		{
		    int grass = getGrassAt(x, y);
		    grassSpace.putObjectAt(x, y, new Integer(0));
		    return grass;
		}
		
		
		//---------------------------------------- move rabbit method ---------------------------------------------
		
		// moves rabbit to new position (newX,newY) from current position (x,y)
		public boolean moveRabbitAt(int x, int y, int newX, int newY)
		{
			boolean retVal = false;
		    if(!isCellOccupied(newX, newY))
		    {
		    	RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)rabbitSpace.getObjectAt(x, y);
		    	removeRabbitAt(x,y);
		    	rgsa.setXY(newX, newY);
		    	rabbitSpace.putObjectAt(newX, newY, rgsa);
		    	retVal = true;
		    }
		    return retVal;
		}
		
		//------------------------------------------ get rabbit at -----------------------------------------------
		
		public RabbitsGrassSimulationAgent getRabbitAt(int x, int y)
		{
		    RabbitsGrassSimulationAgent retVal = null;
		    
		    if(rabbitSpace.getObjectAt(x, y) != null)
		    {
		    	retVal = (RabbitsGrassSimulationAgent)rabbitSpace.getObjectAt(x,y);
		    }
		    return retVal;
		  }
		
		
		//------------------------------------------ get total grass ---------------------------------------------
		
		public double getTotalGrass()
		{
		    int totalGrass = 0;
		    for(int i = 0; i < rabbitSpace.getSizeX(); i++)
		    {
		    	for(int j = 0; j < rabbitSpace.getSizeY(); j++)
		    	{
		    		totalGrass += getGrassAt(i,j);
		    	}
		    }
		    return 0.02 * totalGrass;
		}
}
