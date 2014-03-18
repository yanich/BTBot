package adawg.minecraftbot
import adawg.minecraftbot.BotWrapper._
import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import adawg.minecraftbot.behaviortree.BehaviorStatus._
import adawg.minecraftbot.behaviortree._
import BehaviorStatus._
import com.kunii.mcbot._
import net.minecraft.entity.Entity
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.EntityPlayer
import scala.collection.JavaConversions._
import net.minecraft.entity.EnumCreatureType
import net.minecraft.util.Vec3
import adawg.minecraftbot.BotImplicits._
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import adawg.minecraftbot.pathfinding.LocationUtils
import adawg.minecraftbot.pathfinding.Path3D
import adawg.minecraftbot.behaviortree.behaviors.FollowPath
import adawg.minecraftbot.behaviortree.decorators.Decorators._
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import java.util.ArrayList
import net.minecraft.client.settings.GameSettings

/**
 * A mish-mash of functions for dealing with blocks and entities in the world.
 * Includes short aliases like world, player, playerLoc.
 * TODO reduce this to JUST short aliases?
 * 
 * At the bottom is some steering algorithms and behavior tree actions.
 * TODO turn em into actions
 */
object BotHelper {
  
  def directions = Seq(
      Vec3.createVectorHelper(1, 0, 0),
      Vec3.createVectorHelper(0, 0, 1),
      Vec3.createVectorHelper(-1, 0, 0),
      Vec3.createVectorHelper(0, 0, -1))

  def world = McBot.getWorld()
  def getBlockId(loc: Vec3) = world.getBlockId(loc.getXFloored, loc.getYFloored, loc.getZFloored)
  def getBlockMaterial(loc: Vec3) = world.getBlockMaterial(loc.getXFloored, loc.getYFloored, loc.getZFloored)

  /**
   * Goes down through the blocks directly underneath the given block.
   * Returns coordinates inside of the first non-air block found.
   */
  def firstBlockUnder(block: Vec3): Option[Vec3] = {
    var blockUnder = block.add(0, -1, 0)
    while (getBlockId(blockUnder) == 0) {
      if (blockUnder.yCoord <= 1) return None
      blockUnder = blockUnder.add(0, -1, 0)
    }
    return Some(blockUnder)
  }

  /**
   * Return true if the block at loc is likely to cause harm.
   */
  def isHarmful(loc: Vec3): Boolean = {
    val mat = getBlockMaterial(loc)
    mat == Material.lava
  }

  def isSolid(loc: Vec3): Boolean = getBlockMaterial(loc).isSolid

  /**
   * Return true if the block at loc and its neighbors are not harmful.
   */
  def isSafeToMine(loc: Vec3) = {
    // todo avoid air pockets (i.e. caves) ???
    !isHarmful(loc) &&
      !loc.neighbors.exists(isHarmful)
  }

  /**
   *  Use raytracing to find a list of faces that can be clicked to
   *  break the target block.
   * @param target A point within the target block
   * @return a list of points, one on each exposed face of the target block
   */
  def findExposedFaces(target: Vec3): Seq[Vec3] = {
    val list = new ArrayList[Vec3]();
    val targetCenter = target.floored().add(0.5, 0.5, 0.5);
    for (unit <- LocationUtils.unitVectors()) {
      val neighborBlock = targetCenter.add(unit);
      if (!getBlockMaterial(neighborBlock).isSolid()) {
        val pos = world.rayTraceBlocks(neighborBlock, targetCenter);
        if (pos != null) {
          if (pos.hitVec != null) {
            list.add(pos.hitVec);
          }
        }
      }
    }
    return list;
  }
  
  def blocksSatisfying(radius: Int, center: (Int, Int, Int)) (cond: (Int, Int, Int) => Boolean) = {
    val (cenx, ceny, cenz) = center
    for {
      x <- (cenx - radius) to (cenx + radius)
      y <- (ceny - radius) to (ceny + radius)
      z <- (cenz - radius) to (cenz + radius)
      if cond(x, y, z)
    } yield (x, y, z)
  }

  def blocksWithinCube(center: Vec3, radius: Int): Seq[Vec3] = for {
    x <- (center.getXFloored() - radius) to (center.getXFloored() + radius)
    y <- (center.getYFloored() - radius) to (center.getYFloored() + radius)
    z <- (center.getZFloored() - radius) to (center.getZFloored() + radius)
  } yield Vec3.createVectorHelper(x, y, z)

