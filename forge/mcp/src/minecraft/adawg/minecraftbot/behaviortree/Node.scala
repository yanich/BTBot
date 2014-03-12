package adawg.minecraftbot.behaviortree
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import adawg.minecraftbot._
import adawg.minecraftbot.BotWrapper._
import adawg.minecraftbot.behaviortree.BehaviorStatus._
import net.minecraft.util.Vec3
import scala.util.Random
import net.minecraft.entity.player.EntityPlayer
import scala.swing.Component
import adawg.minecraftbot.behaviortree.gui.GuiComponent
//import adawg.minecraftbot.behaviortree.InputState

object NodeImplicits {
  implicit def unitToBehaviorStatusSuccess(unit: Unit) = Success
  implicit def booleanToBehaviorStatus(bool: Boolean) = bool match {
    case true => Success
    case false => Failed
  }
}

abstract class Node extends Traversable[Node]
  with GuiComponent {
  type NodeOutput = (BehaviorStatus, InputState)
  def update(): BehaviorStatus
  def update2(): NodeOutput = (update, new InputState)
  def resetState(): Unit
  val children: Seq[Node] = Seq()
  def foreach[U](f: Node => U) = {
    f(this)
    children map {_ foreach f }
  }
  override def toString = "node " + this.getClass()
	  
  def *:(decorator: Node => Node) = decorator(this)
}

/*
 * Node that does nothing but check a condition
 * If true, the node succeeds; if false, it fails
 */
class ConditionNode (condition : => Boolean) extends Node {
  def update = update2._1
  override def update2: NodeOutput = condition match {
    case true => (Success, InputState())
    case false => (Failed, InputState())
  }
  def resetState {}
}
object ConditionNode {
  def apply (condition : => Boolean) = new ConditionNode(condition)
}
/*
 * Node that wraps a function returning a BehaviorStatus.  
 * If it returns unit it is implicitly converted to Success using NodeImplicits
 */
class ActionNode(action: => BehaviorStatus) extends Node {
  def update : BehaviorStatus = action
  def resetState = {}
}
object ActionNode {
  def apply(action: => BehaviorStatus) = new ActionNode(action)
}


class ActionNode2(action: => (BehaviorStatus, InputState)) extends Node {
  def update = update2._1
  override def update2: (BehaviorStatus, InputState) = action
  def resetState = {}
}
object ActionNode2 {
  def apply(action: => (BehaviorStatus, InputState)) = new ActionNode2(action)
}

/**
 * Every update, updates all its children from first to last until one returns success or running.
 * If childCheckPeriod is set to a number > 0, the last-running child continues to run and other 
 * children are not checked until that many seconds have elapsed.
 * Returns success/running/error if a child does.  Fails if all children fail.
 * Children get reset when they get interrupted by a higher or lower priority child.
 * If succeedWithFirst is true, returns Success only if highest priority child succeeds.
 */
class PriorityNode (succeedOnlyWithFirstChild: Boolean = false, childCheckPeriod: => Double = 0)(override val children : Node*) extends Node {
  var lastRunningNode: Option[Node] = None
  var lastTime = 0L
  // Checks if it is time to check the other child nodes, or keep going with the last one to run
  def checkTime: Boolean = {
    val currentTime = System.nanoTime()
    if ((currentTime - lastTime) > childCheckPeriod * 1e9) {
      lastTime = currentTime; true
    } else false
  }
  
  def update = update2._1
  override def update2: NodeOutput = {
    
	// In-between checks, just run the last running node.  This saves cpu
    if (!checkTime && lastRunningNode.isDefined) return lastRunningNode.get.update2
    
    for (child <- children) {
      val (status, controlInput) = child.update2()
      status match {
        case Failed => // Try the next child in line
        case Error => return (Error, InputState())
        case x @ (Success | Running) => {
          val runningNodeHasChanged = lastRunningNode map (child !=) getOrElse (true)
          if (runningNodeHasChanged) {
            lastRunningNode.map(_.resetState())
            lastRunningNode = Some(child)
          }
          return if (x == Success && succeedOnlyWithFirstChild) {
            if (child == children.head) (Success, controlInput) else (Running, controlInput)
          } else (status, controlInput)
        }
      }
    }
    (Failed, InputState())
  }
  
