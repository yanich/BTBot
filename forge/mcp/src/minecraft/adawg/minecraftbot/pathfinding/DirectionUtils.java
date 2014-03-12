package adawg.minecraftbot.pathfinding;

import java.util.HashMap;
import java.util.Map;

import adawg.minecraftbot.BotWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraft.src.Material;

public class DirectionUtils {
	public enum CompassDirection {
		//NO_DIRECTION(-1),
//		SOUTHWEST(1),
//		NORTHWEST(3),
//		NORTHEAST(5),
//		SOUTHEAST(7),
		SOUTH(0),
		WEST(1),
		NORTH(2),
		EAST(3);
		private final int id;
		private static Map<Integer, CompassDirection> map;

		private CompassDirection(int id) {
			this.id = id;
			add(id, this);
		}

		private static void add(int type, CompassDirection name) {
			if (map == null) {
				map = new HashMap<Integer, CompassDirection>();
			}
			map.put(type, name);
		}

		public int getType() {
			return id;
		}
		
		// The angles' IDs are assigned in order from 0 degrees to 315.
		public double getCenterAngle() {
			return directionWidth * id;
		}

		public static CompassDirection fromId(final int type) {
			return map.get(type);
		}
		 
		public Material getMaterialInDirection(int x, int y, int z, int distance) {
			return DirectionUtils.getMaterialInDirection(x, y, z, distance, this);
		}
	}
	
	private static double directionWidth = 90;
	
	private static double normalize(double degrees) {
		while (degrees > 360) {
			degrees -= 360;
		}
		while (degrees < 0) {
			degrees += 360;
		}
		return degrees;
	}

	private static boolean isFacing(CompassDirection dir, double degrees) {
		double degreesNormalized = normalize(degrees);
		double halfWidth = directionWidth / 2;
		double center = dir.getCenterAngle();
		double lowerBound = center - halfWidth;
		double upperBound = center + halfWidth;
		if (dir.getType() == 0) {
			return 360 - halfWidth < degreesNormalized || halfWidth > degreesNormalized;
		}
		return (lowerBound < degreesNormalized) && (upperBound > degreesNormalized);
	}
	
	public static CompassDirection getDirectionFromRotation(double degrees) {
		degrees = normalize(degrees);
		for (CompassDirection dir : CompassDirection.values()) {
			if (isFacing(dir, degrees)) {
				return dir;
			}
		}
		return null;
	}
	
	public static Material getMaterialInDirection(int x, int y, int z, int distance, CompassDirection dir) {
		switch (dir) {
//		case SOUTHWEST: z += 1; x -= 1; break;
//		case NORTHWEST: x -= 1; z -= 1; break;
//		case NORTHEAST: z -= 1; x += 1; break;
//		case SOUTHEAST: x += 1; z += 1; break;
		case SOUTH: z += distance; break;
		case WEST: x -= distance; break;
		case NORTH: z -= distance; break;
		case EAST: x += distance; break;
		default: return null;
		}
		return getBlockMaterial(x, y, z);
		/*
		if (facing == CompassDirection.WEST) {
			return w.getBlockMaterial(x - 1, y, z);
		}
		if (facing == CompassDirection.NORTH) {
			return w.getBlockMaterial(x, y, z - 1);
		}
		if (facing == CompassDirection.EAST) {
			return w.getBlockMaterial(x + 1, y, z);
		}
		if (facing == CompassDirection.SOUTH) {
			return w.getBlockMaterial(x, y, z + 1);
		}
		*/
	}
	
	
	public static Material getMaterialInDirection(int x, int y, int z, double degrees) {
		return getMaterialInDirection(x, y, z, getDirectionFromRotation(degrees));
	}
	
	public static Material getMaterialInDirection(double x, double y, double z, CompassDirection dir) {
		int ix = (int)Math.floor(x);
		int iy = (int)Math.floor(y);
		int iz = (int)Math.floor(z);
		return getMaterialInDirection(ix, iy, iz, dir);
	}
	
	public static Material getMaterialInDirection(double x, double y, double z, double degrees) {
		return getMaterialInDirection(x, y, z, getDirectionFromRotation(degrees));
	}
	
	public static Material getMaterialInDirection(Vec3 startLoc, CompassDirection dir) {
		return getMaterialInDirection(startLoc.xCoord, startLoc.yCoord, startLoc.zCoord, dir);
	}
	
	public static Material getMaterialTowardsBlock(int sx, int sy, int sz, int tx, int ty, int tz) {
		return getMaterialInDirection(sx, sy, sz, getDirectionFromRotation(yawToTarget(sx, sz, tx, tz)));
	}
	
	public static Vec3 getLocationInDirection(Vec3 startLoc, CompassDirection dir) {
		int x = 0;
		int y = 0;
		int z = 0;
		switch (dir) {
//		case SOUTHWEST: z += 1; x -= 1; break;
//		case NORTHWEST: x -= 1; z -= 1; break;
//		case NORTHEAST: z -= 1; x += 1; break;
//		case SOUTHEAST: x += 1; z += 1; break;
		case SOUTH: z += 1; break;
		case WEST: x -= 1; break;
		case NORTH: z -= 1; break;
		case EAST: x += 1; break;
		default: return null;
		}
		return startLoc.addVector(x, y, z);
	}
	
	public static Material getBlockMaterial(int x, int y, int z) {
		return BotWrapper.world().getBlockMaterial(x, y, z);
	}
	public static Material getBlockMaterial(Vec3 loc) {
		int x = (int)Math.floor(loc.xCoord);
		int y = (int)Math.floor(loc.yCoord);
		int z = (int)Math.floor(loc.zCoord);
		return getBlockMaterial(x, y, z);
	}
	
	// Returns a minecraft yaw in degrees
	public static double yawToTarget(double sx, double sz, double tx, double tz) {
		double dx = tx - sx;
		double dz = tz - sz;
		return -1 * Math.toDegrees(Math.atan2(dx, dz));
	}
	
	public static double yawToTarget(Vec3 v1, Vec3 v2) {
		double dx = v2.xCoord - v1.xCoord;
		double dz = v2.zCoord - v1.zCoord;
		return -1 * Math.toDegrees(Math.atan2(dx, dz));
	}
	
	public static double pitchToTarget(double sx, double sy, double sz, double tx, double ty, double tz) {
		double dx = tx - sx;
		double dy = ty - sy;
		double dz = tz - sz;
		double dHorizontal = Math.sqrt(dx * dx + dz * dz);
		return -Math.toDegrees(Math.atan(dy / dHorizontal));
	}
	
	public static double pitchToTarget(Vec3 v1, Vec3 v2) {
		double dx = v2.xCoord - v1.xCoord;
		double dy = v2.yCoord - v1.yCoord;
		double dz = v2.zCoord - v1.zCoord;
		double dHorizontal = Math.sqrt(dx * dx + dz * dz);
		return -Math.toDegrees(Math.atan(dy / dHorizontal));
	}
}
