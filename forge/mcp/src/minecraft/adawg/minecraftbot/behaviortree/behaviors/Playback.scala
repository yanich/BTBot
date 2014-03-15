package adawg.minecraftbot.behaviortree.behaviors

import adawg.minecraftbot.behaviortree.Node
import adawg.minecraftbot.behaviortree.BehaviorStatus._
import adawg.minecraftbot.behaviortree.InputState
import adawg.minecraftbot.behaviortree.gui.Gui
import net.minecraft.client.Minecraft

/**
 * A frame of a recorded input sequence
 */
case class Frame(time: Long, input: InputState)

object Frame {
  def apply(time: Double, input: InputState) = new Frame(time.toLong, input)
}

/**
 * A behavior tree node that plays back a preset sequence of InputStates.
 * Returns Running until playback is finished, then returns Success.
 */
class Playback(recording: Seq[Frame]) extends Node {
  var framesRemaining: Seq[Frame] = recording
  var startTime: Option[Long] = None

  def timeSinceStart: Long = System.nanoTime - startTime.getOrElse {
    startTime = Some(System.nanoTime)
    startTime.get
  }
  
  def update = Error
  
  override def update2: NodeOutput = {
    framesRemaining match {
      case Nil => (Success, new InputState)
      case Frame(frameTime, inputState) :: rest =>
        if (timeSinceStart > frameTime) framesRemaining = rest
        (Running, inputState)
    }
  }
  
  def resetState = {
    framesRemaining = recording
    startTime = None
  }
}

object PlaybackTest {
   def dashTest = {
    // This should double tap the forward button, resulting in dashing
    val testRecord = Seq(
        Frame(0, new InputState),
        Frame(1e8, InputState(forward = Some(true))),
        Frame(2e8, InputState(forward = None)),
        Frame(1e9, InputState(forward = Some(true))),
        Frame(2e9, InputState(forward = None)))
    val playback = new Playback(testRecord)
    playback addGui Gui.actionButton("reset dashTest", e => playback.resetState())
    playback
  }
}