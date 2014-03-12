package adawg.minecraftbot.behaviortree
import com.kunii.mcbot.McBot
import net.minecraft.client.Minecraft
import com.kunii.mcbot.Camera

case class InputState(mousePitch: Option[Double] = None, mouseYaw: Option[Double] = None, right: Option[Boolean] = None, left: Option[Boolean] = None, back: Option[Boolean] = None, forward: Option[Boolean] = None, jump: Option[Boolean] = None, sneak: Option[Boolean] = None, drop: Option[Boolean] = None, mouse1: Option[Boolean] = None, mouse2: Option[Boolean] = None, mouse3: Option[Boolean] = None) {
  def mergeOver(other: InputState) = InputState.mergeOver(this, other)
}
/**
 *  Everything here is a var because I don't want to write ": Option[Boolean] = None" 10 times
 *  in the constructor.  So do the anonymous class thing, 
 *  i.e. new InputState { forward = Some(true) }
 */ 
//class InputState2 {
//  var right, left, back, forward, jump, sneak, drop, mouse1, mouse2, mouse3: Option[Boolean] = None
//  var mousePitch, mouseYaw: Option[Double] = None
//}
object InputState {
  def mergeOver(top: InputState, bottom: InputState): InputState = new InputState (
    right = top.right.orElse(bottom.right),
    left = top.left.orElse(bottom.left),
    back = top.back.orElse(bottom.back),
    forward = top.forward.orElse(bottom.forward),
    jump = top.jump.orElse(bottom.jump),
    sneak = top.sneak.orElse(bottom.sneak),
    drop = top.drop.orElse(bottom.drop),
    mouse1 = top.mouse1.orElse(bottom.mouse1),
    mouse2 = top.mouse2.orElse(bottom.mouse2),
    mouse3 = top.mouse3.orElse(bottom.mouse3),
    mousePitch = top.mousePitch.orElse(bottom.mousePitch),
    mouseYaw = top.mouseYaw.orElse(bottom.mouseYaw)
    )
//    mouseAngleDelta = top.mouseAngleDelta.orElse(bottom.mouseAngleDelta)
    
  
  
  // Empty inputs are assumed to be false.
  def sendInput(is: InputState) = {
    McBot.isMovingBack = is.back getOrElse false
    McBot.isMovingLeft = is.left getOrElse false
    McBot.isMoving = is.forward getOrElse false
    McBot.isMovingRight = is.right getOrElse false
    McBot.shouldSneak = is.sneak getOrElse false
    McBot.shouldJump = is.jump getOrElse false
    Minecraft.getMinecraft().gameSettings.keyBindDrop.pressed = is.drop getOrElse false
    Minecraft.getMinecraft().gameSettings.keyBindAttack.pressed = is.mouse1 getOrElse false
    Minecraft.getMinecraft().gameSettings.keyBindUseItem.pressed = is.mouse2 getOrElse false
    Minecraft.getMinecraft().gameSettings.keyBindPickBlock.pressed = is.mouse3 getOrElse false
    is.mouseYaw map {yaw => Camera.setYaw(yaw.toInt)}
    is.mousePitch map {pitch => Camera.setPitch(pitch.toInt)}
  }
  
  // Empty inputs are not changed.
  def sendInputLax(is: InputState) = {
    is.back map { McBot.isMovingBack = _}
    is.left.map { McBot.isMovingLeft = _}
    is.right.map {McBot.isMovingRight = _}
    is.forward.map { McBot.isMoving = _}
    is.sneak.map { McBot.shouldSneak = _}
    is.jump.map { McBot.shouldJump = _}
    is.drop.map { Minecraft.getMinecraft().gameSettings.keyBindDrop.pressed = _}
    is.mouse1.map { Minecraft.getMinecraft().gameSettings.keyBindAttack.pressed = _}
    is.mouse2.map { Minecraft.getMinecraft().gameSettings.keyBindUseItem.pressed = _}
    is.mouse3.map { Minecraft.getMinecraft().gameSettings.keyBindPickBlock.pressed = _}
  }
}

