package adawg.minecraftbot.pathfinding;

import adawg.minecraftbot.pathfinding.AStarPathFinder3D.Node;
/**
 * An interface to an array which will be used to hold pathfinding nodes.
 * @author adawg
 *
 */
public interface NodeStorage3D {
	public Node get(int x, int y, int z);
	public boolean validCoordinates(int x, int y, int z);
	//public void set(int x, int y, int z, Node node);
}
