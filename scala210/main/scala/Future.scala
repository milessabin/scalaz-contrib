package scalaz.contrib
package std

import scalaz._

import scala.concurrent.{Future, ExecutionContext, CanAwait, Await}
import scala.concurrent.duration.Duration

trait FutureInstances1 {

  implicit def futureInstance(implicit ec: ExecutionContext): Monad[Future] with Cobind[Future] with Cojoin[Future] with Each[Future] = 
    new FutureInstance

  implicit def futureSemigroup[A](implicit m: Semigroup[A], ec: ExecutionContext): Semigroup[Future[A]] =
    Monoid.liftSemigroup[Future, A]

}

trait FutureInstances extends FutureInstances1 {

  /**
   * Requires explicit usage as the use of `Await.result`. Can throw an exception, which is inherently bad.
   */
  def futureComonad(duration: Duration)(implicit executionContext: ExecutionContext): Comonad[Future] = new FutureInstance with Comonad[Future] {
    def copoint[A](f: Future[A]): A = Await.result(f, duration)
  }

  implicit def futureMonoid[A](implicit g: Monoid[A], ec: ExecutionContext): Monoid[Future[A]] =
    Monoid.liftMonoid[Future, A]

}

private class FutureInstance(implicit ec: ExecutionContext) extends Monad[Future] with Cobind[Future] with Cojoin[Future] with Each[Future] {

  def point[A](a: => A): Future[A] = Future(a)
  def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa flatMap f
  override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa map f
  def cobind[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future(f(fa))
  def cojoin[A](a: Future[A]): Future[Future[A]] = Future(a)
  def each[A](fa: Future[A])(f: A => Unit) = fa foreach f

  // override for actual parallel execution
  override def ap[A, B](_fa: => Future[A])(_fab: => Future[A => B]) = {
    val fa = _fa
    val fab = _fab
    for { a <- fa; ab <- fab } yield ab(a)
  }

}

object scalaFuture extends FutureInstances

// vim: expandtab:ts=2:sw=2
