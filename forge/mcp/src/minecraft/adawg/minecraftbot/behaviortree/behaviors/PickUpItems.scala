package adawg.minecraftbot.behaviortree.behaviors

import adawg.minecraftbot.behaviortree._
import BehaviorStatus._
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3
import adawg.minecraftbot.BotImplicits._
import adawg.minecraftbot.behaviortree.NodeImplicits._
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._
import net.minecraft.entity.item.{EntityItem, EntityXPOrb}
import adawg.minecraftbot.BotHelper._
import adawg.minecraftbot.behaviortree.gui._

object PickUpItems {
  
  def apply(
      maxDistanceIn: Input[Double] = new DoubleInput("Item search radius", 20), 
      itemHeightIn: Input[Double] = new HiddenInput("Item height offset", 1.62)
      ): Node = {
    var target: Option[Entity] = None
    var blacklist: Set[Entity] = Set()
    
    def newTarget: Option[Entity] = filteredPickups.headOption
    def filteredPickups = nearbyPickups filter {
      !blacklist.contains(_)
    } filter { e => player.getDistanceToEntity(e) < maxDistanceIn.get
    } sortWith { (e1, e2) => player.getDistanceToEntity(e1) < player.getDistanceToEntity(e2)
    }

    def addTargetToBlacklist() = {
      target map { i => blacklist = blacklist ++ Set(i); println("blacklisting item at " + i) }
    }

    // Gets a new target.  Returns true if a new target is successfully acquired.
    def getNewTarget(): Boolean = {
      addTargetToBlacklist()
      target = newTarget
      target map { e => println("spotted item at " + entityLoc(e)) }
      target.isDefined
    }

//    val itemHeightIn = new DoubleInput("Item height offset", 1.62)

    def groundUnderTarget = for {
      t <- target
      blockUnder <- firstBlockUnder(t.getLocation)
      groundUnder = blockUnder.yCoord.ceil
    } yield Vec3.createVectorHelper(blockUnder.xCoord, groundUnder + itemHeightIn.get, blockUnder.zCoord)

    val pathfind = new FollowPath(groundUnderTarget.toSeq)
    //    val pathfind = new StatusPrinter(new Debouncer(new StatusPrinter(pathfind, "pathfind status "), 0.5), "debounced pathfind")

    val node = new ParallelNode(false, false)(
      ActionNode {
        val targetIsGone = target map { !nearbyPickups.contains(_) } getOrElse true
        if (targetIsGone) getNewTarget() else Failed
      },
      ActionNode {blacklist = Set()}.everyNSeconds(.5, "item blacklist reset period")(),
      PriorityNode(
        pathfind.withDebouncer(0.5).onSuccess(() => getNewTarget()),
        ActionNode { getNewTarget() }))

    node.guiComponent ++= itemHeightIn.guiComponent
    node.guiComponent ++= maxDistanceIn.guiComponent
    node
  }
}
