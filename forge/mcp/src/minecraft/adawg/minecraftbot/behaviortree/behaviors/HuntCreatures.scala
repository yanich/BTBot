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
import adawg.minecraftbot.behaviortree.gui.DoubleInput
import adawg.minecraftbot.behaviortree.gui.Input
import adawg.minecraftbot.behaviortree.gui.HiddenInput
import EnumCreatureType._

/**
 * Hunts down creatures within a certain distance of the player.
 * This is a kind of ugly (big) behavior.  TODO break it down
 * @param targets the EnumCreatureTypes to be hunted, e.g. monster, creature
 */
object HuntCreatures {

  def apply(repelDistance: Input[Double] = new DoubleInput("Repel distance", 3), 
      repelStrength: Input[Double] = new DoubleInput("Repel strength", 1.01), 
      reach: Input[Double] = new DoubleInput("Attack reach", 4.4), 
      
       //TODO vary this mob-by-mob (spiders need more than zombies)
      howCloseToGo: Input[Double] = new DoubleInput("How close to go", 4),
      
      targets: Seq[EnumCreatureType] = Seq(monster, creature),
      attackFrequency: Input[Double] = new DoubleInput("Attacks per second", 50),
      maxDistance: Input[Double] = new DoubleInput("Search distance", 20)) = {
    
    def getTargetOfType(creatureType: EnumCreatureType) = BotHelper.getNearestEntity(e =>
      e.isCreatureType(creatureType, false)
        && e.hurtResistantTime < 20
        && e.getDistanceToEntity(BotHelper.player) < maxDistance.get)
        
    def groundUnderTarget(creatureType: EnumCreatureType) = for {
      t <- getTargetOfType(creatureType)
      blockUnder <- BotHelper.firstBlockUnder(t.getLocation)
      groundUnder = blockUnder.yCoord.ceil
    } yield Vec3.createVectorHelper(blockUnder.xCoord, groundUnder + 1.62, blockUnder.zCoord)  
        
    
    /** 
     *  Walk towards and attack creatures of creatureType.
     *  TODO FollowPath to the creatures rather than walk straight towards them
     */
    def huntCreatureType(creatureType: EnumCreatureType) = ParallelNode(false, false)(
      attackEntity(() => getTargetOfType(creatureType), reach, attackFrequency),
//          new FollowPath(getTargetOfType(creatureType) map { _.getLocation } toSeq))
      Actions.walkWithinDistanceOf(howCloseToGo.get)(groundUnderTarget(creatureType)))
 

    val hunts = targets map { huntCreatureType(_) }
    
    /**
     * Always keep a distance from monsters.  (Repel out-prioritizes approach 
     * because it comes first in the ParallelNode)
     * 
     * Hunt the mobs in priority order, e.g. monsters before ambient creatures.
     */
    val node = ParallelNode(true, false)(
      ActionNode2 { (Running, Actions.steerUsingForce(repelFromMobs(repelDistance.get, repelStrength.get))) },
      PriorityNode(hunts:_*))
      
      val inputs = Seq(repelDistance, repelStrength, reach, howCloseToGo, attackFrequency, maxDistance)
      inputs map {node addGui}
      
      node
  }

  /**
   *  A Node that attacks (left-clicks) a target entity if possible.
   *  TODO remove McBot.leftClick() to see if InputState can be used instead
   */  
  def attackEntity(getTarget: () => Option[Entity], 
      reach: Input[Double] = new HiddenInput(4.4), 
      attacksPerSecond: => Input[Double] = new HiddenInput(10)): Node = {

    import com.kunii.mcbot.Camera
    
    var lastClickedTime = System.nanoTime()
    def timeSinceLastClick = System.nanoTime() - lastClickedTime
    def attackPeriod = 1e9 / attacksPerSecond.get
    
    ActionNode2 {
      getTarget() match {
        case None => (Failed, new InputState)
        case Some(entity) => {
          if (McBot.getPlayer().getDistanceToEntity(entity) < reach.get) {
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
        // subtract mob's location from our location
        // mob subtracted from us gives a vector from mob to us.
        val direction = BotHelper.entityLoc(e).subtract(BotHelper.playerLoc).normalize()
        
        // strength/r^2
        val magnitude = strength / BotHelper.player.getDistanceSqToEntity(e)
        accum.add(direction.multiply(magnitude))
      })
    repulsiveForce
  }
}