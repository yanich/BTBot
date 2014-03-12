package com.kunii.mcbot.bots
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
import adawg.minecraftbot.behaviortree.NodeImplicits._
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import adawg.minecraftbot.pathfinding.LocationUtils
import adawg.minecraftbot.pathfinding.Path3D
import adawg.minecraftbot.behaviortree.behaviors._
import adawg.minecraftbot.behaviortree.decorators._
import adawg.minecraftbot.BotHelper._
import adawg.minecraftbot.behaviortree.gui._
import adawg.minecraftbot.behaviortree.gui.Gui._
import adawg.minecraftbot.behaviortree.decorators.GuiToggle
import adawg.minecraftbot.util.Util
import adawg.minecraftbot.behaviortree.decorators.DecoratedNode
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._
import adawg.minecraftbot.BotWrapper
import adawg.minecraftbot.behaviortree.InputState
import adawg.minecraftbot.BotHelper

class BTBot extends CoreBot {
  
  lazy val rootNode: Node = 
    mkRootNode
    .withToggle
    .withActionButton("reset pathfinding map", resetPathfinder())
    .withStatusPrinter("root node status")
//    .withInputDisplay("root node inputs")
  def mkRootNode =
//        PriorityNode(makeHuntTest, makePickupTest) // Picks up items when not fighting mobs
      makePathfindTest // Pathfinds to goal A, or if it can't, to goal B.
//      makePickupTest // Picks up items within a certain distance of the player
  //    makeHuntTest.withToggle // Kills zombies, etc.  Tweak numbers in GUI for different mobs.

  lazy val gui = new BehaviorTreeWindow()
  var running: Boolean = false
  def botName: String = "BTBot"
  def inGameLoop(x: net.minecraft.client.Minecraft): Int = {
    val forwardsComponent = playerVelocity.dotProduct(forwardsVector)
    val sidewaysComponent = playerVelocity.dotProduct(rightVector)
    val (status, controlInput) = rootNode.update2()
    InputState.sendInput(controlInput)
    printEveryNSeconds(2)(controlInput.toString)
    0
  }
  
  var lastPrintTime = System.nanoTime()
  def printEveryNSeconds(period: Double)(output: => String) {
    if (System.nanoTime() - lastPrintTime > period * 1000000000) {
      McBot.sendMsgToPlayer(output)
      lastPrintTime = System.nanoTime()
    }
  }
  
  def inGameRenderer(x: net.minecraft.client.gui.FontRenderer): Unit = {}
  def init(): Unit = {}
  def locksMinecraft(): Boolean = false
  def resetPathfinder() = FollowPath.pathfinder.resetNodeStorage(100, 254, 100, player.posX.toInt, 0, player.posZ.toInt)
  
  import scala.swing.Swing
  def onStart(): Unit = {
    println("BTBot: Initializing behavior tree")
    Swing.onEDT {
      for (node <- rootNode) {
        println("adding guicomponent of " + node)
        gui.addComponent(node)
        gui.visible = true
      }
    }
    resetPathfinder()
  }
  def onStop(): Unit = {
    Swing.onEDT {gui.visible = false}
    Minecraft.getMinecraft().gameSettings.keyBindAttack.pressed = false;
  }

