package adawg.minecraftbot.pathfinding;

import java.util.HashMap;
import java.util.Map;

import adawg.minecraftbot.RichVec3;
import adawg.minecraftbot.pathfinding.AStarPathFinder3D.Node;

public class NodeStorageMap implements NodeStorage3D {
	private Map<RichVec3, Node> nodeMap;
	
	public NodeStorageMap() {
		nodeMap = new HashMap<RichVec3, Node>();
	}
	@Override
	public Node get(int x, int y, int z) {
		return get(new RichVec3(x, y, z));
	}
	
	public Node get(RichVec3 loc) {
		if (nodeMap.containsKey(loc)) {
			return nodeMap.get(loc);
		} else {
//			Node node = new Node(loc.getXFloored(), loc.getYFloored(),
//					loc.getZFloored());
			Node node = new Node(loc.getXFloored(), loc.getYFloored(), 
					loc.getZFloored());
			nodeMap.put(loc, node);
			return node;
		}
	}

	/**
	 * Always true because the map can hold any coordinates pretty much that
	 * aren't like in the millions or billions
	 */
	@Override
	public boolean validCoordinates(int x, int y, int z) {
		// TODO Auto-generated method stub
		return y > 0;
	}

}
