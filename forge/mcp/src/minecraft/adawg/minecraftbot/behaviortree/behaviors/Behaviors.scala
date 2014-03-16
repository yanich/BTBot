package adawg.minecraftbot.behaviortree.behaviors
import adawg.minecraftbot._
import behaviortree._
import behaviortree.BehaviorStatus._
import behaviortree.NodeImplicits._
import BotImplicits._
import scala.util.Random
import adawg.minecraftbot.RichVec3
import adawg.minecraftbot.BotWrapper
import adawg.minecraftbot.BotHelper._
import adawg.minecraftbot.BotImplicits._
import net.minecraft.util.Vec3
import adawg.minecraftbot.pathfinding.Path3D
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.control.ControlThrowable
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Failure
import java.util.concurrent.atomic.AtomicBoolean
import com.kunii.mcbot.McBot
import com.kunii.mcbot.Camera
import net.minecraft.client.Minecraft
import scala.collection.JavaConversions._
import com.google.common.util.concurrent.RateLimiter
import adawg.minecraftbot.pathfinding.LocationUtils
import net.minecraft.block.material.Material
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._
import adawg.minecraftbot.util.Util
import adawg.minecraftbot.behaviortree.gui.DoubleInput

/**
 * A lot of the stuff in this source file doesn't work
 */


/**
 * Break blocks if they're in range.  Otherwise, move into position to break them.
 * Success if all target blocks are air.  Also, there has to be at least one target.
 * Running if blocks are being attacked or we are moving into position.
 * Failed if no blocks are in range and no path can be found to a place where we can hit them.
 */
class BreakBlocks(targets: => Seq[Vec3]) extends Node {
  /**
   * Can't call findExposedFaces too often or the game glitches out (player moves, screen flashes)
   * This function calculates the exposed faces of all target blocks up to 5 times per second.
   */
  val getFaces = Util.throttledCachedFunction(5) {
    targets.flatMap(findExposedFaces(_))
  }

  def whereToStand = LocationUtils.whereToStandToClick(getFaces(targets), false)

  val printLimiter = RateLimiter.create(2)
  private lazy val node = PriorityNode(succeedWithFirst = true)(
    ActionNode2 {
//      setClick(false) // should be unnecessary
      if (printLimiter.tryAcquire())
        println("blocks at " +
          targets.foldLeft("") { _ + ", " + _.toStringFancy } +
          " are air")
//          (Success, InputState(mouse1 = Some(false)))
          (Success, InputState())
    } withCondition { targets.forall { getBlockMaterial(_) == Material.air } },
    new TryBreakingBlocks(getFaces(targets)),
    new FollowPath(whereToStand))
  override def update2: NodeOutput = node.update2()
  def update = update2._1

  def resetState = {
    node.resetState()
//    setClick(false)
  }
}
object BreakBlocks {
  def apply(blocks: => Seq[Vec3]) = new BreakBlocks(blocks)
}

class TryBreakingBlocks(targets: => Seq[Vec3]) extends Node {

  /**
   * Click on one of the targets if it can be done from where the player stands
   * @return success if a neighborFace is in range and being clicked; failed otherwise
   */
  def update = update2._1
  override def update2: NodeOutput = {
    for (targetFace <- targets) {
      val inRange = LocationUtils.distanceTo(targetFace) < 4;
      if (inRange && LocationUtils.canSee(playerLoc, targetFace)) {
//        Camera.turnCam(targetFace.xCoord, targetFace.yCoord, targetFace.zCoord, 30)
//        // Actually left click please
//        if (!Minecraft.getMinecraft.gameSettings.keyBindAttack.pressed)
//          setClick(true)
        val lookInput = BotHelper.lookAt(targetFace)
        return (Success, InputState(mouse1 = Some(true)).mergeOver(lookInput))
      }
    }
//    setClick(false)
    (Failed, InputState())
  }
  def resetState = {
//    setClick(false)
  }
}

class TryMiningBlocks(blockIds: => Set[Int]) extends Node {
    def targetBlocks = blocksSatisfying(5, playerLoc.toIntTuple) { (x: Int, y: Int, z: Int) =>
      blockIds.contains(world.getBlockId(x, y, z)) &&
        isSafeToMine(new RichVec3(x, y, z))
    }
  val targetBlockFaces = {
    Util.throttledCachedFunction(5)(targetBlocks flatMap { block =>
      findExposedFaces(Vec3.createVectorHelper(block._1, block._2, block._3))
      })
  }
  val targetBlocksCache = {
    Util.throttledCachedFunction(5)(targetBlocks map { block => 
      Vec3.createVectorHelper(block._1, block._2, block._3)
    })
  }
  
//  lazy val node = new TryBreakingBlocks(targetBlockFaces())
  lazy val node = new BreakBlocks(targetBlocksCache()) withCondition { !targetBlocksCache().isEmpty}
//  lazy val node = new TryBreakingBlocks(findExposedFaces(targetBlocks))
  
