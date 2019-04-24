package arrow.benchmarks

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.effects.IO
import arrow.effects.suspended.fx.Fx
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
open class MapStream {

  @Benchmark
  fun fxOne(): Long = Fx.unsafeRunBlocking(FxStream.test(12000, 1))

  @Benchmark
  fun fx30(): Long = Fx.unsafeRunBlocking(FxStream.test(1000, 30))

  @Benchmark
  fun fx120(): Long = Fx.unsafeRunBlocking(FxStream.test(100, 120))

  @Benchmark
  fun ioOne(): Long = IOStream.test(12000, 1).unsafeRunSync()

  @Benchmark
  fun io30(): Long = IOStream.test(1000, 30).unsafeRunSync()

  @Benchmark
  fun io120(): Long = IOStream.test(100, 120).unsafeRunSync()

  @Benchmark
  fun zioOne(): Long =
    arrow.benchmarks.effects.scala.zio.`MapStream$`.`MODULE$`.test(12000, 1)

  @Benchmark
  fun zioBatch30(): Long =
    arrow.benchmarks.effects.scala.zio.`MapStream$`.`MODULE$`.test(12000 / 30, 30)

  @Benchmark
  fun zioBatch120(): Long =
    arrow.benchmarks.effects.scala.zio.`MapStream$`.`MODULE$`.test(12000 / 120, 120)

  @Benchmark
  fun catsOne(): Long =
    arrow.benchmarks.effects.scala.cats.`MapStream$`.`MODULE$`.test(12000, 1)

  @Benchmark
  fun catsBatch30(): Long =
    arrow.benchmarks.effects.scala.cats.`MapStream$`.`MODULE$`.test(12000 / 30, 30)

  @Benchmark
  fun catsBatch120(): Long =
    arrow.benchmarks.effects.scala.cats.`MapStream$`.`MODULE$`.test(12000 / 120, 120)

}

object FxStream {
  class Stream(val value: Int, val next: Fx<Option<Stream>>)
  val addOne: (Int) -> Int = { it + 1 }

  fun test(times: Int, batchSize: Int): Fx<Long> {
    var stream = range(0, times)
    var i = 0
    while (i < batchSize) {
      stream = mapStream(addOne)(stream)
      i += 1
    }

    return sum(0)(stream)
  }

  private fun range(from: Int, until: Int): Option<Stream> =
    if (from < until) Some(Stream(from, Fx.lazy { range(from + 1, until) }))
    else None

  private fun mapStream(f: (Int) -> Int): (box: Option<Stream>) -> Option<Stream> = { box ->
    when (box) {
      is Some -> box.copy(Stream(f(box.t.value), box.t.next.map(mapStream(f))))
      None -> None
    }
  }

  private fun sum(acc: Long): (Option<Stream>) -> Fx<Long> = { box ->
    when (box) {
      is Some -> box.t.next.flatMap(sum(acc + box.t.value))
      None -> Fx.just(acc)
    }
  }
}

object IOStream {
  class Stream(val value: Int, val next: IO<Option<Stream>>)
  val addOne: (Int) -> Int = { it + 1 }

  fun test(times: Int, batchSize: Int): IO<Long> {
    var stream = range(0, times)
    var i = 0
    while (i < batchSize) {
      stream = mapStream(addOne)(stream)
      i += 1
    }

    return sum(0)(stream)
  }

  private fun range(from: Int, until: Int): Option<Stream> =
    if (from < until) Some(Stream(from, IO { range(from + 1, until) }))
    else None

  private fun mapStream(f: (Int) -> Int): (box: Option<Stream>) -> Option<Stream> = { box ->
    when (box) {
      is Some -> box.copy(Stream(f(box.t.value), box.t.next.map(mapStream(f))))
      None -> None
    }
  }

  private fun sum(acc: Long): (Option<Stream>) -> IO<Long> = { box ->
    when (box) {
      is Some -> box.t.next.flatMap(sum(acc + box.t.value))
      None -> IO.just(acc)
    }
  }
}
