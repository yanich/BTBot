package adawg.minecraftbot.behaviortree
import com.kunii.mcbot.McBot
import net.minecraft.client.Minecraft
import com.kunii.mcbot.Camera

case class InputState(
    mousePitch: Option[Double] = None, 
    mouseYaw: Option[Double] = None, 
    right: Option[Boolean] = None, 
    left: Option[Boolean] = None, 
    back: Option[Boolean] = None, 
    forward: Option[Boolean] = None, 
    jump: Option[Boolean] = None, 
    sneak: Option[Boolean] = None, 
    drop: Option[Boolean] = None, 
    mouse1: Option[Boolean] = None, 
    mouse2: Option[Boolean] = None, 
    mouse3: Option[Boolean] = None) {
  def mergeOver(other: InputState) = InputState.mergeOver(this, other)
}

object InputState {
  
  /**
   * Merges two InputStates.
   * Inputs from top take priority over inputs from bottom.
   */
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
    
  /**
   * Sets McBot variables and Minecraft keyBinds to the values contained in is.
   * Also uses McBot's Camera.setYaw and setPitch to move the camera if needed.
   * 
   * Assumes false for inputs that are not specified (i.e. that equal None)
   * @param is the InputState to apply
   */
  def sendInput(is: InputState) {
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
  
  /**
   * Sets McBot variables and Minecraft keyBinds to the values contained in is.
   * Also uses McBot's Camera.setYaw and setPitch to move the camera if needed.
   * 
   * Does nothing for inputs that are not specified.
   * @param is the InputState to apply
   */
  def sendInputLax(is: InputState) {
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
    is.mouseYaw map {yaw => Camera.setYaw(yaw.toInt)}
    is.mousePitch map {pitch => Camera.setPitch(pitch.toInt)}
  }
  
  /**
   * Returns the current physical keyboard inputs, plus the player's yaw and 
   * pitch, as an InputState.
   */
  def currentPlayerInput: InputState = new InputState(
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
}



