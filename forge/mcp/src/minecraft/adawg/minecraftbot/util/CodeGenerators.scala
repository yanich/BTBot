package adawg.minecraftbot.util

object CodeGenerators {

  val ControlInputPrinterStrings = {
    val labels = Seq("back", "forward", "left", "right", "jump", "sneak", 
        "mouse1", "mouse2", "mouse3")
    val toggleNames = labels map {_ + "Toggle"}
   val output1 = for (label <- labels) yield "val " + label + "Toggle = new scala.swing.CheckBox(\"" + label + "\")"
   val output2 = for (label <- labels) yield label + "Toggle.selected = input." + label + " getOrElse false"
   
   def appendWithComma(list: String, item: String) = list + ", " + item
   val toggleList = toggleNames.foldLeft("")(appendWithComma)
   val output3 = "guiComponent = Seq(" + toggleList + ")"
   
  output1 ++ output2 ++ Seq(output3)
  }
}