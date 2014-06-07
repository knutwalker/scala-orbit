package orbit

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

object atom {

  type Atom[T] = AtomicReference[T]

  private def fun2unop[T](fun: T => T): UnaryOperator[T] = new UnaryOperator[T] {
    def apply(t: T): T = fun(t)
  }

  implicit final class RichAtom[T](val underlying: AtomicReference[T]) extends AnyVal {
    def alter(fun: T => T): Unit = {
      underlying.updateAndGet(fun2unop(fun))
    }
  }

  implicit final class Any2Atom[T](val item: T) extends AnyVal {
    def `@` = new AtomicReference[T](item)
  }
}
