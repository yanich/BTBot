package adawg.minecraftbot.behaviortree

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InputStateSuite extends FunSuite {

  test ("Merge two simple InputStates"){
    val t = new InputState (
      forward = Some(true)
    )
    val t2 = new InputState (
      forward = Some(false),
      back = Some(true)
    )
    assert(InputState.mergeOver(t, t2) equals new InputState (forward = Some(true), back = Some(true)))
    
  }
}