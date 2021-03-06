package adawg.minecraftbot.behaviortree.behaviors
import adawg.minecraftbot._
import behaviortree._
import behaviortree.BehaviorStatus._
import behaviortree.NodeImplicits._
import BotImplicits._
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
import scala.collection.JavaConversions._
import com.google.common.util.concurrent.RateLimiter
import adawg.minecraftbot.pathfinding.LocationUtils
import net.minecraft.block.material.Material
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._
import adawg.minecraftbot.behaviortree.gui.DoubleInput
import adawg.minecraftbot.behaviortree.gui.Input

/**
 * Do everything it takes to reach one of the goals.
 * Returns success when a goal is reached.  Fails if pathfinder returns no path.
 */
class FollowPath(goals: => Seq[Vec3]) extends Node {
  import FollowPath._

  // Returns a future path from the player's location to the goal.
  private def pathFuture(goal: Vec3): Future[Path3D] = {
    val promise = Promise[Path3D]
    future {
      val cancelFlag = new AtomicBoolean(false)
      val calculation = future {
        pathfinder.findPath(player.getLocation, goal, 400, cancelFlag)
      }
      try {
        val result = Await result (calculation, 0.1 seconds)
        if (result == null) {
          cancelFlag.set(true)
          promise.failure(new NoPathException())
        } else promise success result
      } catch {
        case e: TimeoutException => cancelFlag.set(true); promise failure e
      }
    }
    promise.future
  }

  // Calculate paths to all goals simultaneously, yield the earliest one completed.
  private def firstCalculated(goals: Seq[Vec3]): Future[Path3D] = {
    val futures = for (goal <- goals) yield pathFuture(goal)
    Future.firstCompletedOf(futures)
  }
  // Calculate paths to all goals simultaneously and yield the first one on the list that succeeds.
  private def tryInOrderConcurrent(goals: Seq[Vec3]): Future[Path3D] = {
    val futures = for (goal <- goals) yield pathFuture(goal)
    futures.foldLeft(Future.failed[Path3D](new NoGoalsException)) { _ fallbackTo _ }
  }

  // Tries calculating a path to each goal in order until one succeeds or all have failed.
  private def tryInOrder(goals: Seq[Vec3]): Future[Path3D] = {
    goals.foldLeft(Future.failed[Path3D](new NoGoalsException)) {
      (future: Future[Path3D], goal: Vec3) =>
        future recoverWith {
          case e @ (_: NoGoalsException | _: NoPathException | _: TimeoutException) => pathFuture(goal)
        }
    }
  }

  var lastGoals = goals map { new RichVec3(_) }
  var hasFailedWithGoals = false
  
  var pathSequence: Option[Node] = None
  var pathCalculation: Option[Future[Path3D]] = None

  val printerLimiter = RateLimiter.create(2)
  
  def pathLogic(path: Path3D): NodeOutput = {
    if (printerLimiter.tryAcquire()) {
      println(path)
    }
    pathSequence map { _.update2() } getOrElse {
      pathSequence = Some(RichPath.pathToSequence(path))
      (Running, new InputState)
    }
  }

  val pathThrottle = RateLimiter.create(2)
  def update(): BehaviorStatus = update2._1
  override def update2: NodeOutput = {
    // This seems like flawed logic (if the goal list simply changes order, this may return true)
    def goalsHaveChanged = {
      lastGoals.length != goals.length ||
        (lastGoals zip goals).exists { case (old, current) =>
          old.distanceTo(current) > 0.1
        }
    }
    
    if (pathThrottle.tryAcquire()) {
      if (goalsHaveChanged) {
//        println("goals changed from " + lastGoals + "\n to " + goals)
        lastGoals = goals map { new RichVec3(_) }
        hasFailedWithGoals = false
      }
//      pathCalculation = Some(firstCalculated(goals))
      pathCalculation = Some(tryInOrder(goals))
      pathCalculation map {_ onFailure {
        case _ => hasFailedWithGoals = true
      }}
      pathSequence = None
    }
    
    val lastMileOutput = 
      goals.filter {g: Vec3 => 
//        playerLoc.distanceTo(g) < 1.41
        LocationUtils.isDirectPathBetweenPoints(playerLoc, g)
    }.headOption map { g: Vec3 => (Running, BotHelper.walkWhileLooking(g, 4.3, 0.1, 0.2)) 
    } 
    val followingPathOutput = pathCalculation map {
      _.value.map {
        case scala.util.Success(p) => hasFailedWithGoals = false; pathLogic(p)
        case scala.util.Failure(p) => hasFailedWithGoals = true; (Failed, new InputState)
      } getOrElse (if (hasFailedWithGoals) Failed else Running, new InputState) // future has not completed.
    } getOrElse (Running, new InputState) // We are in between paths.
    
    
    import BotImplicits._
    def isFinished = goals exists {g => playerLoc.horizontalDistanceTo(g) < goalRadiusIn.get}

    val output = if (isFinished) (Success, new InputState)
    else lastMileOutput getOrElse followingPathOutput
    
     output
  }

  val goalRadiusIn = new DoubleInput("pathfind goal radius", 0.3)
  guiComponent = goalRadiusIn.guiComponent

  def resetState = {
    pathSequence = None
    pathCalculation = None
    lastGoals = goals map { new RichVec3(_) }
    hasFailedWithGoals = false
  }
}
object FollowPath {
  def apply(goal: => Vec3) = new FollowPath(Seq(goal))
  
  lazy val pathfinder = new MinecraftPathFinder(400, 100, 400)
  
  def resetPathfinder() = pathfinder.resetNodeStorage(100, 254, 100, player.posX.toInt, 0, player.posZ.toInt)
  
  case class NoPathException extends Throwable
  case class NoGoalsException extends Throwable
  
