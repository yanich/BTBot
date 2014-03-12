package adawg.minecraftbot.pathfinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import adawg.minecraftbot.behaviors.PlaceBlock;
//import adawg.minecraftbot.pathfinding.DirectionUtils.CompassDirection;
import adawg.minecraftbot.RichVec3;

//import net.minecraft.client.Minecraft;
import org.newdawn.slick.util.pathfinding.Mover;

import com.kunii.mcbot.McBot;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.block.material.Material;
import net.minecraft.util.Vec3;

public class LocationUtils {
	
	public static Minecraft mc() {
		return Minecraft.getMinecraft();
	}
	/**
	 * Returns the distance one would fall, straight downward, from the given map coordinates..
	 * Used to check for dangerous drops or whether a block can be reached from the ground.
	 * @param map
	 * @param x
	 * @param y
	 * @param z
	 * @return 
	 */
	public static int fallDistance(int x, int y, int z) {
		if (y < 2) {
			throw new IllegalArgumentException("Given y coordinate is too low (Or there is a hole in the bedrock)");
		}
		Material groundMat = mc().theWorld.getBlockMaterial(x, y - 2, z);
		if (groundMat.blocksMovement()) {
			return 0;
		} else {
			return 1 + fallDistance(x, y - 1, z);
		}
	}
	
	public static int fallDistance(RichVec3 loc) {
		return fallDistance(loc.getXFloored(), loc.getYFloored(), loc.getZFloored());
	}
	
	/**
	 * Returns distance from player location to the given loc
	 * @param loc
	 * @return
	 */
	public static double distanceTo(Vec3 loc) {
		return getPlayerLoc().distanceTo(loc);
	}
	
	/**
	 * Returns distance from the player's location to the given location.
	 * @param loc
	 * @return
	 */
	public static double distanceTo(RichVec3 loc) {
		return distanceTo(loc.getVec());
	}
	
	/**
	 * Return a pathfindable location underneath the given block
	 * @param mc
	 * @param startLoc
	 * @return
	 */
	public static Vec3 groundUnderBlock(Vec3 startLoc, int maxDistance) {
		if (maxDistance == 0) {
			System.out.println("Max distance reached in groundUnderBlock.  Unable to find ground.");
			return null;
		} else if (startLoc.yCoord < 2) { 
			System.out.println("Bottom of map reached.  Giving up.");
			return null;
		}
		Vec3 locUnder = startLoc.addVector(0, -1, 0);
		if (standableLoc((Mover)null, locUnder)) {
			return locUnder;
		} else {
			return groundUnderBlock(locUnder, maxDistance - 1);
		}
	}
	
	public static int fallDistance(Vec3 loc) {
		int x = (int)Math.floor(loc.xCoord);
		int y = (int)Math.floor(loc.yCoord);
		int z = (int)Math.floor(loc.zCoord);
		return fallDistance(x, y, z);
	}
	
	/**
	 * Tell whether the target square is impossible/pointless to go in.  Does not check for ground to stand on.
	 * @param mover
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return
	 */
	public static boolean blockedDest(Mover mover, int tx, int ty, int tz) {
		World world = McBot.getWorld();
		Material lava = Material.lava;
		Material head = world.getBlockMaterial(tx, ty, tz);
		Material waist = world.getBlockMaterial(tx, ty - 1 , tz);
		Material ground = world.getBlockMaterial(tx, ty - 2, tz);
		//return !ground.blocksMovement() || waist.blocksMovement() || head.blocksMovement() || head == lava || waist == lava || ground == lava;
		return waist.blocksMovement() || head.blocksMovement() || head == lava || waist == lava || ground == lava;
		
		/*
		if (ground.blocksMovement()) {
			return waist.blocksMovement() || head.blocksMovement();
		}
		*/
		//return true;
	}
	
	public static boolean blockedDest(Mover mover, Vec3 loc) {
		int x = (int)Math.floor(loc.xCoord);
		int y = (int)Math.floor(loc.yCoord);
		int z = (int)Math.floor(loc.zCoord);
		return blockedDest(mover, x, y, z);
	}
	
	public static boolean standableLoc(Mover mover, Vec3 loc) {
		int x = (int)Math.floor(loc.xCoord);
		int y = (int)Math.floor(loc.yCoord);
		int z = (int)Math.floor(loc.zCoord);
		return standableLoc(mover, x, y, z);
	}
	
