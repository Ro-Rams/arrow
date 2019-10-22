package arrow.extreme

// metadebug

import kotlin.reflect.KProperty

class IO<A>(val value: A) {

  operator fun getValue(value: Any?, property: KProperty<*>): A = TODO()

  fun <B> flatMap(f: (A) -> IO<B>): IO<B> =
    f(value)

  companion object {
    fun <A> fx(f: IO.Companion.() -> A): IO<A> = TODO()
    fun <A> just(a: A): IO<A> = IO(a)
  }
}

fun test1(): IO<Int> =
  IO.fx {
    val a: Int by IO(1)
    val b: Int by IO(2)
    a + b
  }

fun test2() = // full on inference
  IO.fx {
    val a by IO(1)
    val b by IO(2)
    a + b
  }

fun test3(): IO<Int> = // same vals name
  IO.fx {
    val a by IO.fx {
      val a by IO(1)
      val b by IO(2)
      a + b
    }
    val b by IO.fx {
      val a by IO(3)
      val b by IO(4)
      a + b
    }
    a + b
  }

fun test3Should(): IO<Int> =
  IO(1).flatMap { a ->
    IO(2).flatMap { b ->
      IO.just(a + b)
    }
  }.flatMap { a ->
    IO(3).flatMap { a ->
      IO(4).flatMap { b ->
        IO.just(a + b)
      }
    }.flatMap { b ->
      IO.just(a + b)
    }
  }

fun test4(): IO<Int> = // mixed properties and expressions
  IO.fx {
    val a by IO(1)
    val t = a + 1
    val b by IO(2)
    val y = a + b
    val f by IO(3)
    val n = a + 1
    val g by IO(4)
    y + f + g + t + n
  }

fun test5(): IO<Int> = // simple TODO Should this fail at compile time due to lack of bindings to enforce efficiency recommending just or just lift the pure value?
  IO.fx { 1 + 1 }
