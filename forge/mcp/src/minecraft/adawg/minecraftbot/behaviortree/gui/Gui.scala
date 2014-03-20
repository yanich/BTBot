package adawg.minecraftbot.behaviortree.gui

import swing._
import scala.swing.event.MouseClicked
import adawg.minecraftbot.behaviortree.BehaviorStatus
import adawg.minecraftbot.behaviortree.decorators.GuiToggle
import scala.swing.event.EditDone
import net.minecraft.util.Vec3
import scala.Array.fallbackCanBuildFrom
import adawg.minecraftbot.behaviortree.Node

class BehaviorTreeWindow(guiComponents: Traversable[GuiComponent]*) extends MainFrame {
  val t = guiComponents.flatten
  val panel =
    new BoxPanel(Orientation.Horizontal) {
      //    new GridPanel(2, 4) {
      //    hGap = 20
      //    vGap = 20
      minimumSize = new Dimension(500, 500)
      preferredSize = new Dimension(500, 500)
      //    minimumSize = new Dimension(250, 250)
      // WEIRD!  If you don't add anything to the contents here, no components will ever show.
      contents += new Label("Behavior tree ")
      for (node <- t) {
        node.guiComponent map {
          contents += _
        }
      }
    }
  contents = panel
  def addComponent(gc: GuiComponent) = {
    for (c <- gc.guiComponent) Swing.onEDT {
      panel.contents += c
      panel.revalidate
      panel.repaint
    }
  }
  def addComponent(c: Component) = Swing.onEDT {
    panel.contents += c
    panel.revalidate()
    panel.repaint()
  }
}

/**
 * Inherited by Nodes and by GUI inputs.  Add nodes to the main window by traversing the root node.
 */
trait GuiComponent {
  var guiComponent: Seq[Component] = Seq()
  def addGui(c: Seq[Component]) {
    guiComponent = guiComponent ++ c
  }
  def addGui(c: Component): Unit = addGui(Seq(c))
  def addGui(c: GuiComponent): Unit = addGui(c.guiComponent)
}

abstract class Input[T](name: String, default: T) extends GuiComponent {
  protected var value: T = default
  def get: T = value
  def set(t: T) = {
    value = t
  }
}

class HiddenInput[T](name: String = "HiddenInput", input: => T) extends FunInput(name, input) {
  def this(input: => T) = this("hidden input", input)
  guiComponent = Seq()
}

class FunInput[T](name: String = "FunInput", input: => T) extends Input(name, input) {
  def this(input: => T) = this("FunInput", input)
  override def get: T = {
    val value = input
    textField.text = value.toString
    value
  }
  override def set(t: T) = throw new Exception("Calling set() on a FunInput is pointless")
  
  lazy val textField = new TextField
  guiComponent = Seq(Gui.vertBox(new Label(name), textField))
}

class BoolInput(name: String, default: Boolean, trueLabel: String = "on", falseLabel: String = "off", callback: (Boolean => Unit) = { i => })
  extends Input(name, default) {
  lazy val gui = Gui.radioButtonGroup(if (default) elements else elements.reverse)

  val elements = List[(String, MouseClicked => Unit)](
    trueLabel -> { (_) => value = true },
    falseLabel -> { (_) => value = false })

  guiComponent =
    Seq(Gui.vertBox(new Label(name), gui))
}

class IntInput(name: String, default: Int, callback: (Int => Unit) = { i => })
  extends Input(name, default) {
  lazy val textField: TextField =
    new TextField(default.toString, 5) {
      maximumSize = new Dimension(200, 100)
      text = default.toString
      def myV(v: Component): Boolean = text.tail.forall(_.isDigit) &&
        (text.head.isDigit || text.head.equals('-'))
      inputVerifier = myV _
      listenTo(keys, this)
      reactions += {
        //      case e: KeyTyped => {
        //        val loc = e.location
        //    	if (!e.char.isDigit)
        //    	  e.consume 
        //      }
        case e: EditDone =>
          try {
            set(text.toInt)
            callback(get)
            println(value)
          } catch {
            case e: NumberFormatException => text = get.toString
          }
      }
    }
  
  override def set(i: Int) {
    value = i
    textField.text = i.toString
  }
  guiComponent =
    Seq(Gui.vertBox(new Label(name), textField))
}

