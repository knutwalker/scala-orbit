Scala orbit playground
======================

A Scala port of Uncle Bobs [clojureOrbit](https://github.com/unclebob/clojureOrbit) as he [presented it to the Chicago JUG](https://www.youtube.com/watch?v=SYeDxWKftfA).


This is mostly for fun, to see how certain idiomatic Clojure would translate to Scala.
Sometimes, a more Clojure-esque Scala is written (e.g. using stuff `->>` und `@`), and sometimes, it's the other way around (e.g. using `isCollide` instead of `collide?`).

## Usage

This project uses [`sbt`](http://www.scala-sbt.org/).

### run

```
./sbt run
```

### test

```
./sbt test
```

### build uberjar
```
./sbt assembly
```

### develop

Run this in one terminal

```
./sbt ~test
```

In another terminal, just enter the sbt shell

```
./sbt
```

And use the task `re-start` if you want to run the GUI.

With this setup you have:
1. a forked VM for every start, so you savely kill it without getting thrown out of the sbt shell
2. automatic test exectution on every file change. code -> save -> see tests fail (or pass)


## Differences from Uncle Bob

### Changes in code style

I tried to mirror Uncle Bobs code as closely as possible, that includes variable und function names with these exceptions (and possible some others):
- Where Clojure would use `make`, the Scala idiomatic naming is `apply`
- Predicates, that end in Clojure on `?`, e.g. `foo?` have the Scala (Java) naming of `isFoo`
- hyphen-case names are replaced by camelCase names, although it would be possible in Scala, to used hyphen-cased names as well

### Changes in domain types

- Used `Vec` instead of `vector`, `Pos` instead of `position`, and `Obj` instead of `object`
- Used value classes and type aliases for stuff like the age of an collision, the delay setting, the history of worlds, etc.
    Since Scala ist statically types, using small domain types like this can increase readability on the ability to reason about the code.
    On top, Scala makes it very easy to create such types and properly used value class have no runtime overhead, they exist only during compile time.


### Changes in implementation

#### less recur

I replaced some recursive loops with higher kinded loops from the Scala collection library.
For example, Uncle Bob writes:

```clojure
(defn accumulate-forces
  ([o world]
   (assoc o :force (accumulate-forces o world (vector/make))))
  ([o world f]
   (cond
     (empty? world) f
     (= o (first world)) (recur o (rest world) f)
     :else (recur o (rest world) (vector/add f (force-between o (first world)))))))
```

Here, I used a fold instead:

```scala
def accumulateForces(o: Obj, world: World): Obj = {
  val newForce = world.foldLeft(Vec()) { (f, obj) =>
    if (obj == o) f else Vec.add(f, forceBetween(o, obj)) }
  o.copy(force = newForce)
}
```

Another example from Uncle Bob

```clojure
(loop [world [sun] n 400]
  (if (zero? n)
    world
    (recur (conj world (random-object sun n)) (dec n))))
```

which I replaced with

```scala
sun +: Vector.tabulate(400)(n => randomObject(sun, 400 - n))
```

#### other stuff

- I did not check the current `controls`, if a `mouseup` is set to determine when to trigger the `handle-mouse` handler but used monadic comprehension for this.
    I always call `handleMouse`, but it would only execute the state changes, if both mouse events are set.
- For the history/trails, Uncle Bob used a `vector` and ~~adds~~ `conj`s new worlds to the end of it.
    I used a `List` instead and `cons` new world to the beginning.
    I could have used a `Vector` and appending as well, but prepending to Lists is such a typically found Scala usage, that I didn't wanted to discard it.
    There might be other places, where I used a List instead of a Vector.

### Changes in behavior

The general intention (entites) of the program did not change, but I modified some stuff on the UI plugin.
- For the trails, I actually just used a simple 'least x worlds' approach. Uncle Bob did something, that would delete some random trails
- Magnifying the UI does not center the view around the sun

### Additions

- Mapped the arrow keys, so that you can move around
 - press shift for faster pace
 - switched from `KeyEvent.getKeyCode` to `getKeyChar` for this
 - moving disabled the sun tracing, since that would always reset your viewport to the suns center
- Added a mapping to `r`, that would remove all but the current world, thus eliminating the trails
- Added command line options
 - `-sun` sets the initial mass of the sun (default: 1500)
 - `-count` sets the initial object count (default: 400)
- The color of an object gets less and less opaque, the more it gets to the tail end. So, the current objects have full opacity, the least recent object is almost fully transparent. This makes the trails fade out nicely.


## Conclusion

Clojure and Scala are not so different. This is mostly due to the functional nature of the program.
Of course you can have Scala and Clojure code that differ tremendously and especially Scala code, that is not functional at all.
But, if you know your way around functional programming, the difference between Scala and Clojure becomes more and more just syntax (and a bit of types). Using higher order functions, dealing with state and immutability, recursion etc. is all very similar (though, Scala __does__ have tail recursion)


### Atoms

[`atom`s](http://clojure.org/atoms) seam really nice. However, Scala does not have atoms, so I kinda had to [implement](src/main/scala/orbit/atom.scala#L5) them myself.

But since atoms are quite simple, this was an easy task. Given their [Clojure implementation](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Atom.java), I'm not that far off.

### doto

[`doto`](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/doto) is another nice thing for Java interop (especially with the [GUI stuff](https://github.com/unclebob/clojureOrbit/blob/master/src/orbit/world.clj#L235-L243)).
There is no direct Scala equivalent, but due to the way Scala handle blocks and imports, it can be [simulated very easy](/src/main/scala/orbit/package.scala#L238-L246):

```clojure
(doto (new java.util.HashMap) (.put "a" 1) (.put "b" 2))
```

becomes

```scala
val m = new java.util.HashMap

{ import m._
  put("a", 1)
  put("b", 2) }
```

Ok, not __really__ the same, but close enough.


### vec

There's some weird stuff happening with [`vec`](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/vec).
Or I don't quite understand it yet, whatever is more likely.
I choose to ignore this and instead stick with ye good olde Scala collections.


### Conciseness

Scala claims to allow you to write concise and expressive code. Clojure even more so.
There's even a [Twitter account](https://twitter.com/learnclojure) that teaches you Clojure in parts of 140 characters.

However, these are the (totally scientific) line counts.

Uncle Bobs CLojure code:

```
∵ cloc src/
      13 text files.
      13 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.04 s (347.6 files/s, 19040.0 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Clojure                         13            112              0            600
-------------------------------------------------------------------------------
SUM:                            13            112              0            600
-------------------------------------------------------------------------------
```

My Scala version:

```
∵ cloc src/
      13 text files.
      13 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.05 s (249.2 files/s, 13573.9 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Scala                           13            129              0            579
-------------------------------------------------------------------------------
SUM:                            13            129              0            579
-------------------------------------------------------------------------------
```

This includes source and test files and even my atom implementation.
If I count only the source files:

Clojure:

```
∵ cloc --not-match-f='.*(_test|-tests).clj' src/
       8 text files.
       8 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.03 s (229.3 files/s, 12641.9 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Clojure                          8             75              0            366
-------------------------------------------------------------------------------
SUM:                             8             75              0            366
-------------------------------------------------------------------------------
```

Scala:

```
∵ cloc --not-match-f=atom.scala  src/main/
       6 text files.
       6 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.03 s (179.2 files/s, 13557.9 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Scala                            6             82              0            372
-------------------------------------------------------------------------------
SUM:                             6             82              0            372
-------------------------------------------------------------------------------
```


And the counts for the relevant entities, sans the UI stuff


Clojure:

```
∵ cloc --not-match-f='.*(_test|-tests).clj' src/physics/
       5 text files.
       5 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.02 s (235.4 files/s, 8098.7 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Clojure                          5             36              0            136
-------------------------------------------------------------------------------
SUM:                             5             36              0            136
-------------------------------------------------------------------------------
```

Scala:

```
∵ cloc --not-match-f=atom.scala  src/main/scala/physics/
       4 text files.
       4 unique files.
       0 files ignored.

http://cloc.sourceforge.net v 1.60  T=0.02 s (242.1 files/s, 9077.8 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Scala                            4             29              0            121
-------------------------------------------------------------------------------
SUM:                             4             29              0            121
-------------------------------------------------------------------------------
```


So, Scala and Clojure appear to be quite similar in their conciseness
Now, just counting lines is a silly measure for the conciseness of the code, but it's a start. I'd suspect different numbers if I were to compare Scala against, say, Java ... as I said, totally scientific.


### Wrap up

Anyway, this was fun and I got to learn a bit Clojure on the way. Who knows, maybe, some day, I'll port some of my Scala stuff to Clojure :-)
