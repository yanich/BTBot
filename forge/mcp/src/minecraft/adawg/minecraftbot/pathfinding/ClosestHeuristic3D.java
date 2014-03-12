package adawg.minecraftbot.pathfinding;

import org.newdawn.slick.util.pathfinding.Mover;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

public class ClosestHeuristic3D implements AStarHeuristic3D {
	public float getCost(TileBasedMap3D map, Mover mover, int x, int y, int z, int tx, int ty, int tz) {		
		float dx = tx - x;
		float dy = ty - y;
		float dz = tz - z;
		
		float result = (float) (Math.sqrt((dx*dx)+(dy*dy)+(dz*dz)));
		
		return result;
	}
}
