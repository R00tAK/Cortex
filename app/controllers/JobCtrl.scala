package controllers

import javax.inject.Inject

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }
import play.api.libs.json.{ JsString, Json }
import play.api.mvc.{ Action, AnyContent, Controller }
import models.JsonFormat._
import services.JobSrv

class JobCtrl @Inject() (
    jobSrv: JobSrv,
    implicit val ec: ExecutionContext) extends Controller {
  def list(dataTypeFilter: Option[String], dataFilter: Option[String], analyzerFilter: Option[String], start: Int, limit: Int): Action[AnyContent] = Action.async { request ⇒
    jobSrv.list(dataTypeFilter, dataFilter, analyzerFilter, start, limit).map {
      case (total, jobs) ⇒ Ok(Json.toJson(jobs)).withHeaders("X-Total" → total.toString)
    }
  }

  def get(jobId: String): Action[AnyContent] = Action.async { request ⇒
    jobSrv.get(jobId).map { job ⇒
      Ok(Json.toJson(job))
    }
  }

  def remove(jobId: String): Action[AnyContent] = Action.async { request ⇒
    jobSrv.remove(jobId).map(_ ⇒ Ok(""))
  }

  def report(jobId: String): Action[AnyContent] = Action.async { request ⇒
    jobSrv
      .get(jobId)
      .map { job ⇒
        val report = job.report.value match {
          case Some(Success(r))     ⇒ Json.toJson(r)
          case Some(Failure(error)) ⇒ JsString(error.getMessage)
          case None                 ⇒ JsString("Running")
        }
        Ok(jobWrites.writes(job) +
          ("status" → jobStatusWrites.writes(job.status)) +
          ("report" → report))
      }
  }

  def waitReport(jobId: String, atMost: String): Action[AnyContent] = Action.async { request ⇒
    jobSrv.waitReport(jobId, Duration(atMost))
      .map { job ⇒ Ok(Json.toJson(job)) }
  }
}