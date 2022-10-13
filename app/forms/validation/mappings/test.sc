val list = List(1,2,3,4)

list.flatMap{
  case value if (value / 2 == 0) => List(value)
  case value if (value / 2 != 0) => List(value + 2)
}

def testmatch(i : Int) = {
  i match {
    case value if (value / 2 == 0) => List(value)
    case value if (value / 2 != 0) => List(value + 2)
  }
}

def testMatchWithAdditionalCheck(i : Int) = {
  i match {
    case value if (value / 2 == 0) => List(value)
    case value if (value / 2 != 0) => List(value + 2)
    case value if (value / 3 != 0) => List(value + 2)
  }
}
val bool = true
list.flatMap{
  case x if bool == true => testmatch(x)
  case x if bool == false => testMatchWithAdditionalCheck(x)
}