  def makeRootNode: Node = GuiToggle.apply _ *: {
    val heightInput = new IntInput("mining height", playerLoc.yCoord.toInt)
    gui.addComponent(heightInput)
//    gui.addComponent(new GuiComponent {
//      override lazy val guiComponent = Seq(rayTraceButton, exposedFaces)
//    })
    //    PriorityNode()
    //      new GuiToggle(
    //        ResetIfStatusMatches(Failed, Success)(
    //          new FollowPath(Seq(in1.get, in2.get)))),
    ActionNode { world.rayTraceBlocks(playerLoc, null); Success }
    val test2 = Util.throttledCachedFunction(1){
//      println(mine.targetBlockFaces())
      val output = blocksSatisfying(2, playerLoc.toIntTuple()) {
        (_, _, _) => true
      }
      println(output)
    }
    ActionNode{test2()}
    val oreIds = Set(14, 15, 16, 21, 56, 73, 74) // gold, iron, coal, lapis, diamond, redstone
    val itemIds = oreIds ++ Set(263, 264, 331) // diamond, coal, redstone
    val mine = new TryMiningBlocks(oreIds)
//    new FollowPath(Seq(in1.get, in2.get))
    val mineBlocks = new MineBlocks(heightInput.get, oreIds, itemIds)
    new PickUpItems(7, itemIds)
    val mineTest = PriorityNode(false, 0.2)(
      // PickUpItems seems to make the rest of the tree update slowly
//      new PickUpItems(7, itemIds),
//      FollowPath(Vec3.createVectorHelper(0, 0, 0)),
      new TryMiningBlocks(oreIds),
      new DigSideways withCondition {
        (playerLoc.yCoord - heightInput.get).abs < 1
      } withBenchmark("DigSideways"),
      new DigDown withBenchmark,
      new DigSideways withBenchmark
      )
//    FollowPath(playerLoc add (-1, 0, 0))
//    testNode
//    new DigSideways withBenchmark ""
    gui.addComponent(actionButton("Reset pathfinding map", e => 
      resetPathfinder()))
    mineBlocks
    new DigSideways
    val itemPickupToggle = new BoolInput("Pick up items", false)
    val woodHuntToggle = new BoolInput("Hunt wood", false)
    gui.addComponent(itemPickupToggle)
    gui.addComponent(woodHuntToggle)
    
    val pickupItems = pickUpItems(new FunInput(20))
//    gui.addComponent(actionButton("Skip item", e => pickupItems.skipItem()))
    gui.addComponent(actionButton("Nearby wood", e => McBot.sendMsgToPlayer(huntWood.toString)))
    
    val priorityPeriod = new DoubleInput("Time between child checks", 0.2)
    gui.addComponent(priorityPeriod)
    
    val harvestWood = PriorityNode(false, priorityPeriod.get)(
        pickupItems.withCondition {itemPickupToggle.get},
        huntWood withCondition {woodHuntToggle.get})
    harvestWood
  }
  
  def huntWood = {
    import net.minecraft.block.Block
    // Look in a cube 40 blocks long centered on the player
    def nearbyWood = nearbyLocations(playerLoc, 20) {
      loc => getBlockId(loc) == Block.wood.blockID
      } sortWith {playerLoc.distanceTo(_) < playerLoc.distanceTo(_)}
    var target: Option[Vec3] = None
    var blacklist: Set[(Int, Int, Int)] = Set()
    def newTarget: Option[Vec3] = nearbyWood filter {!isInBlacklist(_)} headOption
    
    def vec3ToIntTuple(vec: Vec3): (Int, Int, Int) = 
      (vec.xCoord.floor.toInt, vec.yCoord.floor.toInt, vec.zCoord.floor.toInt)
    
    def addTargetToBlacklist() = target map {l => blacklist += vec3ToIntTuple(l)}
    def isInBlacklist(vec: Vec3): Boolean = blacklist.contains(vec3ToIntTuple(vec))
    def targetIsGone: Boolean = target map {l => getBlockId(l) != Block.wood.blockID} getOrElse true
    
    def node = ParallelNode(false, false) (
      ActionNode {target = newTarget; target.isDefined} withCondition {targetIsGone},
      PriorityNode(
        BreakBlocks(target.toSeq),
//          BreakBlocks(nearbyWood),
        ActionNode { addTargetToBlacklist(); target = newTarget; target.isDefined })
    )
    node
  }
  
  def makePathfindTest: Node = {
    val in1 = new Vec3Input("target 1", playerLoc)
    val in2 = new Vec3Input("target 2", playerLoc)
    
    val save1 = actionButton("Set goal 1", _ => in1.set(playerLoc))
    val save2 = actionButton("Set goal 2", _ => in2.set(playerLoc))
    
    def swapTargets() = {
      val temp = in1.get
      in1.set(in2.get)
      in2.set(temp)
    }
    val swapButton = actionButton("Swap targets", _ => swapTargets())
    
    val followPath = new FollowPath(Seq(in1.get, in2.get))
    
    followPath.guiComponent ++= Seq(Gui.vertBox(save1, save2, swapButton))
    
    followPath
  }
  
