package adawg.minecraftbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scala.Int;
import scala.Tuple3;

import net.minecraft.util.Vec3;

/**
 * This class is a replacement for Minecraft's Vec3D class.
 * Basically, Vec3 objects are all shared in a pool and if you hold onto a 
 * reference to one while certain Minecraft methods are called, its coordinates
 * will be overwritten, causing bugs if you expect your Vec3 not to be changed.
 * If you want to hold onto a Vec3 for later use, use this class instead.
 * It implements equals in order to support List<RichVec3>.contains().
 * 
 * NOTE: The above rationale for this class is no longer true because Forge gets
 * rid of the reuse behavior.  Now it is used to "pimp" the Vec3 class with useful
 * methods.
 * @author adawg
 *
 */

public class RichVec3 {
	public RichVec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public RichVec3(Vec3 vec3d) {
		this.x = vec3d.xCoord;
		this.y = vec3d.yCoord;
		this.z = vec3d.zCoord;
	}
	
	public boolean equals(Object aThat) {
		if (this == aThat) return true;
		
		Vec3 that;
		if (!(aThat instanceof RichVec3)) {
			if (!(aThat instanceof Vec3)) {
				return false;
			} else {
				that = (Vec3)aThat;
			}
		} else {
			that = ((RichVec3)aThat).getVec();
		}
		
		
		return 
			x == that.xCoord &&
			y == that.yCoord &&
			z == that.zCoord;
	}
	
	private double x;
	private double y;
	private double z;
	
	public String toString() {
		return String.format("(%.2f, %.2f, %.2f)", x, y, z);
	}
	
	// Allow pretty printing to be implicitly available
	public String toStringFancy() {
		return toString();
	}
	
	/**
	 * Returns the coordinates of this vector, rounded down, as a list of integers.
	 * @return
	 */
	public List<Double> flooredCoords() {
		return Arrays.asList(Math.floor(x), Math.floor(y), Math.floor(z));
	}
	
	public Vec3 projectedOnto(Vec3 otherVector) {
		return new RichVec3(otherVector).multiply(
				this.getVec().dotProduct(otherVector) / 
				otherVector.dotProduct(otherVector));
	}
	
	/**
	 * Floors the coordinates of this vector
	 * @return a new vector with floored coordinates
	 */
	public RichVec3 floored() {
		return new RichVec3(Math.floor(x), Math.floor(y), Math.floor(z));
	}
	
	public static int floor(double x) {
		return (int)Math.floor(x);
	}
	
	public Tuple3<Integer, Integer, Integer> toIntTuple() {
		return new Tuple3<Integer, Integer, Integer>(getXFloored(), getYFloored(), getZFloored());
	}
	
	/**
	 * Returns a new Vec3 with given values added
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vec3 add(double x, double y, double z) {
		return Vec3.createVectorHelper(this.x + x, this.y + y, this.z + z); 
	}
	
	/**
	 * Returns a new RichVec3 with all values multiplied by a scalar
	 * @param s
	 * @return
	 */
	public Vec3 multiply(double s) {
		return Vec3.createVectorHelper(x * s, y * s, z * s);
	}

	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	
	/**
	 * Returns the X value of this vector, rounded down to an integer value.
	 * @return
	 */
	public int getXFloored() { 
		return floor(x);
	}

	/**
	 * Returns the Y value of this vector, rounded down to an integer value.
	 * @return
	 */
	public int getYFloored() {
		return floor(y);
	}

	/**
	 * Returns the Z value of this vector, rounded down to an integer value.
	 * @return
	 */
	public int getZFloored() {
		return floor(z);
	}
	
	/**
	 * Returns a Vec3 with the same coordinates as this vector
	 * @return
	 */
	public Vec3 getVec() {
		return Vec3.createVectorHelper(x, y, z);
	}

	public Vec3 add(RichVec3 offset) {
		return add(offset.x, offset.y, offset.z);
	}

	/**
	 * Returns this vector with x and z coordinates set to the center of the square they are in
	 * and y coordinate with 0.62 added (head height)
	 * @return
	 */
	public RichVec3 centered() {
		return new RichVec3(Math.floor(x) + 0.5, Math.floor(y) + 0.62, Math.floor(z) + 0.5);
	}

	public double distanceTo(RichVec3 loc) {
		return getVec().distanceTo(loc.getVec());
	}
	
	public double horizontalDistanceTo(Vec3 loc) {
		double dz = z - loc.zCoord;
		double dx = x - loc.xCoord;
		double distance = Math.sqrt(dx * dx + dz * dz);
		return distance;
	}
	
	/**
	 * Returns a list of vec3s all shifted +- 1 from this vec3 in 1-3 directions
	 * i.e. coordinates within each of the neighbors of the cube this vec3 is located in.
	 */
    public List<Vec3> neighbors() {
    	List<Vec3> list = new ArrayList<Vec3>();
    	for (int i = -1; i < 2; i++) {
    		for (int j = -1; j < 2; j++ ) {
    			for (int k = -1; k < 2; k++) {
    				if (i != 0 || j != 0 || k != 0)
    				list.add(this.add(i, j, k));
    			}
    		}
    	}
    	return list;
    }
}
