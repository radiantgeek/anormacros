package ru.radiant.anormacro.conf

import ru.radiant.anormacro._

object Defaults {

  implicit val connectionSource: ConnSource = new PlayConnection

}
