package adawg.minecraftbot.pathfinding;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Vec3;


/**
 * A path determined by some path finding algorithm. A series of steps from
 * the starting location to the target location. This includes a step for the
 * initial location.
 * 
 */
public class Path3D {
	/** The list of steps building up this path */
	private ArrayList<Step> steps = new ArrayList<Step>();
	
	/** The index of the current step being followed on this path */
	private int pathIndex;
	
	/**
	 * Create an empty path
	 */
	public Path3D() {
		this.pathIndex = 0;
	}

	/**
	 * Get the length of the path, i.e. the number of steps
	 * 
	 * @return The number of steps in this path
	 */
	public int getLength() {
		return steps.size();
	}
	
	/**
	 * Get the step at a given index in the path
	 * 
	 * @param index The index of the step to retrieve. Note this should
	 * be >= 0 and < getLength();
	 * @return The step information, the position on the map.
	 */
	public Step getStep(int index) {
		return (Step) steps.get(index);
	}
	
	public List<Step> getSteps() {
		return (List<Step>) steps.clone();
	}
	
	public void incrementPathIndex() {
		pathIndex++;
	}
	
	public int getPathIndex() {
		return pathIndex;
	}
	
	public void setPathIndex(int i) {
		pathIndex = i;
	}
	
	public String toString() {
		List<String> strings = new ArrayList<String>();
		for (Step step : steps) {
			strings.add(step.toString());
		}
		return strings.toString();
	}
	
	public boolean isFinished() {
		return pathIndex >= getLength();
	}
	
	/**
	 * Get the x coordinate for the step at the given index
	 * 
	 * @param index The index ofn the step whose x coordinate should be retrieved
	 * @return The x coordinate at the step
	 */
	public int getX(int index) {
		return getStep(index).x;
	}
	
	/**
	 * Get the y coordinate for the step at the given index
	 * 
	 * @param index The index of the step whose y coordinate should be retrieved
	 * @return The y coordinate at the step
	 */
	public int getY(int index) {
		return getStep(index).y;
	}
	
	public int getZ(int index) {
		return getStep(index).z;
	}
	
	/**
	 * @return the x coordinate of the current path step
	 */
	public int getX() {
		return getX(pathIndex);
	}

	/**
	 * @return the y coordinate of the current path step
	 */
	public int getY() {
		return getY(pathIndex);
	}

	/**
	 * @return the z coordinate of the current path step
	 */
	public int getZ() {
		return getZ(pathIndex);
	}
	
	/**
	 * Append a step to the path.  
	 * 
	 * @param x The x coordinate of the new step
	 * @param y The y coordinate of the new step
	 */
	public void appendStep(int x, int y, int z) {
		steps.add(new Step(x,y,z));
	}

	/**
	 * Prepend a step to the path.  
	 * 
	 * @param x The x coordinate of the new step
	 * @param y The y coordinate of the new step
	 */
	public void prependStep(int x, int y, int z) {
		steps.add(0, new Step(x, y, z));
	}
	
	/**
	 * Check if this path contains the given step
	 * 
	 * @param x The x coordinate of the step to check for
	 * @param y The y coordinate of the step to check for
	 * @return True if the path contains the given step
	 */
	public boolean contains(int x, int y, int z) {
		return steps.contains(new Step(x,y,z));
	}
	
	/**
	 * A single step within the path
	 * 
	 * @author Kevin Glass
	 */
	public class Step {
		/** The x coordinate at the given step */
		private final int x;
		/** The y coordinate at the given step */
		private final int y;
		private final int z;
		
		/**
		 * Create a new step
		 * 
		 * @param x The x coordinate of the new step
		 * @param y The y coordinate of the new step
		 */
		public Step(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		/**
		 * Get the x coordinate of the new step
		 * 
		 * @return The x coodindate of the new step
		 */
		public int getX() {
			return x;
		}

		/**
		 * Get the y coordinate of the new step
		 * 
		 * @return The y coodindate of the new step
		 */
		public int getY() {
			return y;
		}
		
		public int getZ() {
			return z;
		}
		
		/**
		 * @see Object#hashCode()
		 */
		public int hashCode() {
			return x*y*z;
		}

		/**
		 * @see Object#equals(Object)
		 */
		public boolean equals(Object other) {
			if (other instanceof Step) {
				Step o = (Step) other;
				
				return (o.x == x) && (o.y == y) && (o.z == z);
			}
			
			return false;
		}
		
		public String toString() {
			return "(" + getX() + ", " + getY() + ", " + getZ() + ")";
		}
		
		public Vec3 toVec3() {
			return Vec3.createVectorHelper(x + 0.5, y + 0.62, z + 0.5);
		}
	}
}
