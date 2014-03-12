package adawg.minecraftbot.pathfinding;

import org.newdawn.slick.util.pathfinding.Mover;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

import net.minecraft.client.Minecraft;
import net.minecraft.*;
import net.minecraft.block.material.Material;
import net.minecraft.src.*;

public class MinecraftMap implements TileBasedMap3D {
	
	private Minecraft mc;
	
	public MinecraftMap() {
		mc = Minecraft.getMinecraft();
	}

	@Override
	public void pathFinderVisited(int x, int y, int z) {
		// Called when point x,y is searched
	}
	
	// True if 2 or more or the input values are nonzero.
	private boolean isDiagonal(int x, int y, int z) {
		if (y != 0) {
			return x != 0 || z != 0;
		} else {
			return x != 0 && z != 0;
		}
	}
	
	@Override
	public boolean blocked(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		return blockedDest(mover, tx, ty, tz) || blockedMove(mover, sx, sy, sz, tx, ty, tz);
	}
	
	/**
	 * Tell whether the target square is impossible/pointless to go in.  
	 * @param mover
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return
	 */
	@Override
	public boolean blockedDest(Mover mover, int tx, int ty, int tz) {
		Material lava = Material.lava;
		Material head = mc.theWorld.getBlockMaterial(tx, ty, tz);
		Material waist = mc.theWorld.getBlockMaterial(tx, ty - 1 , tz);
		Material ground = mc.theWorld.getBlockMaterial(tx, ty - 2, tz);
		//return !ground.blocksMovement() || waist.blocksMovement() || head.blocksMovement() || head == lava || waist == lava || ground == lava;
		return waist.blocksMovement() || head.blocksMovement() || head == lava || waist == lava || ground == lava;
		
		/*
		if (ground.blocksMovement()) {
			return waist.blocksMovement() || head.blocksMovement();
		}
		*/
		//return true;
	}
	
	/**
	 *  Determines whether a move is allowed.
	 * @param mover
	 * @param sx
	 * @param sy
	 * @param sz
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return
	 */
	private boolean blockedMove(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		boolean blocked = false;
		if (ty < sy) { // going downward
			Material groundStart = mc.theWorld.getBlockMaterial(sx, sy - 2, sz);
			if (tx == sx && tz == sz) { // Straight down
				// Can't fall down if you're standing on a block
				blocked = blocked || groundStart.blocksMovement();
			} else { 
				// Check if you can walk forward and fall down
				Material waistAhead = mc.theWorld.getBlockMaterial(tx, ty - 1, tz);
				Material headAhead = mc.theWorld.getBlockMaterial(tx, ty, tz);
				Material headAbove = mc.theWorld.getBlockMaterial(tx, sy, tz);
				blocked = blocked 
						|| !groundStart.blocksMovement() //Don't try to fall horizontally in midair
						|| headAbove.blocksMovement() //This block will block you from walking forward
						|| headAhead.blocksMovement() 
						|| waistAhead.blocksMovement();
			}
		} else { // not going downward
			//If jumping straight up
			if (LocationUtils.jumpable(mover, sx, sy, sz, tx, ty, tz)) {
				return false;
			} else	{
				// Check for ground ahead, in order to stand on it
				Material groundAhead = mc.theWorld.getBlockMaterial(tx, ty - 2, tz);
				blocked = blocked || !groundAhead.blocksMovement();
				
				// Check for ground underneath you, in order to stand on it
				Material groundStart = mc.theWorld.getBlockMaterial(sx, sy - 2, sz);
				blocked = blocked || !groundStart.blocksMovement();
				
				if (ty > sy) { // Above you
					// Check for block which would prevent jumping
					Material aboveHeadS = mc.theWorld.getBlockMaterial(sx, sy + 1, sz);
					blocked = blocked || aboveHeadS.blocksMovement();
				}
			}
		}
		
		// Diagonal movement 
		if (tx != sx && tz != sz) {
			int dx = tx - sx;
			int dy = ty - sy;
			int dz = tz - sz;
			Material waistAheadX = mc.theWorld.getBlockMaterial(tx, ty - 1, sz);
			Material headAheadX = mc.theWorld.getBlockMaterial(tx, ty, sz);
			Material waistAheadZ = mc.theWorld.getBlockMaterial(sx, ty - 1, tz);
			Material headAheadZ = mc.theWorld.getBlockMaterial(sx, ty, tz);
			boolean blockedX = waistAheadX.blocksMovement() || headAheadX.blocksMovement();
			boolean blockedZ = waistAheadZ.blocksMovement() || headAheadZ.blocksMovement();
			if (dy > 0) {
				blocked = blocked || (blockedX || blockedZ);
			} else {
				blocked = blocked || (blockedX && blockedZ);
			}
		}
		
		return blocked;
	}

	@Override
	/**
	 * A simple implementation of this would be 'return 1;'
	 * @param mover - the mover to calculate cost for
	 * @param sx - start x coord
	 * @param sy
	 * @param sz
	 * @param tx - end x coord
	 * @param ty
	 * @param tz
	 */
	public float getCost(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		Material head = mc.theWorld.getBlockMaterial(tx, ty, tz);
		Material waist = mc.theWorld.getBlockMaterial(tx, ty - 1 , tz);
		Material ground = mc.theWorld.getBlockMaterial(tx, ty - 2, tz);
		Material water = Material.water;
		
		float cost = 1;
		if (head == water || waist == water || ground == water)
			cost += 5;
//		
//		if (ty > sy) {
//			cost += 0.5;
//		}
		
		return cost;
	}
	
}
