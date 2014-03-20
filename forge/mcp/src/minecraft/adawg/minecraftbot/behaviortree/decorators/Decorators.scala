package adawg.minecraftbot.behaviortree.decorators
import adawg.minecraftbot.behaviortree.BehaviorStatus._
import com.google.common.util.concurrent.RateLimiter
import scala.swing.Label
import adawg.minecraftbot.behaviortree.gui._
import adawg.minecraftbot.behaviortree.gui.Gui._
import adawg.minecraftbot.behaviortree.BehaviorStatus
import adawg.minecraftbot.behaviortree.Node
import scala.swing.Dimension
import scala.swing.TextField
import scala.swing.event.EditDone
import adawg.minecraftbot.behaviortree.ParallelNode
import adawg.minecraftbot.behaviortree.ConditionNode
import adawg.minecraftbot.behaviortree.InputState

object Decorators {

}

class ResetIfStatusMatches(statuses: BehaviorStatus*)(child: Node) extends Node {
  override val children = Seq(child)
  def update = {
    val status = child.update
    status match {
      case x if statuses.contains(x) => {
        println("resetting child: " + child)
        child.resetState
        x
      }
      case x => x
    }
  }
  def resetState = child.resetState
}

object DecoratorImplicits {
  implicit def nodeToDecoratedNode(n: Node) = new DecoratedNode(n)
}

class DecoratedNode(child: Node) {
  /**
   * Child fails unless cond is true.
   */
  def withCondition(cond: => Boolean) = ParallelNode(true, false)(
    ConditionNode(cond),
    child)
    
  /**
   * Only runs the child every <period> seconds.  Fails otherwise.
   */
  def everyNSeconds(
      defaultPeriod: Double = 1, label: String = "Time between ticks")(
      periodIn: DoubleInput = new DoubleInput(label, defaultPeriod)) = {
    var lastTime = 0L
    def checkTime: Boolean = {
      val currentTime = System.nanoTime()
      if ((currentTime - lastTime) > periodIn.get * 1e9) { lastTime = currentTime; true
      } else false
    }
    val node = withCondition(checkTime)
    node.guiComponent ++= periodIn.guiComponent
    node
  }
  
  def succeedIf(cond: => Boolean) = new OverrideWithStatusIf(Success, cond, child)
  def withBenchmark = new PrintTickTime(child)
  def withBenchmark(label: => String) = new PrintTickTime(child, label)
  
  def withToggle(label: String = "") = new GuiToggle(child, label)
  def withToggle = new GuiToggle(child)
  
  def withDebouncer(period: Double, hasGui: Boolean = false) = {
    val periodIn = if (hasGui) new DoubleInput("", period) else new HiddenInput(period)
    new Debouncer(child, periodIn)
  }
  def withStatusPrinter(label: => String) = new StatusPrinter(child, label)
  def withInputDisplay(label: => String) = new ControlInputPrinter(child, label)
  
  def withActionButton(label: String, action: => Unit) = {
    child.addGui(actionButton(label, _ => action))
    child
  }
  
  def resetOnStatus(statuses: BehaviorStatus*) = 
    new ReactOnStatuses(child, statuses.toSet, () => child.resetState)
  
  def onSuccess(reaction: () => Unit) = new ReactOnStatuses(child, Set(Success), reaction)
  def onFailure(reaction: () => Unit) = new ReactOnStatuses(child, Set(Failed), reaction)
}

/**
 * Untested.  Supposed to run reaction() when child returns certain statuses.
 */
class ReactOnStatuses(child: Node, statuses: Set[BehaviorStatus], reaction: () => Unit) extends Node {
  def update = update2._1
  def resetState = child.resetState
  override val children = Seq(child)
  
  override def update2 = {
    val (status, input) = child.update2()
    if (statuses contains status) reaction()
    (status, input)
  }
}

class ControlInputPrinter(child: Node, label: String) extends Node {
  def update = update2._1
  def resetState = child.resetState
  override val children = Seq(child)

  // Generated code (see adawg.minecraftbot.util.CodeGenerators.scala
  val backToggle = new scala.swing.CheckBox("back")
  val forwardToggle = new scala.swing.CheckBox("forward")
  val leftToggle = new scala.swing.CheckBox("left")
  val rightToggle = new scala.swing.CheckBox("right")
  val jumpToggle = new scala.swing.CheckBox("jump")
  val sneakToggle = new scala.swing.CheckBox("sneak")
  val mouse1Toggle = new scala.swing.CheckBox("mouse1")
  val mouse2Toggle = new scala.swing.CheckBox("mouse2")
  val mouse3Toggle = new scala.swing.CheckBox("mouse3")
  
  val yawField = new scala.swing.TextField("Yaw") {
    editable = false
  }
  val pitchField = new scala.swing.TextField("Pitch") {
    editable = false
  }
  
  val displayElements = Seq(backToggle, forwardToggle, leftToggle, rightToggle, jumpToggle, sneakToggle, 
      mouse1Toggle, mouse2Toggle, mouse3Toggle,
      yawField, pitchField)

//  guiComponent = Seq(new Label(label), backToggle, forwardToggle, leftToggle, rightToggle, jumpToggle, sneakToggle, mouse1Toggle, mouse2Toggle, mouse3Toggle)
      guiComponent = Seq(new Label(label)) ++ displayElements
      
