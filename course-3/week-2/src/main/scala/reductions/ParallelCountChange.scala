package reductions

import org.scalameter._
import common._

object ParallelCountChangeRunner {

  @volatile var seqResult = 0

  @volatile var parResult = 0

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 80,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val amount = 250
    val coins = List(1, 2, 5, 10, 20, 50)
    val seqtime = standardConfig measure {
      seqResult = ParallelCountChange.countChange(amount, coins)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential count time: $seqtime ms")

    def measureParallelCountChange(threshold: ParallelCountChange.Threshold): Unit = {
      val fjtime = standardConfig measure {
        parResult = ParallelCountChange.parCountChange(amount, coins, threshold)
      }
      println(s"parallel result = $parResult")
      println(s"parallel count time: $fjtime ms")
      println(s"speedup: ${seqtime / fjtime}")
    }

    measureParallelCountChange(ParallelCountChange.moneyThreshold(amount))
    measureParallelCountChange(ParallelCountChange.totalCoinsThreshold(coins.length))
    measureParallelCountChange(ParallelCountChange.combinedThreshold(amount, coins))
  }
}

object ParallelCountChange {

  def change(money: Int, coins: List[Int], available: List[Int]): Int = {
    money match {
      case 0 => 1
      case _ => coins.map {
        case c if c == money => 1
        case c if c < money => countChange(money - c, available.filter(_ >= c))
        case _ => 0
      }.sum
    }
  }
  /** Returns the number of ways change can be made from the specified list of
   *  coins for the specified amount of money.
   */
  def countChange(money: Int, coins: List[Int]): Int = {
    change(money, coins, coins)
  }

  type Threshold = (Int, List[Int]) => Boolean

  /** In parallel, counts the number of ways change can be made from the
   *  specified list of coins for the specified amount of money.
   */
  def parCountChange(money: Int, coins: List[Int], threshold: Threshold): Int = {
    def parChange(money: Int, coins: List[Int], available: List[Int], threshold: Threshold): Int = {
      (money, coins, threshold(money, coins)) match {
        case (0, _, _) => 1
        case (_, Nil, _) => 0
        case (_, _ :: Nil, _) => change(money, coins, available)
        case (_, _, true) => change(money, coins, available)
        case (_, _, _) => {
          val (p1, p2) = coins.splitAt(coins.length / 2)
          val (r1, r2) = parallel(
            parChange(money, p1, available, threshold),
            parChange(money, p2, available, threshold))
          r1 + r2
        }
      }
    }

    parChange(money, coins, coins, threshold)
  }

  /** Threshold heuristic based on the starting money. */
  def moneyThreshold(startingMoney: Int): Threshold =
    (money: Int, _: List[Int]) => money <= startingMoney * 2 / 3

  /** Threshold heuristic based on the total number of initial coins. */
  def totalCoinsThreshold(totalCoins: Int): Threshold =
    (_: Int, coins: List[Int]) => coins.length <= totalCoins * 2 / 3


  /** Threshold heuristic based on the starting money and the initial list of coins. */
  def combinedThreshold(startingMoney: Int, allCoins: List[Int]): Threshold = {
    (money: Int, coins: List[Int]) => money * coins.length <= startingMoney * allCoins.length / 2
  }
}