  def playerVelocity: Vec3 = {
    val p = McBot.getPlayer()
    Vec3.createVectorHelper(p.motionX, p.motionY, p.motionZ)
  }

  // Return coordinates of the player's head and view (1.62 above feet normally)
  def playerLoc: Vec3 = entityLoc(McBot.getPlayer())
  
  def entityLoc(p: Entity): Vec3 = {
    Vec3.createVectorHelper(p.posX, p.posY, p.posZ)
  }
  
  // Return coordinates within the block the player's legs & feet occupy
  def playerLegs = playerLoc add (0, -1, 0)
  // Return coordinates within the block under the player's feet
  def playerFloor = playerLoc add (0, -2, 0)

  def player = McBot.getPlayer()

  /**
   * Return a unit vector in the direction the player is facing.
   */
  def forwardsVector: Vec3 = {
    val vec = Vec3.createVectorHelper(0, 0, 1)
    vec.rotateAroundY(-Math.toRadians(McBot.getPlayer().rotationYaw).toFloat)
    vec
  }

  /**
   * Returns a unit vector pointing to the right of the player.
   */
  def rightVector: Vec3 = {
    val vec = forwardsVector
    vec.rotateAroundY(-(Math.PI / 2).toFloat)
    vec
  }

  def getNearestPlayer: Option[Entity] = {
    getNearestEntity(_.isInstanceOf[EntityPlayer])
  }
  
  /**
   * Returns a sequence of Vec3s that satisfy predicate.
   * Looks at regularly-spaced locations (1 meter apart) 
   * within a cube of 2 * radius on each side, centered on location.
   * 
   * @param location the center of the search space
   * @param radius the length of each edge of the search space
   * @param predicate 
   */
  def nearbyLocations(location: Vec3, radius: Int)(predicate: Vec3 => Boolean): Seq[Vec3] = {
    val seq = for {
      dx <- (-radius to radius)
      dy <- (-radius to radius)
      dz <- (-radius to radius)
    } yield {
      val loc = location.addVector(dx, dy, dz)
      if (predicate(loc)) Some(loc) else None
    }
    seq.flatten
  }

  def nearbyPickups = {
    entitiesSatisfying(e => e.isInstanceOf[EntityItem]
      || e.isInstanceOf[EntityXPOrb])
  }
  
  def itemsSatisfying(predicate: EntityItem => Boolean): Seq[EntityItem] = {
    entitiesSatisfying(_.isInstanceOf[EntityItem]) map { _.asInstanceOf[EntityItem] }
  }

  // Convert to array for thread safety
  def worldEntities = McBot.getWorld().loadedEntityList.asInstanceOf[java.util.List[Entity]].toArray[Entity](Array())

  /**
   * Returns all entities satisfying predicate, excluding the player, sorted from closest to
   * farthest away.
   */
  def entitiesSatisfying(predicate: Entity => Boolean): Seq[Entity] = {
    val player = McBot.getPlayer().asInstanceOf[Entity]
    val validEntities = worldEntities.filterNot(_ == player).filter(predicate)
    val sortedEntities = validEntities.sortWith(player.getDistanceToEntity(_) < player.getDistanceToEntity(_))
    sortedEntities
  }

  /**
   * Returns the nearest entity to the player, excluding the player, that satisfies predicate.
   */
  def getNearestEntity(predicate: Entity => Boolean): Option[Entity] = {
    entitiesSatisfying(predicate).headOption
  }

  def getPlayerEntityByName(player: String): Option[EntityPlayer] = {
    Option(world.getPlayerEntityByName(player));
  }

  def getNearbyPlayer(distance: Double): Option[EntityPlayer] = {
    val nearbyPlayers = playersWithinRadius(distance)
    if (nearbyPlayers.isEmpty) None else Some(nearbyPlayers(0))
    //	  Option(world().getClosestPlayerToEntity(player(), distance))
    //	  	.filterNot(_.getEntityName().equals(player().getEntityName))
  }