  def makePickupTest: Node = {
    val input = new DoubleInput("Max item distance", 20)
    val node = pickUpItems(maxDistanceIn = input)
//    gui.addComponent(actionButton("skip item", e => node.skipItem))
    node
  }
  
  def makeHuntTest: Node = {
    val repelDistance = new DoubleInput("Repel distance", 3)
    val repelStrength = new DoubleInput("Repel strength", 1.01)
    val reach = new DoubleInput("Attack reach", 4.4)
    val howCloseToGo = new DoubleInput("How close to go", 4)
    val attackFrequency = new DoubleInput("Attacks per second", 50)
    val breathThreshold = new IntInput("Breath threshold", 280)
    val timeToBreathe = new DoubleInput("Time to breathe", 3e8)
    val inputs = Seq(repelDistance, repelStrength, reach, howCloseToGo, attackFrequency)
//    Swing.onEDT {
//      gui.addComponent(repelDistance)
//      gui.addComponent(repelStrength)
//      gui.addComponent(reach)
//      gui.addComponent(howCloseToGo)
//      gui.addComponent(attackFrequency)
//      gui.addComponent(breathThreshold)
//      gui.addComponent(timeToBreathe)
//    }
    
    import EnumCreatureType._
    val targetEntities = Seq(monster, creature)
    val huntTree = huntTest(repelDistance.get, repelStrength.get, reach.get, howCloseToGo.get, targetEntities, attackFrequency.get)
    
//    huntTree.guiComponent ++= (inputs map {_.guiComponent})
    huntTree.guiComponent ++= inputs flatMap {_.guiComponent}
    huntTree
//    withReflexes(huntTree, breathThreshold.get, timeToBreathe.get.toLong)
  }

  // Attacks (left-clicks) a target entity.  
  def attackEntity(target: => Option[Entity], reach: => Double = 4.4, attacksPerSecond: => Double = 10): Node = {
    var lastClickedTime = System.nanoTime()
    def timeSinceLastClick = System.nanoTime() - lastClickedTime
    def attackPeriod = 1e9 / attacksPerSecond
    
    ActionNode {
      target match {
        case None => Failed
        case Some(entity) => {
          if (McBot.getPlayer().getDistanceToEntity(entity) < reach) {
            Camera.turnCam(entity, 30)
            if (timeSinceLastClick > attackPeriod) {
              McBot.leftClick()
              lastClickedTime = System.nanoTime()
              Success
            } else Running
          } else Failed
        }
      }
    }
  }
      
  def huntTest(repelDistance: => Double, repelStrength: => Double, 
      reach: => Double, 
      howCloseToGo: => Double, 
      targets: Seq[EnumCreatureType],
      attackFrequency: => Double = 10,
      maxDistance: => Double = 20) = {
    def getTargetOfType(creatureType: EnumCreatureType) = getNearestEntity(e =>
      e.isCreatureType(creatureType, false)
        && e.hurtResistantTime < 20
        && e.getDistanceToEntity(McBot.getPlayer()) < maxDistance)
        
    def huntCreatureType(creatureType: EnumCreatureType) = ParallelNode(false, false)(
      attackEntity(getTargetOfType(creatureType), reach, attackFrequency),
//          new FollowPath(getTargetOfType(creatureType) map { _.getLocation } toSeq))
      walkWithinDistanceOf(howCloseToGo)(groundUnderTarget(creatureType)))

    def groundUnderTarget(creatureType: EnumCreatureType) = for {
      t <- getTargetOfType(creatureType)
      blockUnder <- firstBlockUnder(t.getLocation)
      groundUnder = blockUnder.yCoord.ceil
    } yield Vec3.createVectorHelper(blockUnder.xCoord, groundUnder + 1.62, blockUnder.zCoord)   

    val hunts = targets map { huntCreatureType(_) }
    ParallelNode(true, false)(
      ActionNode2 { (Success, steerUsingForce2(repelFromMobs(repelDistance, repelStrength))) },
      PriorityNode(hunts:_*))
  }
  
  

  
  import adawg.minecraftbot.behaviortree.gui.Input
  
