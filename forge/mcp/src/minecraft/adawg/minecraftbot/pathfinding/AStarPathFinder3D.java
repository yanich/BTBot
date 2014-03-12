package adawg.minecraftbot.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Mover;
import org.newdawn.slick.util.pathfinding.AStarHeuristic;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.PathFinder;
import org.newdawn.slick.util.pathfinding.heuristics.ClosestHeuristic;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

import adawg.minecraftbot.RichVec3;

public class AStarPathFinder3D {
	
	/** The set of nodes that have been searched through */
	private ArrayList closed = new ArrayList();
	/** The set of nodes that we do not yet consider fully searched */
	private SortedList open = new SortedList();
	
	/** The map being searched */
	private TileBasedMap3D map;
	
	/** The complete set of nodes across the map */
	private NodeStorage3D nodes;
	
	/** True if we allow diaganol movement */
	private boolean allowDiagMovement;
	/** The heuristic we're applying to determine which nodes to search first */
	private AStarHeuristic3D heuristic;
	
	/**
	 * Create a path finder with the default heuristic - closest to target.
	 * 
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diaganol movement
	 */
	public AStarPathFinder3D(TileBasedMap3D map, boolean allowDiagMovement, int dimX, int dimY, int dimZ) {
		this(map, allowDiagMovement, new ClosestHeuristic3D(), dimX, dimY, dimZ);
	}

	/**
	 * Create a path finder 
	 * 
	 * @param heuristic The heuristic used to determine the search order of the map
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diaganol movement
	 */
	public AStarPathFinder3D(TileBasedMap3D map, 
						   boolean allowDiagMovement, AStarHeuristic3D heuristic, int dimX, int dimY, int dimZ) {
		this.heuristic = heuristic;
		this.map = map;
		this.allowDiagMovement = allowDiagMovement;
		
		nodes = new NodeStorage3DArray(dimX, dimY, dimZ);
	}
	
	public void resetNodeStorage(int dimX, int dimY, int dimZ, int xCenter, int yOffset, int zCenter) {
		nodes = new NodeStorage3DArray(dimX, dimY, dimZ, xCenter, yOffset, zCenter);
	}
	
	public Path3D findPath(Mover mover, RichVec3 start, RichVec3 end, int maxSearchDistance, AtomicBoolean shouldStop) {
		if (start == null || end == null) {
			throw new IllegalArgumentException("Null RichVec3 passed to findPath()");
		}
		return findPath(mover, start.getXFloored(), start.getYFloored(), start.getZFloored(), end.getXFloored(), end.getYFloored(), end.getZFloored(), maxSearchDistance, shouldStop);
	}
	
	/**
	 * Find a path from the start to the target, avoiding blockages and honoring heuristic costs.
	 */
	public Path3D findPath(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz, int maxSearchDistance, AtomicBoolean shouldStop) {
		// easy first check, if the destination is blocked or invalid, u cant go in
		if (!nodes.validCoordinates(tx, ty, tz) || map.blockedDest(mover, tx, ty, tz)) {
			return null;
		}
		
		// If start and end are the same return an empty path
		if (sx == tx && sy == ty && sz == tz) {
			return new Path3D();
		}
		
		// initial state for A*. The closed group is empty. Only the starting
		// tile is in the open list and it's cost is zero, i.e. we're already there
		nodes.get(sx, sy, sz).cost = 0;
		nodes.get(sx, sy, sz).depth = 0;
		closed.clear();
		open.clear();
		open.add(nodes.get(sx, sy, sz));
		
		nodes.get(tx, ty, tz).parent = null;
		
		// while we haven't found the goal and haven't exceeded our max search depth
		int maxDepth = 0;
		while ((maxDepth < maxSearchDistance) && (open.size() != 0)) {
			// pull out the first node in our open list, this is determined to 
			// be the most likely to be the next step based on our heuristic
			Node current = getFirstInOpen();
			if (current == nodes.get(tx, ty, tz)) {
				break;
			}
			
			removeFromOpen(current);
			addToClosed(current);
			
			// search through all the neighbours of the current node evaluating
			// them as next steps
			for (int x=-1;x<2;x++) {
				for (int y=-1;y<2;y++) {
					for (int z=-1;z<2;z++) {
						if (shouldStop.get()) return null;
						// not a neighbour, its the current tile
						if ((x == 0) && (y == 0) && (z == 0)) {
							continue;
						}
					
						// if we're not allowing diagonal movement then only 
						// one of x, y, and z can be set
						if (!allowDiagMovement) {
							if (x != 0 && z != 0) {
								continue;
							}
						}
						
						// determine the location of the neighbour and evaluate it
						int xp = x + current.x;
						int yp = y + current.y;
						int zp = z + current.z;
					
						maxDepth = processNeighbor(mover, tx, ty, tz, maxDepth,
								current, xp, yp, zp);
					}
				}
			}
			// TODO process possible jumps
			
		}

		// since we've got an empty open list or we've run out of search 
		// there was no path. Just return null
		if (nodes.get(tx, ty, tz).parent == null) {
			return null;
		}
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		Path3D path = new Path3D();
		Node target = nodes.get(tx, ty, tz);
		//while (!(target == nodes.get(sx, sy, sz))) {
		while (target.depth > 0) {	
			path.prependStep(target.x, target.y, target.z);
			target = target.parent;
		}
		path.prependStep(sx,sy,sz);
		
		// thats it, we have our path 
		return path;
	}