  // Returns a future path from the player's location to the goal.
  def pathFuture(goal: Vec3, 
      timeout: FiniteDuration = 0.1 seconds,
      cancelFlag: AtomicBoolean = new AtomicBoolean(false)): Future[Path3D] = {
    val promise = Promise[Path3D]
    future {
      val calculation = future {
        FollowPath.pathfinder.findPath(player.getLocation, goal, 400, cancelFlag)
      }
      try {
        val result = Await result (calculation, timeout)
        if (result == null) {
          cancelFlag.set(true)
          promise.failure(new NoPathException())
        } else promise success result
      } catch {
        case e: TimeoutException => cancelFlag.set(true); promise failure e
      }
    }
    promise.future
  }

  // Calculate paths to all goals simultaneously, yield the earliest one completed.
  def firstCalculated(goals: Seq[Vec3]): Future[Path3D] = {
    val futures = for (goal <- goals) yield pathFuture(goal)
    Future.firstCompletedOf(futures)
  }
  
  // Calculate paths to all goals simultaneously and yield the first one on the list that succeeds.
  def tryInOrderParallel(goals: Seq[Vec3]): Future[Path3D] = {
    val futures = for (goal <- goals) yield pathFuture(goal)
    futures.foldLeft(Future.failed[Path3D](new NoGoalsException)) { _ fallbackTo _ }
  }

  // Tries calculating a path to each goal in order until one succeeds or all have failed.
  def tryInOrder(goals: Seq[Vec3]): Future[Path3D] = {
    goals.foldLeft(Future.failed[Path3D](new NoGoalsException)) {
      (future: Future[Path3D], goal: Vec3) =>
        future recoverWith {
          case e @ (_: NoGoalsException | _: NoPathException | _: TimeoutException) => pathFuture(goal)
        }
    }
  }
  
  
}

/**
 * New W.I.P. version of FollowPath
 * Eliminates pathSequence to allow tweaking path-following parameters.
 * @param goal the place to pathfind to
 * @param goalRadius the radius within which a step is considered "done"
 * @param maxSpeed the steering algorithm's output is normalized to this magnitude
 * @param walkThreshold the player walks forward if the forward component of steering > this
 * @param jumpThreshold the player jumps up if the upward component of steering > this
 * 
 * @param pathfindTimeout Path calculations are canceled after this many seconds.
 * 
 */
import FollowPath._
class FollowPath2(
  goal: () => Vec3,
  goalRadius: Input[Double] = new DoubleInput("goal radius", 0.7),
  maxSpeed: Input[Double] = new DoubleInput("max speed in", 2),
  walkThreshold: Input[Double] = new DoubleInput("walk threshold", .5),
  jumpThreshold: Input[Double] = new DoubleInput("jump threshold", .5),

  //    pathCalculator: Seq[Vec3] => Future[Path3D] = tryInOrder
  pathfindTimeout: Input[Double] = new DoubleInput("pathfind timeout (s)", 0.1))
  extends Node {
  
  guiComponent = goalRadius.guiComponent ++ maxSpeed.guiComponent ++
  walkThreshold.guiComponent ++ jumpThreshold.guiComponent
  
  // A sequence of coordinates leading from player to goal.
  var path: Seq[Vec3] = Seq()
  
  // This is used to keep track of a path calculation.  
  // When the calculation finishes, it resets this to None
  var pathCalculation: Option[Future[Path3D]] = None 
  
  /**
   * Set this to true in order to cancel an ongoing path calculation.
   */
  var pathCancelFlag: Option[AtomicBoolean] = None
  
  // Set to true when the calculation fails
  // Set to false when the calculation succeeds
  // It is used to determine whether Running or Failed is returned when 
  // we're waiting for a new path to be calculated.
  var lastPathFailed = false
  
  
  def calculateNewPath() {
    val cancelFlag = new AtomicBoolean(false)
    pathCancelFlag = Some(cancelFlag)
    
    pathCalculation = Some(pathFuture(goal(), pathfindTimeout.get seconds, cancelFlag))
    pathCalculation.map { pc =>
      pc.onFailure {
        case _ @ (_: NoPathException | _: TimeoutException) => {
          lastPathFailed = true
          
          pathCalculation = None
          pathCancelFlag = None
        }
        case e => {
          pathCalculation = None
          pathCancelFlag = None
          throw e
        }
      }
      
      pc.onSuccess {
        case newPath => {
          path = newPath.getSteps.map {step => step.toVec3()}
          
          lastPathFailed = false
          
          pathCalculation = None
          pathCancelFlag = None
        }
      }
      
      pc.onComplete {case _ => pathCalculation = None; pathCancelFlag = None}
      
    }
  }
  
  def isFinished = playerLoc.distanceTo(goal()) < goalRadius.get
  def stepIsCompleted = path.headOption map { playerLoc.distanceTo(_) < goalRadius.get } getOrElse false
  
  def update = Error
  
  override def update2() = {
    if (isFinished) {
      (Success, InputState())
      
    } else if (path == Seq()) {
      if (pathCalculation == None) calculateNewPath()
      
      if (lastPathFailed) (Failed, InputState())
      else (Running, InputState())
      
    } else {
      
      if (stepIsCompleted) {
        path = path.tail
      }
      
      (Running, BotHelper.walkWhileLooking(path.head, maxSpeed.get, walkThreshold.get, jumpThreshold.get))
    }
    
  }
  
  /**
   * Cancel the current path calculation.
   * Forget about the path, forget about whether the last path calculation failed.
   */
  def resetState {
    lastPathFailed = false
    path = Seq()
    pathCalculation = None
    pathCancelFlag map {_.set(true)}
    pathCancelFlag = None
  }
}