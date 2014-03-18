package ru.radiant.anormacro

import scala.collection.mutable

trait PreLoading {
  def load(implicit cs: ConnSource) = {}
}

trait CachedTable[K, T] extends SimpleTable[T] with PreLoading {
  val map = mutable.HashMap[K, T]()

  def loadByFunc(keyFunction: (T)=>K)(implicit cs: ConnSource) = {
    all(cs).foreach(item => cache(keyFunction)(item) )
  }

  def cache(keyFunction: (T)=>K)(item: T): Option[T] = {
    map.put(keyFunction(item), item)
    Some(item)
  }

  def cache(dbFunction: (T)=>Option[T], keyFunction: (T)=>K)(item: T): Option[T] =
    cache(keyFunction)( dbFunction(item).get )

  def test  (dbFunction: (T)=>Option[T], keyFunction: (T)=>K)(search: T): Option[T] = {
    map.get(keyFunction(search)) match {
      case Some(c) => Some(c)
      case None => dbFunction(search)
    }
  }
}
