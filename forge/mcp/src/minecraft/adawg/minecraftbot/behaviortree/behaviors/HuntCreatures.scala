package adawg.minecraftbot.behaviortree.behaviors

import net.minecraft.entity.EnumCreatureType
import adawg.minecraftbot.behaviortree._
import BehaviorStatus._
import adawg.minecraftbot.BotHelper
import adawg.minecraftbot.behaviortree.Actions
import net.minecraft.util.Vec3
import com.kunii.mcbot.McBot
import net.minecraft.entity.Entity
import adawg.minecraftbot.RichEntity
import adawg.minecraftbot.RichVec3
import adawg.minecraftbot.BotImplicits._

object HuntCreatures {

  def apply(repelDistance: => Double = 3, 
      repelStrength: => Double = 1.01, 
      reach: => Double = 4.4, 
      
       //TODO vary this mob-by-mob (spiders need more than zombies)
      howCloseToGo: => Double = 4,
      
      targets: Seq[EnumCreatureType],
      attackFrequency: => Double = 10,
      maxDistance: => Double = 20) = {
    
    def getTargetOfType(creatureType: EnumCreatureType) = BotHelper.getNearestEntity(e =>
      e.isCreatureType(creatureType, false)
        && e.hurtResistantTime < 20
        && e.getDistanceToEntity(BotHelper.player) < maxDistance)
        
    def huntCreatureType(creatureType: EnumCreatureType) = ParallelNode(false, false)(
      attackEntity(getTargetOfType(creatureType), reach, attackFrequency),
//          new FollowPath(getTargetOfType(creatureType) map { _.getLocation } toSeq))
      Actions.walkWithinDistanceOf(howCloseToGo)(groundUnderTarget(creatureType)))

    def groundUnderTarget(creatureType: EnumCreatureType) = for {
      t <- getTargetOfType(creatureType)
      blockUnder <- BotHelper.firstBlockUnder(t.getLocation)
      groundUnder = blockUnder.yCoord.ceil
    } yield Vec3.createVectorHelper(blockUnder.xCoord, groundUnder + 1.62, blockUnder.zCoord)   

    val hunts = targets map { huntCreatureType(_) }
    
    ParallelNode(true, false)(
      ActionNode2 { (Running, Actions.steerUsingForce(repelFromMobs(repelDistance, repelStrength))) },
      PriorityNode(hunts:_*))
  }

  // Attacks (left-clicks) a target entity.  
  def attackEntity(target: => Option[Entity], reach: => Double = 4.4, attacksPerSecond: => Double = 10): Node = {

    import com.kunii.mcbot.Camera
    
    var lastClickedTime = System.nanoTime()
    def timeSinceLastClick = System.nanoTime() - lastClickedTime
    def attackPeriod = 1e9 / attacksPerSecond
    
    ActionNode2 {
      target match {
        case None => (Failed, new InputState)
        case Some(entity) => {
          if (McBot.getPlayer().getDistanceToEntity(entity) < reach) {
            Camera.turnCam(entity, 30)
            if (timeSinceLastClick > attackPeriod) {
              McBot.leftClick()
              lastClickedTime = System.nanoTime()
              (Success, InputState(mouse1 = Some(true)))
            } else (Running, new InputState)
          } else (Failed, new InputState)
        }
      }
    }
  }
  /**
   * Returns the repulsive force from nearby mobs using coulomb's law
   */
  def repelFromMobs(maxDistance: Double, strength: Double = 1): Vec3 = {
    val nearbyMobs = BotHelper.entitiesSatisfying(e =>
      e.isCreatureType(EnumCreatureType.monster, false)
        &&
        e.getDistanceToEntity(McBot.getPlayer()) < maxDistance)

    // Sum up the forces of all nearby mobs
    val repulsiveForce = nearbyMobs.foldLeft(Vec3.createVectorHelper(0, 0, 0))(
      (accum: Vec3, e: Entity) => {
        //subtract mob's location from our location
        // mob subtracted from us gives a vector from mob to us.
        val direction = BotHelper.entityLoc(e).subtract(BotHelper.playerLoc).normalize()
        // strength/r^2
        val magnitude = strength / BotHelper.player.getDistanceSqToEntity(e)
        accum.add(direction.multiply(magnitude))
      })
    repulsiveForce
  }
}