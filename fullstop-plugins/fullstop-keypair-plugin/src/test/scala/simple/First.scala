import org.scalatest.{BeforeAndAfter, FunSuite}

class First extends FunSuite with BeforeAndAfter {

  val one: Int = 1
  var two: Int = _

  before {
    // do something before, maybe initialize 'two'
    two = 2
  }

  test("Some description of the test to make it easier to understand the code") {
    // 2 should be greater than 1
    assert(two > one)
  }
}