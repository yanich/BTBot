package adawg.minecraftbot

import com.kunii.mcbot.bots.CoreBot
import adawg.minecraftbot.behaviortree.gui.BehaviorTreeWindow
import adawg.minecraftbot.behaviortree.Node
import adawg.minecraftbot.behaviortree.InputState
import adawg.minecraftbot.behaviortree.behaviors.FollowPath

abstract class BTBot extends CoreBot {
  def mkRootNode: Node
	
  lazy val rootNode = mkRootNode
  lazy val gui = new BehaviorTreeWindow()
  var running: Boolean = false
  def inGameLoop(x: net.minecraft.client.Minecraft): Int = {

    val (status, controlInput) = rootNode.update2()
    InputState.sendInput(controlInput)
    0
  }
  
  def inGameRenderer(x: net.minecraft.client.gui.FontRenderer): Unit = {}
  def init(): Unit = {}
  def locksMinecraft(): Boolean = false
    
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
    FollowPath.resetPathfinder()
  }
  def onStop(): Unit = {
    Swing.onEDT {gui.visible = false}
  }
}