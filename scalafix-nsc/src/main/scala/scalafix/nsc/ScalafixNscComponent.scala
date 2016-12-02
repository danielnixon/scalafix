package scalafix.nsc

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.Fixed
import scalafix.ScalafixConfig
import scalafix.rewrite.Rewrite
import scalafix.util.FileOps
import scalafix.util.logger

class ScalafixNscComponent(plugin: Plugin, val global: Global)
    extends PluginComponent
    with ReflectToolkit
    with NscSemanticApi {
  override val phaseName: String = "scalafix"
  override val runsAfter: List[String] = "typer" :: Nil
  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def name: String = "scalafix"
    override def run(): Unit = {
      global.currentRun.units.foreach { unit =>
        if (unit.source.file.exists &&
            unit.source.file.file.isFile &&
            !unit.isJava) {
          // TODO(olafur) pull out rewrite rules from configuration flags.
          val rewrites = Rewrite.semanticRewrites
          fix(unit, ScalafixConfig(rewrites = rewrites)) match {
            case Fixed.Success(fixed) =>
              FileOps.writeFile(unit.source.file.file, fixed)
            case Fixed.Failed(e) =>
              g.reporter.warning(unit.body.pos,
                                 "Failed to run scalafix. " + e.getMessage)
          }
        }
      }
    }
  }
}