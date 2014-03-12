package adawg.minecraftbot

import net.minecraft.util.Vec3
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.Entity
import net.minecraft.world.World
import adawg.minecraftbot.pathfinding.Path3D
import adawg.minecraftbot.behaviortree.Node
import adawg.minecraftbot.pathfinding.Path3D._
import BotHelper._
import adawg.minecraftbot.behaviortree.BehaviorStatus._
import BotImplicits._
import adawg.minecraftbot.behaviortree.ActionNode
import scala.collection.JavaConversions._
import adawg.minecraftbot.behaviortree.SequenceNode
import adawg.minecraftbot.behaviortree.behaviors.FollowPath
import adawg.minecraftbot.behaviortree.ActionNode2
import adawg.minecraftbot.behaviortree.InputState

object BotImplicits {
  implicit def Vec3ToRichVec3(value : Vec3) = new RichVec3(value)
  implicit def RichVec3ToVec3(value: RichVec3) = value.getVec()
  implicit def EntityToRichEntity(e: Entity) = new RichEntity(e)
  implicit def Vec3ToIntTuple(v: Vec3): (Int, Int, Int) =
    (v.getXFloored(), v.getYFloored(), v.getZFloored())
  implicit def Vec3ToDoubleTuple(v: Vec3): (Double, Double, Double) =
    (v.getX(), v.getY(), v.getZ())
//  implicit def TupleToVec3(t: (Int, Int, Int)) = Vec3.createVectorHelper(t._1, t._2, t._3)
  implicit def StepToVec3(step: Path3D#Step) = step.toVec3
  implicit def javaToScalaIntTuple(d: (Integer, Integer, Integer)) =
    (d._1.intValue(), d._2.intValue(), d._3.intValue())
//  implicit def EntityPlayerToAdawgVec3D(value: EntityPlayer) = 
//    new RichVec3(value.posX, value.posY + 1, value.posZ)
////  implicit def AdawgVec3DToTuple(value: RichVec3) = (value.xCoord, value.yCoord, value.zCoord)
//  implicit def Path3DToSequence(p: Path3D) = {
//    for (step <- p.getSteps) yield ActionNode{}
//  }
}

class RichEntity(e: Entity) {
  def getLocation: Vec3 = Vec3.createVectorHelper(e.posX, e.posY, e.posZ)
  def feetY: Double = e.posY - 1.62
  def feetLoc: Vec3 = Vec3.createVectorHelper(e.posX, feetY, e.posZ)
}

class PathNode(path: Path3D) extends Node {
  var stepIndex: Int = 0
  def update = Success
  def resetState = stepIndex = 0
}
object RichPath {
  def stepCompleted(step: Path3D#Step) = {
	val pathLoc = step.toVec3().centered();
	val playerLoc = player.getLocation
			  //return pathLoc.distanceTo(playerLoc) < stepError;
	val threshold = 0.5
	val dx = Math.abs(pathLoc.getX() - playerLoc.getX());
	val dy = Math.abs(pathLoc.getY() - playerLoc.getY());
	val dz = Math.abs(pathLoc.getZ() - playerLoc.getZ());
	dx < threshold && dy < threshold && dz < threshold;
  }
  def stepToNode(step: Path3D#Step) = ActionNode2 {
//      if (player.feetLoc.floored().equals(step)) Success
    if (stepCompleted(step)) (Success, new InputState)
    else if (playerLoc.distanceTo(step) > 2) (Failed, new InputState)
    else {
//      println("BotHelper.walkTo2(step): " + BotHelper.walkTo2(step))
//    	(Running, InputState.mergeOver(BotHelper.walkWithoutLooking(step), BotHelper.lookAtYaw(step)))
    	(Running, InputState.mergeOver(BotHelper.walkWhileLooking(step, 4.3, 0.1, 0.1), BotHelper.lookAtYaw(step)))
    }
  }
  def pathToSequence(path: Path3D): Node = {
    val steps = for (step <- path.getSteps()) yield stepToNode(step)
    new SequenceNode(steps:_*)
  }
}