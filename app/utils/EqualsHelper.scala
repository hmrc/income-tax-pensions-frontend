package utils

object EqualsHelper {

  def isDifferent[A](left: A, right: Option[A]): Boolean = {
    Option(left) == right
  }

}