class DoubleInput(name: String, default: Double, callback: (Double => Unit) = { i => })
  extends Input(name, default) {
  lazy val textField: TextField =
    new TextField(default.toString, 5) {
      maximumSize = new Dimension(200, 100)
      text = default.toString
      def myV(v: Component): Boolean = {
        def throwsNumberFormatException(f: => Any): Boolean = {
          try { f; false } catch { case e: NumberFormatException => true }
        }
        !throwsNumberFormatException(text.toDouble)
      }
      //	  text.tail.forall(_.isDigit) &&
      //		(text.head.isDigit || text.head.equals('-'))
      inputVerifier = myV _
      listenTo(keys, this)
      reactions += {
        //      case e: KeyTyped => {
        //        val loc = e.location
        //    	if (!e.char.isDigit)
        //    	  e.consume 
        //      }
        case e: EditDone =>
          try {
            set(text.toDouble)
            callback(get)
            println(value)
          } catch {
            case e: NumberFormatException => text = get.toString
          }
      }
    }

  guiComponent =
    Seq(Gui.vertBox(new Label(name), textField))
}

class Vec3Input(name: String, default: Vec3) extends Input(name, default) {
  def callback(i: Int): Unit = set(Vec3.createVectorHelper(xIn.get, yIn.get, zIn.get))
  override def set(v: Vec3) {
    xIn.set(v.xCoord.toInt)
    yIn.set(v.yCoord.toInt)
    zIn.set(v.zCoord.toInt)
    value = v
  }
  lazy val xIn = new IntInput("x", default.xCoord.toInt, callback)
  lazy val yIn = new IntInput("y", default.yCoord.toInt, callback)
  lazy val zIn = new IntInput("z", default.zCoord.toInt, callback)
  guiComponent =
    Seq(new Label(name)) ++ xIn.guiComponent ++ yIn.guiComponent ++ zIn.guiComponent
}

package object Gui {
  def resetButton(child: Node) =
    actionButton("reset " + child, _ => child.resetState)

  def vertBox(components: Component*) = {
    val panel = new BoxPanel(Orientation.Vertical)
    panel.contents ++= components
    panel
  }

  def actionButton(name: String, callback: MouseClicked => Unit) = new Button(name) {
    listenTo(mouse.clicks)
    reactions += {
      case e: MouseClicked => callback(e)
    }
  }

  def radioButton(name: String, callback: MouseClicked => Unit) = new RadioButton(name) {
    listenTo(mouse.clicks)
    reactions += {
      case e: MouseClicked => callback(e)
    }
  }

  def radioButtonGroup(buttonDefs: Iterable[(String, MouseClicked => Unit)]) = {
    val buttons = buttonDefs map { case (name, callback) => radioButton(name, callback) }
    val group = new ButtonGroup
    group.buttons ++= buttons.toTraversable
    buttons.headOption foreach { group.select(_) }
    new BoxPanel(Orientation.Horizontal) {
      contents ++= buttons
      def reset = buttons.headOption foreach { group.select(_) }
    }
  }
}

object GuiTest {
  import adawg.minecraftbot.behaviortree.ConditionNode
  def main(args: Array[String]) = {
    val node = new GuiToggle(ConditionNode { false })
    val gui = new BehaviorTreeWindow(node)
    gui.addComponent(new IntInput("test", -4))
    gui.addComponent(new IntInput("test2", -5))

    val test = new BoxPanel(Orientation.Vertical) {
      //      minimumSize = new Dimension(500, 500)
      //      preferredSize = new Dimension(500, 500)
      //      contents += new Label("above")
      //      contents += new Label("below")
    }
    test.contents += new Label("above")
    test.contents += new IntInput("test input", 42).textField
    test.contents += new Label("below")
    gui.addComponent(test)

    gui.visible = true
    node.resetState
    //	  MyFirstGUI.main(args)
    import adawg.minecraftbot.util.CodeGenerators
    for (i <- CodeGenerators.ControlInputPrinterStrings) println(i)

  }
  val behaviorStatusRadios: Seq[Component] =
    for (status <- BehaviorStatus.values) yield new RadioButton(status.toString())
}