  def playersWithinRadius(rad: Double): Seq[EntityPlayer] = {
    val (x, y, z) = (player.posX, player.posY, player.posZ)
    val box = AxisAlignedBB.getBoundingBox(x - rad, y - rad, z - rad, x + rad, y + rad, z + rad)
    val entities = world.getEntitiesWithinAABBExcludingEntity(player, box)
    val players = entities.filter(_ match {
      case x: EntityPlayer => true
      case _ => false
    })
    players.toSeq.asInstanceOf[Seq[EntityPlayer]]
  }

  /**
   * Returns the velocity vector the player should have to gradually slow down and arrive at goal.
   */
  def arrivalVelocity(goal: Vec3): Vec3 = {
    val maxSpeed = 0.1155
    val slowingDistance = 0.5
    // goal minus us gives a vector pointing from us to goal
    val offset = playerLoc.subtract(goal)
    val distance = offset.lengthVector()

    //Outside the stopping radius, max speed.  Within it, slow down linearly with distance.
    val rampedSpeed = maxSpeed * distance / slowingDistance
    val clippedSpeed = rampedSpeed min maxSpeed

    // Divide offset by distance to normalize it.  Multiply by clippedSpeed to get the answer
    val desiredVelocity = offset.multiply(clippedSpeed / distance)

    desiredVelocity
  }

  def seekVelocity(goal: Vec3): Vec3 = {
    val offset = playerLoc.subtract(goal)
    val desiredVelocity = offset.normalize().multiply(0.1155)
    desiredVelocity
  }
  
  /**
   * Functional version of steerUsingForce.  Returns InputState to steer towards target ground velocity.
   */
  def steerUsingForce2(targetVelocity: Vec3): InputState = {
    // Break down current velocity and target velocity into forwards and rightwards components
    val curFwdVel = playerVelocity.dotProduct(forwardsVector)
    val curSideVel = playerVelocity.dotProduct(rightVector)
    val tarFwdVel = targetVelocity.dotProduct(forwardsVector)
    val tarSideVel = targetVelocity.dotProduct(rightVector)
    val fwdSteering = tarFwdVel - curFwdVel
    val sideSteering = tarSideVel - curSideVel

    val threshold = 0.02
    val forward, back, right, left = false
    if (targetVelocity.lengthVector() > 0) {
      InputState(
        forward = Some(fwdSteering > threshold),
        back = Some(fwdSteering < -threshold),
        right = Some(sideSteering > threshold),
        left = Some(sideSteering < -threshold))
    } else new InputState
  }
  
  def walkWithoutLooking(goal: Vec3): InputState = {
    steerUsingForce2(seekVelocity(goal)).copy(jump = Some(player.posY < goal.yCoord))
  }
  
  
  def walkWhileLooking(goal: Vec3, maxSpeed: Double, moveThreshold: Double, jumpThreshold: Double = 0.1): InputState = {
    val desiredVelocity = goal.subtract(playerLoc).normalize().multiply(maxSpeed)
    val steering = playerVelocity.subtract(desiredVelocity)
    val horizontalComponent = Math.sqrt(steering.xCoord * steering.xCoord + steering.zCoord * steering.zCoord)
    
    // Dot product between steering and the vector pointing from player to goal.
    // If this is negative, it's telling us to steer backwards
    val dotProduct = steering.dotProduct(goal.subtract(playerLoc))
    
    
    val walkInput = if (horizontalComponent < moveThreshold) new InputState
    else if (dotProduct > 0) InputState(forward = Some(true))
    else InputState(back = Some(true))
    
    val lookInput = lookAtYaw(goal)
    
    lookInput.mergeOver(walkInput).copy(jump = Some(player.posY + jumpThreshold < goal.yCoord))
  }

  def lookAt(goal: Vec3): InputState = {
    val xDist = goal.xCoord - player.posX;
    val yDist = player.posY + player.getEyeHeight() - goal.yCoord;
    val zDist = goal.zCoord - player.posZ;

    val yaw = (Math.atan2(zDist, xDist) * 180.0D / Math.PI) - 90F;
    val pitch = (Math.atan2(yDist, Math.sqrt(xDist * xDist + zDist * zDist)) * 180.0D / Math.PI);
    InputState(
      mouseYaw = Some(yaw),
      mousePitch = Some(pitch))
  }
  def lookAtYaw(goal: Vec3): InputState = lookAt(goal).copy(mousePitch = None)

}