  def pickUpItems(
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
      ActionNode {blacklist = Set()}.everyNSeconds(.5)(),
      PriorityNode(
        pathfind.withDebouncer(0.5).onSuccess(() => getNewTarget()),
        ActionNode { getNewTarget() }))

    node.guiComponent ++= itemHeightIn.guiComponent
    node.guiComponent ++= maxDistanceIn.guiComponent
    node
  }
  
  
  /**
   * Precondition: Have clear line of sight to goal (Nothing in the way at all)
   * Postcondition: Player should be within goalDistance of goal.
   */
  def walkToPrecisely(goal: => Option[Vec3]) = ActionNode2 {
    goal map {g => 
    LocationUtils.isDirectPathBetweenPoints(playerLoc, g) match {
      case true =>
        (Running, walkWhileLooking(g, 4.3, 0.1, 0.2))
      case false => (Failed, InputState())
    }
    } getOrElse (Failed, new InputState)
  }
  
  def withReflexes(child: Node, breathThreshold: => Int, timeToBreathe: => Long) = {
    import adawg.minecraftbot.BotHelper._
    val antiDrown = {
      var lastAirTime = 0L
      ActionNode2 {
        def checkForAir() = {
          if (player.getAir() < breathThreshold) {
            lastAirTime = System.nanoTime
            true
          } else false
        }
        
        // If you only go to the surface for a really short time, it doesn't register on the server.
        val shouldJump = checkForAir || System.nanoTime() - lastAirTime < timeToBreathe
        if (shouldJump) (Success, InputState(jump = Some(true)))
        else (Failed, InputState())
      }
    }

    ParallelNode(false, true)(
      antiDrown,
      child)
  }

//  def walkWhileLooking(goal: Vec3, maxSpeed: Double, threshold: Double): InputState = {
//    val desiredVelocity = goal.subtract(playerLoc).normalize().multiply(maxSpeed)
//    val steering = playerVelocity.subtract(desiredVelocity)
//    val horizontalComponent = Math.sqrt(steering.xCoord * steering.xCoord + steering.zCoord * steering.zCoord)
////    val shouldWalk = horizontalComponent > threshold
//    
//    // Dot product between steering and the vector pointing from player to goal.
//    // If this is negative, it's telling us to steer backwards
//    val dotProduct = steering.dotProduct(goal.subtract(playerLoc))
//    val walkInput = if (horizontalComponent < threshold) new InputState
//    else if (dotProduct > 0) InputState(forward = Some(true))
//    else InputState(back = Some(true))
//    
//    val lookInput = lookAtYaw(goal)
//    lookInput.mergeOver(walkInput)
//  }

  def walkWithinDistanceOf(distance: Double)(loc: => Option[Vec3]) = {
    ActionNode2 {
      loc match {
        case None => (Failed, new InputState) // No one to walk to
        case Some(loc) => {
          val input = if (playerLoc.distanceTo(loc) > distance)
//            walkWithoutLooking(Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ))
            walkWhileLooking(loc, 4.3, 0.1, 0.2)
            else new InputState
          (Success, input)
        }
      }
    }
  }
  
  def recordingTest = {
    val testRecord = List(
        Frame(0, new InputState),
        Frame(1e8, InputState(forward = Some(true))),
        Frame(2e8, InputState(forward = None)),
        Frame(1e9, InputState(forward = Some(true))),
        Frame(2e9, InputState(forward = None)))
    val playback = new Playback(testRecord)
    val recorder = new Record(30, {playback.setRecording(_)})
    gui.addComponent(actionButton( "Save recording", e => recorder.endRecording()))
    ParallelNode(playback withToggle, recorder withToggle)
    gui.addComponent(actionButton("reset playback node", e => playback.resetState()))
    playback
  }
  
}