	/**
	 * Determines whether character can stand at a location
	 * @param mover
	 * @param x
	 * @param y - head height
	 * @param z
	 * @return
	 */
	public static boolean standableLoc(Mover mover, int x, int y, int z) {
		World world = mc().theWorld;
		Material lava = Material.lava;
		Material ground = world.getBlockMaterial(x, y - 2, z);
		Material waist = world.getBlockMaterial(x, y - 1, z);
		Material head = world.getBlockMaterial(x, y, z);
		boolean isLiquid = ground.isLiquid() || waist.isLiquid() || head.isLiquid();
		boolean isBlocked = waist.blocksMovement() || head.blocksMovement();
		boolean hasGround = ground.blocksMovement();
		boolean isLava = head == lava || waist == lava || ground == lava;
		return !isLiquid && !isBlocked && hasGround && !isLava;
	}
	
	/**
	 * Figure out if the player can jump from sx,sy,sz to tx,ty,tz.
	 * Currently only allows jumping straight upward
	 * @param mover
	 * @param sx
	 * @param sy
	 * @param sz
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return true if player can jump straight up from sx,sy,sz to tx,ty,tz.
	 */
	public static boolean jumpable(Mover mover, int sx, int sy, int sz, int tx, int ty, int tz) {
		boolean straightUp = (sx == tx && sy + 1 == ty && sz == tz);
		boolean standableStart = LocationUtils.standableLoc(mover, sx, sy, sz);
		boolean blockedHead = mc().theWorld.getBlockMaterial(tx, ty, tz).blocksMovement();
		return straightUp && standableStart && !blockedHead;
	}

	private static boolean jumpable(RichVec3 start, RichVec3 target) {
		return jumpable((Mover)null, start.getXFloored(), start.getYFloored(), start.getZFloored(),
				target.getXFloored(), target.getYFloored(), target.getZFloored());
	}
	
