package orbit

import java.util.concurrent.atomic.AtomicReference

object atom {

  type Atom[T] = AtomicReference[T]

  implicit final class RichAtom[T](val underlying: AtomicReference[T]) extends AnyVal {
    def `@`: T = underlying.get()
    def reset(v: T) = underlying.set(v)
    def swap(fun: T => T) {
      def loop(): Boolean = {
        val prev = underlying.get()
        val next = fun(prev)
        underlying.compareAndSet(prev, next) || loop()
      }
      loop()
    }
    def foreach(fun: T => Unit) = fun(underlying.get())
  }

  def Atom[T](item: T): Atom[T] = new AtomicReference(item)
}
