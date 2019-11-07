import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @afresne
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;

    //Constructor
    public RabbitsGrassSimulationSpace(int xSize, int ySize){
        grassSpace = new Object2DGrid(xSize, ySize);
        agentSpace = new Object2DGrid(xSize, ySize);
        for(int i = 0; i < xSize; i++){
            for(int j = 0; j < ySize; j++){
                grassSpace.putObjectAt(i,j,new Integer(0)); //No object
            }
        }
    }


    //Grass Space
    //-----------------------------------------------------------------------------------
    public void spreadGrass(int NbGrass, int EnergyGrass){
        // Randomly place grass in rabbitsSpace
        for(int i = 0; i < NbGrass; i++){

            // Choose coordinates
            int x = (int)(Math.random()*(grassSpace.getSizeX()));
            int y = (int)(Math.random()*(grassSpace.getSizeY()));

            // Get the value of the object at those coordinates
            int I;
            if(grassSpace.getObjectAt(x,y)!= null){
                I = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
            }
            else{
                I = 0;
            }
            // Replace the Integer object with another one with the new value
            grassSpace.putObjectAt(x,y,new Integer(I + EnergyGrass));
        }
    }

    public int takeGrassAt(int x, int y){
        int grass = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0)); //Set the case in 0 -> no more grass
        return grass;
    }

    public int getGrassAt(int x, int y){
        int i;
        if(grassSpace.getObjectAt(x,y)!= null){
            i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
        }
        else{
            i = 0;
        }
        return i;
    }

    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }



    //Agent Space
    //-----------------------------------------------------------------------------------
    public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

        while((retVal==false) && (count < countLimit)){
            int x = (int)(Math.random()*(agentSpace.getSizeX()));
            int y = (int)(Math.random()*(agentSpace.getSizeY()));
            if(isCellOccupied(x,y) == false){
                agentSpace.putObjectAt(x,y,agent);
                agent.setXY(x,y);
                agent.setRabbitsGrassSimulationSpace(this);
                retVal = true;
            }
            count++;
        }

        return retVal;
    }

    public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if(!isCellOccupied(newX, newY)){
            RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
            removeAgentAt(x,y);
            rgsa.setXY(newX, newY);
            agentSpace.putObjectAt(newX, newY, rgsa);
            retVal = true;
        }
        return retVal;
    }

    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
    }

    public void removeAgentAt(int x, int y){
        agentSpace.putObjectAt(x, y, null);
    }

    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
    }
}
