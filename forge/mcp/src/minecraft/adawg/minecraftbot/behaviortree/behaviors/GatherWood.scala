package adawg.minecraftbot.behaviortree.behaviors

import net.minecraft.util.Vec3
import adawg.minecraftbot.BotHelper._
import adawg.minecraftbot.behaviortree._
import adawg.minecraftbot.behaviortree.NodeImplicits._
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._
import adawg.minecraftbot.BotWrapper
import adawg.minecraftbot.BotHelper
import net.minecraft.block.Block

object GatherWood {

  // TODO: figure out why this aims a little bit below where it's supposed to
  def apply = {
    // Look in a cube 40 blocks long centered on the player
    def nearbyWood = nearbyLocations(playerLoc, 20) {
      loc => getBlockId(loc) == Block.wood.blockID
    } sortWith { playerLoc.distanceTo(_) < playerLoc.distanceTo(_) }
    var target: Option[Vec3] = None
    var blacklist: Set[(Int, Int, Int)] = Set()
    
    def newTarget: Option[Vec3] = nearbyWood filter { !isInBlacklist(_) } headOption

    def vec3ToIntTuple(vec: Vec3): (Int, Int, Int) =
      (vec.xCoord.floor.toInt, vec.yCoord.floor.toInt, vec.zCoord.floor.toInt)

    def addTargetToBlacklist() = target map { l => blacklist += vec3ToIntTuple(l) }
    
    def isInBlacklist(vec: Vec3): Boolean = blacklist.contains(vec3ToIntTuple(vec))
    
    def targetIsGone: Boolean = target map { l => getBlockId(l) != Block.wood.blockID } getOrElse true

    def node = ParallelNode(false, false)(
      ActionNode { target = newTarget; target.isDefined } withCondition { targetIsGone },
      PriorityNode(
        BreakBlocks(target.toSeq),
        //          BreakBlocks(nearbyWood),
        ActionNode { addTargetToBlacklist(); target = newTarget; target.isDefined }))
    node
  }
}