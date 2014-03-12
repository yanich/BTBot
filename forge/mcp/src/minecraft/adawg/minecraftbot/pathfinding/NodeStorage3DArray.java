package adawg.minecraftbot.pathfinding;
import adawg.minecraftbot.pathfinding.AStarPathFinder3D.Node;

public class NodeStorage3DArray implements NodeStorage3D {
	private Node[][][] array;
	
	/**
	 * These store the dimensions of the array for easy access.  
	 * Another option is doing array.length(), array[0].length(), and array[0][0].length()
	 */
	private int dimX;
	private int dimY;
	private int dimZ;
	
	/**
	 * These store the actual, in-game coordinates of the node at array[0][0][0].
	 */
	private int xOffset;
	private int yOffset;
	private int zOffset;
	
	public NodeStorage3DArray(int dimX, int dimY, int dimZ) {
		array = new Node[dimX][dimY][dimZ];
		this.dimX = dimX;
		this.dimY = dimY;
		this.dimZ = dimZ;
		xOffset = -(dimX / 2);
		yOffset = 0;
		zOffset = -(dimZ / 2);
	}
	
	/**
	 * The dimensions given here determine the size of the A* search volume.
	 * The offsets are the world coordinates of the node at array[0][0][0]
	 * 
	 */
	public NodeStorage3DArray(int dimX, int dimY, int dimZ, int xCenter, int yOffset, int zCenter) {
		array = new Node[dimX][dimY][dimZ];
		this.dimX = dimX;
		this.dimY = dimY;
		this.dimZ = dimZ;
		this.xOffset = -(dimX / 2) + xCenter;
		this.yOffset = yOffset;
		this.zOffset = -(dimZ / 2) + zCenter;
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the world coordinates x, y, and z correspond with 
	 * a value in the array.
	 */
	public boolean validCoordinates(int worldX, int worldY, int worldZ) {
		int arrayX = worldX - xOffset;
		int arrayY = worldY - yOffset;
		int arrayZ = worldZ - zOffset;
		return (arrayX >= 0 && arrayX < dimX) && (arrayY >= 0 && arrayY < dimY) && (arrayZ >=0 && arrayZ < dimZ);
	}
	
	/**
	 * Returns the node at world coordinates x, y, z.  
	 * If outside of the array, return null.
	 */
	public Node get(int x, int y, int z) {
		if (!validCoordinates(x, y, z)) {
			return null;
		}
		Node stored = array[x - xOffset][y - yOffset][z - zOffset];
		if (stored != null) {
			return stored; 
		} else {
			array[x - xOffset][y - yOffset][z - zOffset] = new Node(x, y, z);
			return array[x - xOffset][y - yOffset][z - zOffset];
		}
	}
}
