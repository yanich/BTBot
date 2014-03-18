package com.kunii.mcbot.bots

import adawg.minecraftbot.BTBot
import adawg.minecraftbot.behaviortree._
import adawg.minecraftbot.behaviortree.behaviors._

class BTHuntAndPickup extends BTBot {

  def mkRootNode = PriorityNode(HuntCreatures(), PickUpItems())
  def botName = "Hunt mobs and pickup items"
}