	private int processNeighbor(Mover mover, int tx, int ty, int tz,
			int maxDepth, Node current, int xp, int yp, int zp) {
		if (isValidLocation(mover,current.x,current.y,current.z,xp,yp,zp)) {
			// the cost to get to this node is cost the current plus the movement
			// cost to reach this node. Note that the heursitic value is only used
			// in the sorted open list
			float nextStepCost = current.cost + getMovementCost(mover, current.x, current.y, current.z, xp, yp, zp);
			Node neighbour = nodes.get(xp, yp, zp);
			map.pathFinderVisited(xp, yp, zp);
		
			// if the new cost we've determined for this node is lower than 
			// it has been previously makes sure the node hasn't been discarded. We've
			// determined that there might have been a better path to get to
			// this node so it needs to be re-evaluated
			if (nextStepCost < neighbour.cost) {
				if (inOpenList(neighbour)) {
					removeFromOpen(neighbour);
				}
				if (inClosedList(neighbour)) {
					removeFromClosed(neighbour);
				}
			}
		
			// if the node hasn't already been processed and discarded then
			// reset its cost to our current cost and add it as a next possible
			// step (i.e. to the open list)
			if (!inOpenList(neighbour) && !(inClosedList(neighbour))) {
				neighbour.cost = nextStepCost;
				neighbour.heuristic = getHeuristicCost(mover, xp, yp, zp, tx, ty, tz);
				maxDepth = Math.max(maxDepth, neighbour.setParent(current));
				addToOpen(neighbour);
			}
		}
		return maxDepth;
	}

	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	protected Node getFirstInOpen() {
		return (Node) open.first();
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	protected void addToOpen(Node node) {
		open.add(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	protected boolean inOpenList(Node node) {
		return open.contains(node);
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	protected void removeFromOpen(Node node) {
		open.remove(node);
	}
	
	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	protected void addToClosed(Node node) {
		closed.add(node);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	protected boolean inClosedList(Node node) {
		return closed.contains(node);
	}
	
	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	protected void removeFromClosed(Node node) {
		closed.remove(node);
	}
	
	/**
	 * Check if a given location is valid for the supplied mover
	 * 
	 * @param mover The mover that would hold a given location
	 * @param sx The starting x coordinate
	 * @param sy The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @return True if the location is valid for the given mover
	 */
	protected boolean isValidLocation(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		//boolean invalid = (x < 0) || (y < 2) || (z < 0) || (x >= map.getDimX()) || (y >= map.getDimY() || (z >= map.getDimZ()));
		// Check if the location is inside nodes.
		boolean invalid = (ty < 2) || !nodes.validCoordinates(tx, ty, tz);
		
		// Check if the location is blocked.
		if ((!invalid) && ((sx != tx) || (sy != ty) || (sz != tz))) {
			//invalid = map.blocked(mover, x, y, z);
			invalid = map.blocked(mover, sx, sy, sz, tx, ty, tz);
		}
		
		return !invalid;
	}
	
	/**
	 * Get the cost to move through a given location
	 * 
	 * @param mover The entity that is being moved
	 * @param sx The x coordinate of the tile whose cost is being determined
	 * @param sy The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The cost of movement through the given tile
	 */
	public float getMovementCost(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		return map.getCost(mover, sx, sy, sz, tx, ty, tz);
	}

	/**
	 * Get the heuristic cost for the given location. This determines in which 
	 * order the locations are processed.
	 * 
	 * @param mover The entity that is being moved
	 * @param x The x coordinate of the tile whose cost is being determined
	 * @param y The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The heuristic cost assigned to the tile
	 */
	public float getHeuristicCost(Mover mover, int x, int y, int z, int tx, int ty, int tz) {
		return heuristic.getCost(map, mover, x, y, z, tx, ty, tz);
	}
	
	
	/**
	 * A simple sorted list
	 *
	 * @author kevin
	 */
	private class SortedList {
		/** The list of elements */
		private ArrayList list = new ArrayList();
		
		/**
		 * Retrieve the first element from the list
		 *  
		 * @return The first element from the list
		 */
		public Object first() {
			return list.get(0);
		}
		
		/**
		 * Empty the list
		 */
		public void clear() {
			list.clear();
		}
		
		/**
		 * Add an element to the list - causes sorting
		 * 
		 * @param o The element to add
		 */
		public void add(Object o) {
			list.add(o);
			Collections.sort(list);
		}
		
		/**
		 * Remove an element from the list
		 * 
		 * @param o The element to remove
		 */
		public void remove(Object o) {
			list.remove(o);
		}
	
		/**
		 * Get the number of elements in the list
		 * 
		 * @return The number of element in the list
 		 */
		public int size() {
			return list.size();
		}
		
		/**
		 * Check if an element is in the list
		 * 
		 * @param o The element to search for
		 * @return True if the element is in the list
		 */
		public boolean contains(Object o) {
			return list.contains(o);
		}
	}
	
	/**
	 * A single node in the search graph
	 */
	public static class Node implements Comparable {
		/** The x coordinate of the node */
		private int x;
		/** The y coordinate of the node */
		private int y;
		/** The z coordinate of the node */
		private int z;
		/** The path cost for this node */
		private float cost;
		/** The parent of this node, how we reached it in the search */
		private Node parent;
		/** The heuristic cost of this node */
		private float heuristic;
		/** The search depth of this node */
		private int depth;
		
		/**
		 * Create a new node
		 * 
		 * @param x The x coordinate of the node
		 * @param y The y coordinate of the node
		 */
		public Node(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		/**
		 * Set the parent of this node
		 * 
		 * @param parent The parent node which lead us to this node
		 * @return The depth we have no reached in searching
		 */
		public int setParent(Node parent) {
			depth = parent.depth + 1;
			this.parent = parent;
			
			return depth;
		}
		
		/**
		 * @see Comparable#compareTo(Object)
		 */
		public int compareTo(Object other) {
			Node o = (Node) other;
			
			float f = heuristic + cost;
			float of = o.heuristic + o.cost;
			
			if (f < of) {
				return -1;
			} else if (f > of) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
