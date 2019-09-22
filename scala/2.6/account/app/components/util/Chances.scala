package components.util

import scala.util.Random

object Chances {

  def everyTime = new EveryTime

  def random(probability: Int, divisor: Int): Chance = new RandomChance(probability, divisor)

  private[Chances] class EveryTime extends Chance {
    override def yes: Boolean = true
  }

  private[Chances] class RandomChance(probability: Int, divisor: Int = 100) extends Chance {
    override def yes: Boolean = probability <= new Random().nextInt(divisor) + 1
  }
}
