package parseback.benchmarks

import org.openjdk.jmh.annotations._

import scala.util.parsing.{combinator => spc}

@State(Scope.Thread)
@Fork(2)
@Measurement(iterations = 10)
@Warmup(iterations = 10)
@Threads(1)
class ArithmeticBenchmarks {

  @Param(Array("2", "4", "8", "16", "32", "64", "128"/*, "512", "2048"*/))
  var size: Int = _

  val parseback = {
    import _root_.parseback._

    lazy val expr: Parser[Int] = (
        expr ~ "+" ~ term ^^ { (_, e, _, t) => e + t }
      | expr ~ "-" ~ term ^^ { (_, e, _, t) => e - t }
      | term
    )

    lazy val term: Parser[Int] = (
        term ~ "*" ~ factor ^^ { (_, e, _, f) => e * f }
      | term ~ "/" ~ factor ^^ { (_, e, _, f) => e / f }
      | factor
    )

    lazy val factor: Parser[Int] = (
        "(" ~> expr <~ ")"
      | "-" ~ factor       ^^ { (_, _, e) => -e }
      | """\d+""".r        ^^ { (_, str) => str.toInt }
    )

    expr
  }

  val gll = {
    import com.codecommit.gll.RegexParsers._

    lazy val expr: Parser[Int] = (
        expr ~ "+" ~ term ^^ { (e, _, t) => e + t }
      | expr ~ "-" ~ term ^^ { (e, _, t) => e - t }
      | term
    )

    lazy val term: Parser[Int] = (
        term ~ "*" ~ factor ^^ { (e, _, f) => e * f }
      | term ~ "/" ~ factor ^^ { (e, _, f) => e / f }
      | factor
    )

    lazy val factor: Parser[Int] = (
        "(" ~> expr <~ ")"
      | "-" ~ factor       ^^ { (_, e) => -e }
      | """\d+""".r        ^^ { _.toInt }
    )

    expr
  }

  object ScalaParserCombinator extends spc.RegexParsers with spc.PackratParsers {

    lazy val expr: PackratParser[Int] = (
        expr ~ "+" ~ term ^^ { case e ~ _ ~ t => e + t }
      | expr ~ "-" ~ term ^^ { case e ~ _ ~ t => e - t }
      | term
    )

    lazy val term: PackratParser[Int] = (
        term ~ "*" ~ factor ^^ { case e ~ _ ~ f => e * f }
      | term ~ "/" ~ factor ^^ { case e ~ _ ~ f => e / f }
      | factor
    )

    lazy val factor: PackratParser[Int] = (
        "(" ~> expr <~ ")"
      | "-" ~ factor       ^^ { case _ ~ e => -e }
      | """\d+""".r        ^^ { _.toInt }
    )

    def run(input: String) = parseAll(expr, input)
  }

  val sample: Map[Int, String] = {
    val operators = Map(
      0 -> "+",
      1 -> "-",
      2 -> "*",
      3 -> "/")

    def inner(size: Int) = {
      0 until size map { i =>
        val neg = if (i % 7 == 0)
          "-"
        else
          ""

        neg + i.toString + operators(i % 4)
      } drop 1 mkString
    }

    val sizes = List(2, 4, 8, 16, 32, 64, 128)

    sizes.map({ i => i -> inner(i) })(collection.breakOut)
  }

  @Benchmark
  def parsebackRun(): Unit = {
    import _root_.parseback.LineStream
    import _root_.parseback.compat.cats._

    import cats.Eval

    val stream = LineStream[Eval](sample(size))
    parseback(stream).value
  }

  @Benchmark
  def gllRun(): Unit = {
    import com.codecommit.gll.LineStream

    val stream = LineStream(sample(size))
    gll(stream)
  }

  @Benchmark
  def spcRun(): Unit = {
    ScalaParserCombinator.run(sample(size))
  }
}
