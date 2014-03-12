package adawg.minecraftbot.behaviortree

package object actions {
//
//  /**
//   * Functional version of steerUsingForce.  Returns InputState to steer towards target ground velocity.
//   */
//  def steerUsingForce2(targetVelocity: Vec3): InputState = {
//    // Break down current velocity and target velocity into forwards and rightwards components
//    val curFwdVel = playerVelocity.dotProduct(forwardsVector)
//    val curSideVel = playerVelocity.dotProduct(rightVector)
//    val tarFwdVel = targetVelocity.dotProduct(forwardsVector)
//    val tarSideVel = targetVelocity.dotProduct(rightVector)
//    val fwdSteering = tarFwdVel - curFwdVel
//    val sideSteering = tarSideVel - curSideVel
//
//    val threshold = 0.02
//    val forward, back, right, left = false
//    if (targetVelocity.lengthVector() > 0) {
//      InputState(
//        forward = Some(fwdSteering > threshold),
//        back = Some(fwdSteering < -threshold),
//        right = Some(sideSteering > threshold),
//        left = Some(sideSteering < -threshold))
//    } else new InputState
//  }
//  
//  def walkWithoutLooking(goal: Vec3): InputState = {
//    steerUsingForce2(seekVelocity(goal)).copy(jump = Some(player.posY < goal.yCoord))
//  }
//  
//  
//  def walkWhileLooking(goal: Vec3, maxSpeed: Double, moveThreshold: Double, jumpThreshold: Double = 0.1): InputState = {
//    val desiredVelocity = goal.subtract(playerLoc).normalize().multiply(maxSpeed)
//    val steering = playerVelocity.subtract(desiredVelocity)
//    val horizontalComponent = Math.sqrt(steering.xCoord * steering.xCoord + steering.zCoord * steering.zCoord)
//    
//    // Dot product between steering and the vector pointing from player to goal.
//    // If this is negative, it's telling us to steer backwards
//    val dotProduct = steering.dotProduct(goal.subtract(playerLoc))
//    
//    
//    val walkInput = if (horizontalComponent < moveThreshold) new InputState
//    else if (dotProduct > 0) InputState(forward = Some(true))
//    else InputState(back = Some(true))
//    
//    val lookInput = lookAtYaw(goal)
//    
//    lookInput.mergeOver(walkInput).copy(jump = Some(player.posY + jumpThreshold < goal.yCoord))
//  }
//
//  def lookAt(goal: Vec3): InputState = {
//    val xDist = goal.xCoord - player.posX;
//    val yDist = player.posY + player.getEyeHeight() - goal.yCoord;
//    val zDist = goal.zCoord - player.posZ;
//
//    val yaw = (Math.atan2(zDist, xDist) * 180.0D / Math.PI) - 90F;
//    val pitch = (Math.atan2(yDist, Math.sqrt(xDist * xDist + zDist * zDist)) * 180.0D / Math.PI);
//    InputState(
//      mouseYaw = Some(yaw),
//      mousePitch = Some(pitch))
//  }
//  def lookAtYaw(goal: Vec3): InputState = lookAt(goal).copy(mousePitch = None)
}