  val numbers = Seq("lookYaw", "lookPitch")
  override def update2 = {
    val (status, input) = child.update2
//    Swing.onEDT {
      
      backToggle.selected = input.back getOrElse false
      forwardToggle.selected = input.forward getOrElse false
      leftToggle.selected = input.left getOrElse false
      rightToggle.selected = input.right getOrElse false
      jumpToggle.selected = input.jump getOrElse false
      sneakToggle.selected = input.sneak getOrElse false
      mouse1Toggle.selected = input.mouse1 getOrElse false
      mouse2Toggle.selected = input.mouse2 getOrElse false
      mouse3Toggle.selected = input.mouse3 getOrElse false
      yawField.text = input.mouseYaw map { _.toString } getOrElse "None"
      pitchField.text = input.mousePitch map { _.toString } getOrElse "None"
//    }
    
    (status, input)
  }
}

class StatusPrinter(child: Node, label: String) extends Node {
  def update = update2._1
  def resetState = child.resetState; lastStatus = None
  
  override val children = Seq(child)
  var lastStatus: Option[BehaviorStatus] = None

  guiComponent = Seq(Gui.vertBox(new Label(label), textField))
  
  import swing._
  lazy val textField =
    new TextField("n/a", 5) {
      maximumSize = new Dimension(200, 100)
      text = "n/a"
      //	  text.tail.forall(_.isDigit) &&
      //		(text.head.isDigit || text.head.equals('-'))
      inputVerifier = { _ => true }
//      editable = false
//      shouldYieldFocus = _ => true
      listenTo(keys, this)
      import scala.swing.event.EditDone
      reactions += {
        case e: EditDone =>
          text = lastStatus map { a => a.toString } getOrElse "n/a"
      }
    }
  override def update2: NodeOutput = {
    val (status, input) = child.update2
    lastStatus = Some(status)
//    Swing.onEDT { textField.text = status.toString }
    textField.text = status.toString
    (status, input)
  }
}

// Child fails if and only if it continuously fails for <period> seconds.
class Debouncer(child: Node, 
    periodIn: Input[Double] = new DoubleInput("Debounce period", 0.5)) extends Node {
  def update = update2._1
  def resetState = {
    child.resetState
    lastFailureTime = 0
    lastNonFailureTime = 0
  }
  override val children = Seq(child)

  guiComponent = periodIn.guiComponent
  //    gui.addComponent(this)
  def periodNanos = periodIn.get * 1e9

  var lastNonFailureTime = 0L
  var lastFailureTime = 0L

  override def update2: NodeOutput = {
    val currentTime = System.nanoTime
    val timeSinceLastNonfail = currentTime - lastNonFailureTime
    val timeSinceLastFail = currentTime - lastFailureTime

    val (status, input) = child.update2

    val output = if (status != Failed) {
      (status, input)
    } else {
      if (timeSinceLastFail > 1e8 // 1/10th of second elapsed since last tick => Not continuously failing
          || timeSinceLastNonfail < periodNanos) (Running, input)
      else (Failed, input)
    }

    if (status == Failed)
      lastFailureTime = currentTime
    else
      lastNonFailureTime = currentTime

    output
  }
}

class PrintTickTime(child: Node, label: => String, frequency: Int) extends Node {
  def this(child: Node) = this(child, child.toString, 5)
  def this(child: Node, label: => String) = this(child, label, 5)
  override val children = Seq(child)
  lazy val timer = RateLimiter.create(frequency)
  def update = update2._1
  override def update2: NodeOutput = {
    val startTime = System.nanoTime()
    val status = child.update2()
    val duration = System.nanoTime - startTime
    if (timer.tryAcquire()) println(child + " took " + duration + " nanoseconds to update")
    status
  }
  def resetState = child.resetState()
}

class OverrideWithStatusIf(status: => BehaviorStatus, cond: => Boolean, child: Node) extends Node {
  def update = if (cond) status else child.update()
  override def update2() = {
    val (childStatus, input) = child.update2()
    if (cond) (status, input) else (childStatus, input)
  }
  def resetState = child.resetState()
}

class WithResetButton(child: Node) extends Node {
  guiComponent = Seq apply resetButton(child)
  def update = child.update
  def resetState = child.resetState
}
object WithResetButton {
  def apply(child: Node) = new WithResetButton(child)
}

class GuiToggle(child: Node, label: String = "run/stop node") extends Node {
  override val children = Seq(child)
  lazy val gui = radioButtonGroup(
    Map("run" -> { (_) => running = true },
      "stop" -> { (_) => running = false }))

  guiComponent = Seq(Gui.vertBox(new Label(label), gui))
  var running = true

  def update = update2._1
  
  override def update2: NodeOutput = {
    if (running) child.update2() else (Failed, InputState())
  }
  
  def resetState = child.resetState; gui.reset; running = true
}

object GuiToggle {
  def apply(child: Node) = new GuiToggle(child)
}