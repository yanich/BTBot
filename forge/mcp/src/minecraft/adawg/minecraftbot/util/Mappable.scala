// This is supposed to let you access case class fields like it was a map.
// Requires macros.  I think I have to get a newer version of scala.

//package adawg.minecraftbot.util
//import scala.reflect.macros.Context
//import scala.language.experimental.macros
//import scala.language.experimental.macros._
//trait Mappable[T] {
//  def toMap(t: T): Map[String, Any]
//  def fromMap(map: Map[String, Any]): T
//}
//
//object Mappable {
//  implicit def materializeMappable[T]: Mappable[T] = macro materializeMappableImpl[T]
//
//  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Mappable[T]] = {
//    import c.universe._
//    val tpe = weakTypeOf[T]
//    val companion = tpe.typeSymbol.companionSymbol
//
//    val fields = tpe.declarations.collectFirst {
//      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
//    }.get.paramss.head
//
//    val (toMapParams, fromMapParams) = fields.map { field ⇒
//      val name = field.name
//      val decoded = name.decoded
//      val returnType = tpe.declaration(name).typeSignature
//
//      (q"$decoded → t.$name", q"map($decoded).asInstanceOf[$returnType]")
//    }.unzip
//
//    c.Expr[Mappable[T]] { q"""
//      new Mappable[$tpe] {
//        def toMap(t: $tpe): Map[String, Any] = Map(..$toMapParams)
//        def fromMap(map: Map[String, Any]): $tpe = $companion(..$fromMapParams)
//      }
//    """ }
//  }
//}