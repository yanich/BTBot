package adawg.minecraftbot.behaviortree
import BehaviorStatus._

object worksheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  def fail = ActionNode(Failed)                   //> fail: => adawg.minecraftbot.behaviortree.ActionNode
  val condition1 = ConditionNode(5 > 2)           //> condition1  : adawg.minecraftbot.behaviortree.ConditionNode = adawg.minecraf
                                                  //| tbot.behaviortree.ConditionNode@157d2287
  val condition2 = new ConditionNode(3 > 5)       //> condition2  : adawg.minecraftbot.behaviortree.ConditionNode = adawg.minecraf
                                                  //| tbot.behaviortree.ConditionNode@644db282
  val action = ActionNode({ println("action is performed"); Success })
                                                  //> action  : adawg.minecraftbot.behaviortree.ActionNode = adawg.minecraftbot.be
                                                  //| haviortree.ActionNode$$anon$1@65a45a7f
  val subsequence = SequenceNode(Wait(3), action) //> subsequence  : adawg.minecraftbot.behaviortree.SequenceNode = adawg.minecraf
                                                  //| tbot.behaviortree.SequenceNode@10b090ac
  
  val sequence = new SequenceNode(subsequence, action)
                                                  //> sequence  : adawg.minecraftbot.behaviortree.SequenceNode = adawg.minecraftbo
                                                  //| t.behaviortree.SequenceNode@37ea14c3
  def updateN(node: Node, times: Int) {
    for (i <- 1 to times) println(i + ": " + node.update)
  }                                               //> updateN: (node: adawg.minecraftbot.behaviortree.Node, times: Int)Unit
  
  List(5, 7, 3, 1).sortWith(_ < _)                //> res0: List[Int] = List(1, 3, 5, 7)
  
}