  def resetState {
    children.foreach(_.resetState())
    lastRunningNode = None
  }
}
object PriorityNode {
  def apply(children: Node*) = new PriorityNode(false)(children:_*)
  def apply(succeedWithFirst: Boolean = false, childCheckPeriod: => Double = 0)(children: Node*) = 
    new PriorityNode(succeedWithFirst, childCheckPeriod)(children:_*)
}

class SequenceNode (override val children : Node*) extends Node {
  var nextChildToUpdateIndex = 0
  
  override def update2: NodeOutput = {
    while (nextChildToUpdateIndex < children.length) {
      val status = children(nextChildToUpdateIndex).update2()
      status match {
        case (Success, input) =>
          nextChildToUpdateIndex += 1; return (Running, input)
        case x => return x
      }
    }
    (Success, new InputState)
  }
  
//  def update : BehaviorStatus = {
//    while (nextChildToUpdateIndex < children.length) {
//      val status = children(nextChildToUpdateIndex).update()
//      status match {
//        case Success => nextChildToUpdateIndex += 1
//        case x => return x
//      }
//    }
//    Success
//  }
  def update: BehaviorStatus = update2()._1
  def resetState = {
    children.foreach(_.resetState())
    nextChildToUpdateIndex = 0
  }
}
object SequenceNode {
  def apply(children: Node*) = new SequenceNode(children:_*)
}

/**
 * This node normally runs all its children in order every update.
 * There are four different modes for this type of node.
 * If failOnOneFails is true and a child returns Failed, evaluation short-circuits and (Failed, new InputState) is returned.
 * Otherwise, this node fails only when all children return Failed.  In that case, it also returns (Failed, new InputState).
 * If succeedOnLast is true, this node succeeds when the last child on the list does.
 * Otherwise, this node succeeds only when all children return Success.
 * 
 * 
 * If any child returns error, this node does as well.
 * In all other cases, this node will return running.
 */
class ParallelNode (val failIfOneFails: Boolean, val succeedOnLast: Boolean)
				   (override val children : Node*) extends Node {
//  def update() : BehaviorStatus = {
//    var childStatuses = ListBuffer.empty[BehaviorStatus]
//    for (child <- children) {
//      val status = child.update()
//      if (status == Error) return Error
//      else if (failOnOne && status == Failed) return Failed
//      else childStatuses += status
//    }
//    if (childStatuses.forall(_ == Failed)) Failed 
//    else if (succeedOnLast && (childStatuses.last == Success)) Success
//    else if (childStatuses.forall(_ == Success)) Success
//    else Running
//  }
  def update = update2._1
  
  override def update2: NodeOutput = {
    var childStatuses = ListBuffer.empty[NodeOutput]
    for (child <- children) {
      val (status, input) = child.update2()
      if (status == Error) return (Error, new InputState)
      else if (failIfOneFails && status == Failed) return (Failed, new InputState)
      else childStatuses += ((status, input))
    }
//    val mergedInput = childStatuses.foldLeft((Failed, new InputState))((p: NodeOutput, p2: NodeOutput) =>
//      InputState.mergeOver(p._2, p2._2))
    val mergedInput = childStatuses.map(_._2).foldLeft(new InputState)(InputState.mergeOver)
    if (childStatuses.forall(_._1 == Failed)) (Failed, new InputState) 
    else if (succeedOnLast && (childStatuses.last._1 == Success)) (Success, mergedInput)
    else if (childStatuses.forall(_._1 == Success)) (Success, mergedInput)
    else (Running, mergedInput)
  }
  
  def resetState = {children.foreach(_.resetState())}
}
object ParallelNode {
  def apply(failOnOne: Boolean, succeedOnLast: Boolean)(children : Node*) = 
    new ParallelNode(failOnOne, succeedOnLast)(children:_*)
  def apply(children: Node*): ParallelNode = apply(false, false)(children:_*)
}