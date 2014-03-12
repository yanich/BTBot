package adawg.minecraftbot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Helper class, allows access to a single shared instance of MinecraftPathFinder.  
 * 
 * @author adawg
 * 
 */
public final class BotWrapper {
	private BotWrapper() {
		
	}
	
	private static MinecraftPathFinder pathfinder = null;
//	public static MinecraftPathFinder pathfinder() {
//		if (pathfinder == null) {
//			pathfinder = new MinecraftPathFinder(400, 100, 400);
//		}
//		return pathfinder;
//	}
}
