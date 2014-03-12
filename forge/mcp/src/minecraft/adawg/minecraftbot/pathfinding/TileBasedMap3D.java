package adawg.minecraftbot.pathfinding;

import org.newdawn.slick.util.pathfinding.Mover;

public interface TileBasedMap3D {
	
	/* 
	 * Notify that a location has been visited.  Use to debug new heuristics.
	 */
	public void pathFinderVisited(int x, int y, int z);
	
	/**
	 * Tell whether the given mover can occupy a given space.
	 */
	public boolean blockedDest(Mover mover, int tx, int ty, int tz);
	
	/**
	 * Tell whether the given mover can make the described move into the given destination.
	 * @param mover
	 * @param dx - tx minus startx
	 * @param dy - ty minus starty
	 * @param dz - tz minus startz
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return
	 */
	public boolean blocked(Mover mover, int dx, int dy, int dz, int tx, int ty, int tz);
	
	/*
	 * Get the cost of moving through the given cube.  Simple implementation: Return 1
	 */
	public float getCost(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz);
}