  override def update2 = node.update2()
  def update = update2._1
  def resetState = node.resetState()
}

class MineBlocks(height: => Int, blockIds: => Set[Int], itemIds: => Set[Int]) extends Node {
  lazy val node = PriorityNode(
      // PickUpItems seems to make the rest of the tree update slowly
//      new PickUpItems(7, itemIds),
      new TryMiningBlocks(blockIds) withBenchmark "Mineblocks -> TryMiningBlocks(blockIds)",
      new DigSideways withCondition {
        (playerLoc.yCoord - height).abs < 1
      },
      new DigDown,
      new DigSideways
      )
  def update = update2._1
  override def update2: NodeOutput = node.update2()
  def resetState = node.resetState()
}

/**
 * Dig sideways in a direction as long as it's safe
 * If it's not safe go another direction
 */
class DigSideways extends Node {
  // A unit vector in +x, -x, +z, or -z
  def randomDirection = Random.shuffle(directions).head
  
  // The blocks at head and leg level
  def occupiedBlocks = Seq(playerLoc, playerLegs)
  
  // Try to break the blocks in the next 4 spaces ahead
  def blocksToMine = (1 to 2) flatMap { i =>
    occupiedBlocks map {_ add (direction multiply i)}
  }
  
  def canProceed = {
    blocksToMine.forall { isSafeToMine(_) } &&
    isSolid(playerFloor add direction)
  }
  val facesOfBlocksToMine = Util.throttledCachedFunction(5){
    val blocksToMineTimed = Util.printExecTime("blocksToMine", 5){blocksToMine}
//    val sortedBlocks = Util.printExecTime("blocksToMineTimed sortWith distance", 5) {
//      blocksToMineTimed sortWith {
//        playerLoc.distanceTo(_) < playerLoc.distanceTo(_)
//      }
//    }
    val exposedFaces = Util.printExecTime("blocksToMineTimed flatMap findExposedFaces", 5) {
      blocksToMineTimed flatMap {findExposedFaces(_)}
    }
    exposedFaces
  }
 
  var direction = randomDirection
  
  // Go forward and dig the blocks ahead as long as it is safe to do so
  lazy val node = PriorityNode(
    ParallelNode(
      FollowPath(playerLoc add direction),
//      new TryBreakingBlocks(facesOfBlocksToMine())
      new BreakBlocks(blocksToMine)
    ) withCondition {
        canProceed
      } withBenchmark "ParallelNode in DigSideways",
    ActionNode(direction = randomDirection))
  override def update2 = node.update2()
  def update = update2._1
  def resetState = {}
}

/**
 * Digs down as long as there's no lava or air on all neighboring blocks underneath.
 * Returns running if digging, else failed.
 */
class DigDown extends Node {
  /**
   * Return true if its safe to stand at loc and dig straight down.
   */
  def canDigDown(loc: Vec3): Boolean = {
    firstBlockUnder(loc) match {
      case None => false // You have reached the bottom of the map
      case Some(floor) => {
        val underFloor = floor.add(0, -1, 0)
        isSafeToMine(floor) && isSolid(floor) && isSolid(underFloor)
      }
    }
  }
  def update = update2._1
  override def update2: NodeOutput = {
    if (canDigDown(playerLoc)) {
      val input = steerUsingForce2(arrivalVelocity(playerLoc.centered)) mergeOver 
      lookAt(playerLoc.addVector(0, -100, 0)) mergeOver
      InputState(mouse1 = Some(true))
//      Camera.turnCam(playerLoc.xCoord, -100, playerLoc.zCoord, 30)
      //      McBot.isLeftClick = true
//      Minecraft.getMinecraft.gameSettings.keyBindAttack.pressed = true
      (Running, input)
    } else {
//      Minecraft.getMinecraft.gameSettings.keyBindAttack.pressed = false
      (Failed, InputState())
    }
  }
  def resetState = {
//    Minecraft.getMinecraft.gameSettings.keyBindAttack.pressed = false
  }
}