	/**
	 * Returns true if bot can just walk straight from start to target.
	 */
	public static boolean isDirectPathBetweenPoints(Vec3 start, Vec3 target) {
		double sx = start.xCoord;
		double sy = start.yCoord;
		double sz = start.zCoord;
		double tx = target.xCoord;
		double ty = target.yCoord;
		double tz = target.zCoord;
//		 Testing this new condition check to see if it makes this behave.
		if (ty != sy) {
			return false;
		}
		
		int xStart = (int)Math.floor(sx);
		int zStart = (int)Math.floor(sz);
		double dx = tx - sx;
		double dz = tz - sz;
		double distanceSquared = dx * dx + dz * dz;
		
		// I don't understand this exactly.
		if (distanceSquared < 1E-008D) {
			return false;
		}
		
		double reciprocalDistance = 1.0D / Math.sqrt(distanceSquared);
		dx *= reciprocalDistance;
		dz *= reciprocalDistance;
		
		//if (!isSafeToStandAt(xStart, (int)par1Vec3D.yCoord, zStart, par3, par4, par5, par1Vec3D, dx, dz))
       // {
        //    return false;
        //}
		
		if (LocationUtils.blockedDest((Mover)null, (int)Math.floor(tx), (int)Math.floor(ty), (int)Math.floor(tz))) {
			return false;
		}
		
		double absDistanceOverDx = 1.0D / Math.abs(dx);
		double absDistanceOverDz = 1.0D / Math.abs(dz);
		double d6 = (double)(xStart * 1) - sx;
        double d7 = (double)(zStart * 1) - sz;

        if (dx >= 0.0D)
        {
            d6++;
        }

        if (dz >= 0.0D)
        {
            d7++;
        }

        d6 /= dx;
        d7 /= dz;
        byte byte0 = ((byte)(dx >= 0.0D ? 1 : -1));
        byte byte1 = ((byte)(dz >= 0.0D ? 1 : -1));
        int k = MathHelper.floor_double(tx);
        int l = MathHelper.floor_double(tz);
        int i1 = k - xStart;

        for (int j1 = l - zStart; i1 * byte0 > 0 || j1 * byte1 > 0;)
        {
            if (d6 < d7)
            {
                d6 += absDistanceOverDx;
                xStart += byte0;
                i1 = k - xStart;
            }
            else
            {
                d7 += absDistanceOverDz;
                zStart += byte1;
                j1 = l - zStart;
            }
           
//            if (LocationUtils.blockedDest((Mover)null, xStart, (int)Math.floor(sy), zStart)) {

//            for (int i = -1; i < 2; i++) {
//            	for (int j = -1; j < 2; j++) {
//            		if (!LocationUtils.standableLoc((Mover)null, xStart + i, (int)Math.floor(sy), zStart + j)) {
//            			return false;
//            		}
//            	}
//            }

//            if (!LocationUtils.standableLoc((Mover)null, xStart, (int)Math.floor(sy), zStart)) {
//            	return false;
//            }
            
            // This prevents over-aggressive corner cutting that used to make the bot get stuck.
            if (!LocationUtils.standableLoc((Mover)null, xStart, (int)Math.floor(sy), zStart)
            || !LocationUtils.standableLoc((Mover)null, xStart + byte0, (int)Math.floor(sy), zStart)
            || !LocationUtils.standableLoc((Mover)null, xStart, (int)Math.floor(sy), zStart + byte1))
            {
    			return false; 
            }
        }

        return true;
		
	}
	
	
	/**
	 * Returns the location of the closest wood within a radius.
	 * @param world
	 * @param startPos
	 * @param radius
	 * @return
	 */
	public static Vec3 findClosestBlock(Material targetMaterial, Vec3 vec, int maxRadius, List<RichVec3> blacklist) {
		World world = mc().theWorld;
		for (int radius = 1; radius <= maxRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						int x = (int)Math.floor(vec.xCoord) + dx;
						int y = (int)Math.floor(vec.yCoord) + dy;
						int z = (int)Math.floor(vec.zCoord) + dz;
						Material mat = world.getBlockMaterial(x, y, z);
						RichVec3 blockLoc = new RichVec3(x + 0.5, y, z + 0.5);
						if (mat == targetMaterial && !blacklist.contains(blockLoc)) {
							return blockLoc.getVec();
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
     * Horizontal distance between the two given vectors.
     */
	public static double horizontalDistance(Vec3 a, Vec3 b) {
		double d = a.xCoord - b.xCoord;
        //double d1 = a.yCoord - b.yCoord;
        double d2 = a.zCoord - b.zCoord;
        return Math.sqrt(d * d + d2 * d2);
	}
	
	/**
	 * Horizontal distance between the player's location and the given location.
	 * @param loc
	 * @return
	 */
	public static double horizontalDistanceTo(Vec3 loc) {
		return horizontalDistance(getPlayerLoc(), loc);
	}
	
	public static double horizontalDistanceTo(RichVec3 loc) {
		return horizontalDistanceTo(loc.getVec());
	}
	
	/**
	 * Return an empty pathfindable space near a block.  First looks directly underneath, then 
	 * looks at neighbors, then looks at all blocks in a 4-block radius.
	 * @param world
	 * @param block
	 * @return
	 */
	public static Vec3 spaceNearBlock(Vec3 startLoc, int maxGroundDistance, int searchRadius) {
		if (standableLoc((Mover)null, startLoc)) {
			return startLoc;
		}
		Vec3 groundUnder = LocationUtils.groundUnderBlock(startLoc, maxGroundDistance);
		//if (!LocationUtils.blockedDest(mc, (Mover)null, woodGround)) {
		if (groundUnder != null) {
			return groundUnder;
		} else {
			System.out.println("Wood is unreachable from underneath");
		}
//		for (CompassDirection dir : CompassDirection.values()) {
//			Vec3 newLoc = DirectionUtils.getLocationInDirection(startLoc, dir).addVector(0, 1, 0);
//			//Material mat = DirectionUtils.getMaterialAtLocation(mc.theWorld, newLoc);
//			if (standableLoc((Mover)null, newLoc)) {
//				return newLoc;
//			} else {
		
		//Next 2 checks have changed newLoc to startLoc
				Vec3 newLocGround = groundUnderBlock(startLoc, maxGroundDistance);
				if (newLocGround != null) {
					return newLocGround;
				} else {
					Vec3 newLocRadial = spaceAroundBlock(startLoc, searchRadius);
					return newLocRadial;
				}
//			}
//		}
//		return null;
	}

	public static Vec3 spaceNearBlock(RichVec3 target, int maxGroundDistance,
			int searchRadius) {
		return spaceNearBlock(target.getVec(), maxGroundDistance, searchRadius);
	}
	
	
	/**
	 * Return an empty pathfindable space within a radius of a block.  Looks close, then farther away.
	 * @param mc
	 * @param startLoc
	 * @param maxRadius
	 * @return
	 */
	public static Vec3 spaceAroundBlock(Vec3 startLoc, int maxRadius) {
		if (standableLoc((Mover)null, startLoc)) {
			return startLoc;
		}
		for (int radius = 1; radius <= maxRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						int x = (int)Math.floor(startLoc.xCoord) + dx;
						int y = (int)Math.floor(startLoc.yCoord) + dy;
						int z = (int)Math.floor(startLoc.zCoord) + dz;
						Vec3 blockLoc = Vec3.createVectorHelper(x + 0.5, y, z + 0.5);
						double distance = startLoc.distanceTo(blockLoc);
						if (distance < maxRadius && standableLoc((Mover)null, blockLoc)) {
							return blockLoc;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * @param targetFace - the face to click on
	 * @param maxDistance - maximum distance from the face
	 * @param minDistance - minimum distance from the face
	 * @param allowJumpClick - whether to allow clicking while in midair (not
	 * recommended for breaking blocks)
	 * @return the set of locations standing at which the bot could click on
	 * targetFace (does not check for pathfindability)
	 */
	public static List<Vec3> spacesToClickFace(Vec3 target, double maxDistance, double minDistance, boolean allowJumpClick) {
		RichVec3 targetFace = new RichVec3(target);
		List<Vec3> list = new ArrayList<Vec3>();
//		for (int radius = 1; radius <= Math.ceil(maxDistance); radius++) {
		int radius = (int)Math.ceil(maxDistance);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int x = targetFace.getXFloored() + dx;
					int y = targetFace.getYFloored() + dy;
					int z = targetFace.getZFloored() + dz;
					RichVec3 testLoc = new RichVec3(x + 0.5, y + 0.62, z + 0.5);

					double distance = targetFace.getVec().distanceTo(testLoc.getVec());
					if (distance < maxDistance && distance > minDistance
							&& (standableLoc((Mover)null, testLoc.getVec())
									|| (allowJumpClick 
											&& jumpable(new RichVec3(testLoc.add(0,-1,0)), testLoc)))) {
						boolean canSee = canSee(testLoc, targetFace);
						if (canSee) {
							list.add(testLoc.getVec());
						}
					}
				}
			}
		}
		return list;
	}

	/**
	 * Returns a list of spaces standing at which the bot could click on
	 * (or shoot in a straight line at) any of the faces contained in targetFaces.
	 * @param targetFaces
	 * @param allowJumpClick - whether to allow clicking in midair
	 * @return
	 */
	public static List<Vec3> whereToStandToClick(List<Vec3> targetFaces, boolean allowJumpClick) {
		List<Vec3> foundTargets = new ArrayList<Vec3>();
		for (Vec3 targetFace : targetFaces) {
			List<Vec3> spaces = LocationUtils.spacesToClickFace(targetFace, 4, 0, allowJumpClick);
			for (Vec3 space : spaces) {
				if (!foundTargets.contains(space)) {
					foundTargets.add(space);
				}
			}
		}
		return foundTargets;
	}
	
	
	
	/**
	 * Wrapper method for World.getBlockMaterial().
	 * @param block
	 * @return the material of the block that the input vector is inside of
	 */
	public static Material getBlockMaterial(Vec3 block) {
		return mc().theWorld.getBlockMaterial((int)Math.floor(block.xCoord), (int)Math.floor(block.yCoord), (int)Math.floor(block.zCoord));
	}
	
	public static Material getBlockMaterial(int x, int y, int z) {
		return mc().theWorld.getBlockMaterial(x, y, z);
	}
	
	/**
	 * RichVec3 wrapper for other getBlockMaterial() wrapper.
	 * @param block
	 * @return
	 */
	public static Material getBlockMaterial(RichVec3 block) {
		return getBlockMaterial(block.getVec());
	}
	
	/**
	 * Returns a vector of the given entity's position.
	 * @param e
	 * @return
	 */
	public static Vec3 getEntityLocation(Entity e) {
		return Vec3.createVectorHelper(e.posX, e.posY, e.posZ);
	}
	
	public static Vec3 getPlayerLoc() {
		Entity player = mc().thePlayer;
		return getEntityLocation(player);
	}
	
	public static RichVec3 getPlayerLocAdawg() {
		return new RichVec3(getPlayerLoc());
	}
	
	public static List<Vec3> unitVectors() {
		List<Vec3> list = new ArrayList<Vec3>();
		list.add(Vec3.createVectorHelper(1, 0, 0));
		list.add(Vec3.createVectorHelper(0, 1, 0));
		list.add(Vec3.createVectorHelper(0, 0, 1));
		list.add(Vec3.createVectorHelper(-1, 0, 0));
		list.add(Vec3.createVectorHelper(0, -1, 0));
		list.add(Vec3.createVectorHelper(0, 0, -1));
		return list;
	}
	
	/**
	 * 
	 * @return a list of unit vectors in the order z, x, -z, -x
	 */
	public static List<RichVec3> unitHorizontals() {
		List<RichVec3> list = new ArrayList<RichVec3>();
		list.add(new RichVec3(0, 0, 1));
		list.add(new RichVec3(1, 0, 0));
		list.add(new RichVec3(0, 0, -1));
		list.add(new RichVec3(-1, 0, 0));
		return list;
	}
	
	/**
	 * Borrowed from playtechs.blogspot.com
	 * @return true if line-of-sight is clear from start to target
	 * false otherwise
	 */
	public static boolean canSee(RichVec3 start, RichVec3 target)
	{
	    double dx = Math.abs(target.getX() - start.getX());
	    double dy = Math.abs(target.getY() - start.getY());
	    double dz = Math.abs(target.getZ() - start.getZ());

	    int x = (int)(Math.floor(start.getX()));
	    int y = (int)(Math.floor(start.getY()));
	    int z = (int)(Math.floor(start.getZ()));

	    double dt_dx = 1.0 / dx;
	    double dt_dy = 1.0 / dy;
	    double dt_dz = 1.0 / dz;

	    double t = 0;

	    int n = 1;
	    int x_inc, y_inc, z_inc;
	    double t_next_vertical, t_next_horizontal, t_next_depth;

	    if (dx == 0)
	    {
	        x_inc = 0;
	        t_next_horizontal = dt_dx; // infinity
	    }
	    else if (target.getX() > start.getX())
	    {
	        x_inc = 1;
	        n += (int)(Math.floor(target.getX())) - x;
	        t_next_horizontal = (Math.floor(start.getX()) + 1 - start.getX()) * dt_dx;
	    }
	    else
	    {
	        x_inc = -1;
	        n += x - (int)(Math.floor(target.getX()));
	        t_next_horizontal = (start.getX() - Math.floor(start.getX())) * dt_dx;
	    }

	    if (dy == 0)
	    {
	        y_inc = 0;
	        t_next_vertical = dt_dy; // infinity
	    }
	    else if (target.getY() > start.getY())
	    {
	        y_inc = 1;
	        n += (int)(Math.floor(target.getY())) - y;
	        t_next_vertical = (Math.floor(start.getY()) + 1 - start.getY()) * dt_dy;
	    }
	    else
	    {
	        y_inc = -1;
	        n += y - (int)(Math.floor(target.getY()));
	        t_next_vertical = (start.getY() - Math.floor(start.getY())) * dt_dy;
	    }
	    
	    if (dz == 0)
	    {
	        z_inc = 0;
	        t_next_depth = dt_dz; // infinity
	    }
	    else if (target.getZ() > start.getZ())
	    {
	        z_inc = 1;
	        n += (int)(Math.floor(target.getZ())) - z;
	        t_next_depth = (Math.floor(start.getZ()) + 1 - start.getZ()) * dt_dz;
	    }
	    else
	    {
	        z_inc = -1;
	        n += z - (int)(Math.floor(target.getZ()));
	        t_next_depth = (start.getZ() - Math.floor(start.getZ())) * dt_dz;
	    }

	    for (; n > 0; --n)
	    {
	    	// Prevent false negatives when the target point is located on a boundary
	    	if (x == target.getXFloored() && y == target.getYFloored() && z == target.getZFloored()) {
		    	if ((target.getX() % 1 == 0 && x_inc == 1)
		    			|| (target.getY() % 1 == 0 && y_inc == 1) 
		    			|| (target.getZ() % 1 == 0 && z_inc == 1)) {
		    		continue;
		    	}
	    	}
	    	int block = Minecraft.getMinecraft().theWorld.getBlockId(x, y, z);
	    	if (block != 0 ) {
	    		return false;
	    	}
	        //visit(x, y, z);
	    	
	        if (t_next_vertical < t_next_horizontal && t_next_vertical < t_next_depth)
	        {
	            y += y_inc;
	            t = t_next_vertical;
	            t_next_vertical += dt_dy;
	        } else if (t_next_horizontal < t_next_vertical && t_next_horizontal < t_next_depth) {
	            x += x_inc;
	            t = t_next_horizontal;
	            t_next_horizontal += dt_dx;
	        }
	        else 
	        {
	            z += z_inc;
	            t = t_next_depth;
	            t_next_depth += dt_dz;
	        }
	    }
	    return true;
	}
}
