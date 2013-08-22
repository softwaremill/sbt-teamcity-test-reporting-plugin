package com.gu

import sbt._
import sbt.testing.{Event => TEvent, Status => TStatus}

import Keys._
import java.io.{PrintWriter, StringWriter}

object TeamCityTestReporting extends Plugin {
  override def settings = Seq(
    testListeners ++= TeamCityTestListener.ifRunningUnderTeamCity
  )
}

class TeamCityTestListener extends TestReportListener {
  /** called for each class or equivalent grouping */
  def startGroup(name: String) {
    // we can't report to teamcity that a test group has started here,
    // because even if parallel test execution is disabled there may be multiple
    // projects running tests from different projects at the same time.
    // So if you tell TC that a test group has started, the tests from
    // different projects will get mixed up.
  }

  def nicelyFormatException(t: Throwable) = {
    val w = new StringWriter
    val p = new PrintWriter(w)
    t.printStackTrace(p)
    w.toString
  }

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent) {
    for (e: TEvent <- event.detail) {
          
      if (e.selector.isInstanceOf[sbt.testing.TestSelector]) {
        val selector = e.selector.asInstanceOf[sbt.testing.TestSelector]
        // TC seems to get a bit upset if you start a test while one is already running
        // so a nasty bit of synchronisation here to stop that happening
        synchronized {
          // this is a lie: the test has already been executed and started by this point,
          // but sbt doesn't send an event when test starts
          teamcityReport("testStarted", "name" -> selector.testName)

          e.status match {
            case TStatus.Success => // nothing extra to report
            case TStatus.Error | TStatus.Failure =>
              teamcityReport("testFailed",
                "name" -> selector.testName,
                "details" -> nicelyFormatException(e.throwable().get)
              )
            case TStatus.Skipped | TStatus.Ignored =>
              teamcityReport("testIgnored", "name" -> selector.testName)
          }
          teamcityReport("testFinished", "name" -> selector.testName)
        }
      }
    }
  }


  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable) { }
  /** called if test completed */
  def endGroup(name: String, result: TestResult.Value) { }


  // http://confluence.jetbrains.net/display/TCD65/Build+Script+Interaction+with+TeamCity
  def tidy(s: String) = s
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("\u0085", "|x")
    .replace("\u2028", "|l")
    .replace("\u2029", "|p")
    .replace("[", "|[")
    .replace("]", "|]")

  private def teamcityReport(messageName: String, attributes: (String, String)*) {
    println("##teamcity[" + messageName + " " + attributes.map {
      case (k, v) => k + "='" + tidy(v) + "'"
    }.mkString(" ") + "]")
  }
}

object TeamCityTestListener {
  // teamcity se
  private lazy val teamCityProjectName = Option(System.getenv("TEAMCITY_PROJECT_NAME"))
  lazy val ifRunningUnderTeamCity = teamCityProjectName.map(ignore => new TeamCityTestListener).toSeq
}