/**
 * A frame of a recorded input sequence
 */
case class Frame(time: Long, input: InputState) {
  def this(time: Double, input: InputState) = this(time.toLong, input)
}
object Frame {
  def apply(time: Double, input: InputState) = new Frame(time.toLong, input)
}

import adawg.minecraftbot.behaviortree.BehaviorStatus._

class Playback(recording: List[Frame]) extends Node {
  var framesRemaining: List[Frame] = recording
  var startTime: Option[Long] = None
  
  def setRecording(record: List[Frame]) {
    framesRemaining = record
    startTime = None
  }

  def relativeTime: Long = System.nanoTime - startTime.getOrElse {
    startTime = Some(System.nanoTime)
    startTime.get
  }
  def update = Error
  override def update2: NodeOutput = {
    framesRemaining match {
      case Nil => (Success, new InputState)
      case Frame(time, input) :: rest =>
        if (relativeTime > time) framesRemaining = rest
        (Running, input)
    }
  }
  def resetState = {
    framesRemaining = recording
    startTime = None
  }
}

class Record(fps: Int, callback: List[Frame] => Unit) extends Node {
  def captureInput: InputState = new InputState(
      drop = Some(Minecraft.getMinecraft().gameSettings.keyBindDrop.pressed),
      back = Some(Minecraft.getMinecraft().gameSettings.keyBindBack.pressed),
      right = Some(Minecraft.getMinecraft().gameSettings.keyBindRight.pressed),
      left = Some(Minecraft.getMinecraft().gameSettings.keyBindLeft.pressed),
      forward = Some(Minecraft.getMinecraft().gameSettings.keyBindForward.pressed),
      jump = Some(Minecraft.getMinecraft().gameSettings.keyBindJump.pressed),
      sneak = Some(Minecraft.getMinecraft().gameSettings.keyBindSneak.pressed),
      mouse1 = Some(Minecraft.getMinecraft().gameSettings.keyBindAttack.pressed),
      mouse2 = Some(Minecraft.getMinecraft().gameSettings.keyBindUseItem.pressed),
      mouse3 = Some(Minecraft.getMinecraft().gameSettings.keyBindPickBlock.pressed),
      mouseYaw = Some(Minecraft.getMinecraft().thePlayer.rotationYaw),
      mousePitch = Some(Minecraft.getMinecraft().thePlayer.rotationPitch)
      )
  var recording: List[Frame] = Nil
  var startTime: Option[Long] = None
  def lastTime: Long = recording.headOption.map{ _.time} getOrElse 0
  def relativeTime = System.nanoTime - startTime.getOrElse {
    startTime = Some(System.nanoTime)
    startTime.get
  }
  
  // Send the recording to callback listeners.  Delete it and start fresh
  def endRecording() {
    callback(recording)
    recording = Nil
  }
  
  override def update2 = {
    if (System.nanoTime - lastTime > 1e9 / fps) {
      recording = Frame(relativeTime, captureInput) :: recording
    }
    (Running, new InputState)
  }
  
  def update = update2._1
  def resetState = { 
    recording = Nil
  }
}

//abstract class ControlInput {
//}
//case class Right(pressed: Boolean) extends ControlInput
//case class Left(pressed: Boolean) extends ControlInput
//case class Back(pressed: Boolean) extends ControlInput
//case class Forward(pressed: Boolean) extends ControlInput
//case class Jump(pressed: Boolean) extends ControlInput
//case class Sneak(pressed: Boolean) extends ControlInput
//case class Mouse1(pressed: Boolean) extends ControlInput
//case class Mouse2(pressed: Boolean) extends ControlInput
//case class Mouse3(pressed: Boolean) extends ControlInput
//type InputState2 = List[ControlInput]
//def mergeOver(top: InputState2, bottom: InputState2) = {
//	for (p <- top)
//	  true
//	
//}
//
//object InputState2 {
//  val t: InputState2 = List(Right(true))
//  
//}



