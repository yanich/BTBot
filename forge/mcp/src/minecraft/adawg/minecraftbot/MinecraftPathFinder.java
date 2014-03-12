package adawg.minecraftbot;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import net.minecraft.util.Vec3;

import org.newdawn.slick.util.pathfinding.Mover; // empty tagging interface


import adawg.minecraftbot.pathfinding.MinecraftMap;
import adawg.minecraftbot.pathfinding.AStarPathFinder3D;
import adawg.minecraftbot.pathfinding.Path3D;

public class MinecraftPathFinder implements Mover {
    
	private AStarPathFinder3D pathFinder;
	private Minecraft mc;
	
	//These store the start/end positions of a path
	private RichVec3 start;
	private RichVec3 target;
	private RichVec3 lastStart;
	private RichVec3 lastTarget;
	
	//This stores a calculated path
	private Path3D path;
    
    public MinecraftPathFinder(int dimX, int dimY, int dimZ) {
    	this.mc = Minecraft.getMinecraft();
    	pathFinder = new AStarPathFinder3D(new MinecraftMap(), true, dimX, dimY, dimZ);
    }
    
    public Path3D getPath() {
    	return path;
    }
    
    public Path3D findPath(RichVec3 start, RichVec3 target, int maxSearchDistance, AtomicBoolean shouldStop) {
    	Path3D path = pathFinder.findPath(this, start, target, maxSearchDistance, shouldStop);

    	if (path == null) {
    		System.out.println("Couldn't calculate path from " + start.toStringFancy() + " to " + target.toStringFancy());
    	} else {
    		System.out.println("Path calculated.");
    	}
    	return path;
    }
    
    public void calculatePath(int maxSearchDistance) {
    	/*
    	if (sx < 0 || sy < 0 || sz < 0 || tx < 0 || ty < 0 || tz < 0) {
    		System.out.println("Error: Negative coordinate(s) passed to calculatePath()");
    		return;
    	}
    	*/
    	if (start == null || target == null) {
    		System.out.println("Either start or target is missing for calculatePath");
    		return;
    	}
    	
    	path = pathFinder.findPath(this, start, target, maxSearchDistance, new AtomicBoolean(false));
    }
    
    public void deletePath() {
    	path = null;
    }
    
    public void resetNodeStorage(int dimX, int dimY, int dimZ, int xCenter, int yOffset, int zCenter) {
    	pathFinder.resetNodeStorage(dimX, dimY, dimZ, xCenter, yOffset, zCenter);
    }
    
    public void printStep(int i, Path3D path) {
    	if (i < path.getLength()) {
    		System.out.println("Path step " + i + ": " + path.getX(i) + ", " + path.getY(i) + ", " + path.getZ(i));
    	}
    }
    
    //Print the path, just to test.
    public void printPath(Path3D path) {
    	if (path == null) {
    		System.out.println("No path to print");
    		return;
    	}
    	
    	for (int i = 0; i < path.getLength(); i++) {
    		printStep(i, path);
    	}
    }
    
    /**
     * Print the path stored in this MinecraftPathFinder object.  Obsolete.
     */
    public void printPath() {
    	printPath(this.path);
    }
}
