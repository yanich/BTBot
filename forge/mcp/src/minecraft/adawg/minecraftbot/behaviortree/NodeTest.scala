package adawg.minecraftbot.behaviortree
import BehaviorStatus._

import adawg.minecraftbot.BotImplicits._

object NodeTest {
  class Wait(timeToWait: Int) extends Node {
    var timeLeft = timeToWait
    def update = {
      timeLeft -= 1
      (if (timeLeft >= 0) Running else Success)
    }
    def resetState { timeLeft = timeToWait }
  }
}