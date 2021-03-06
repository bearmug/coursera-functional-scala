package recfun

import scala.annotation.tailrec

object Main {
  def main(args: Array[String]) {
    println("Pascal's Triangle")
    for (row <- 0 to 10) {
      for (col <- 0 to row)
        print(pascal(col, row) + " ")
      println()
    }
  }

  /**
    * Exercise 1
    */
  def pascal(c: Int, r: Int): Int = {
    if (c == 0 || c == r) 1
    else pascal(c - 1, r - 1) + pascal(c, r - 1)
  }

  /**
    * Exercise 2
    */
  def balance(chars: List[Char]): Boolean = {
    @tailrec
    def balanceLoop(stack: Int, chars: List[Char]): Boolean = (stack, chars) match {
      case (s, _) if s < 0 => false
      case (_, c) if chars.isEmpty => stack == 0
      case _ => {
        chars match {
          case '(' :: _ => balanceLoop(stack + 1, chars.tail)
          case ')' :: _ => balanceLoop(stack - 1, chars.tail)
          case _ => balanceLoop(stack, chars.tail)
        }
      }
    }
    balanceLoop(0, chars)
  }

  /**
    * Exercise 3
    */
  def countChange(money: Int, coins: List[Int]): Int = {

    def change(money: Int, coins: List[Int], minCoin: Int): Int = {
      coins.map {
        case c if c < minCoin => 0
        case c if c == money => 1
        case c if c < money => change(money - c, coins, c)
        case _ => 0
      }.sum
    }

    change(money, coins, 0)
  }
}
