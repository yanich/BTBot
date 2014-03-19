package com.kunii.mcbot.bots

import adawg.minecraftbot.BTBot
import adawg.minecraftbot.behaviortree._
import adawg.minecraftbot.behaviortree.behaviors._
import adawg.minecraftbot.behaviortree.decorators.DecoratorImplicits._

class BTHuntAndPickup extends BTBot {

  def mkRootNode = PriorityNode(
      HuntCreatures() withToggle "hunt mobs",
      PickUpItems() withToggle "pick up items")
      
  def botName = "Hunt mobs and pickup items"
}