/*
 * Copyright 2008-2011 Trung Dinh
 *
 *  This file is part of Doom.
 *
 *  Doom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Doom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Doom.  If not, see <http://www.gnu.org/licenses/>.
 */
import ninthdrug.command.Command
import ninthdrug.util.Util
import java.net.InetAddress

package object doom {
  lazy val HOME = System.getProperty("user.home")
  lazy val HOST = InetAddress.getLocalHost.getCanonicalHostName
  lazy val HOSTNAME = InetAddress.getLocalHost.getHostName.split('.')(0)
  lazy val DOOMHOME = sys.env("DOOMHOME")

  val Config = ninthdrug.config.Config

  val Block = ninthdrug.command.Block
  val Chain = ninthdrug.command.Chain
  val Echo = ninthdrug.command.Echo
  val EditFile = ninthdrug.command.EditFile
  val EditXmlFile = ninthdrug.command.EditXmlFile
  val Exec = ninthdrug.command.Exec
  val Exist = ninthdrug.command.Exist
  val False = ninthdrug.command.False
  val Fork = ninthdrug.command.Fork
  val Get = ninthdrug.command.Get
  val If = ninthdrug.command.If
  val Install = ninthdrug.command.Install
  val Kill = ninthdrug.command.Kill
  val KillAll = ninthdrug.command.KillAll
  val Lazarus = ninthdrug.command.Lazarus
  val Not = ninthdrug.command.Not
  val NullCommand = ninthdrug.command.NullCommand
  val Print = ninthdrug.command.Print
  val Put = ninthdrug.command.Put
  val Result = ninthdrug.command.Result
  val Return = ninthdrug.command.Return
  val Rez = ninthdrug.command.Rez
  val Sh = ninthdrug.command.Sh
  val Sleep = ninthdrug.command.Sleep
  val Ssh = ninthdrug.command.Ssh
  val True = ninthdrug.command.True
  val Tunnel = ninthdrug.command.Tunnel
  val Write = ninthdrug.command.Write

  type Result = ninthdrug.command.Result
  type Command = ninthdrug.command.Command

  def exec(command: String): Result = Command.exec(command)
  def sh(command: String) = Command.sh(command)
  def shout(command: String) = Command.shout(command)
  def shlines(command: String) = Command.shlines(command)
  def psgrep(pattern: String) = Command.psgrep(pattern)
  def read(filename: String) = Command.read(filename)
  def readlines(filename: String) = Command.readlines(filename)
  def write(filename: String, text: String) = Command.write(filename, text)
  def writelines(filename: String, lines: List[String]) =
    Command.writelines(filename, lines)
  def kill_process_tree(ppid: Int, delay: Int = 180) = 
    Command.kill_process_tree(ppid, delay)
  def kill(pid: Int, delay: Int = 180) = 
    Command.kill(pid, delay)
  def interpolate(string: String, map: Map[String,String]) =
    Util.interpolate(string, map)
  def prompt(args: Array[String], n: Int, name: String): String =
    Command.prompt(args.toSeq, n, name)

  def remote(command: Command, host: String): Command =
    DoomAPI.remote(command, host)
}
