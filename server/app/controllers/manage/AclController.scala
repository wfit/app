package controllers.manage

import akka.Done
import base.UserAction
import javax.inject.{Inject, Singleton}
import models._
import play.api.mvc.{InjectedController, Result}
import scala.concurrent.{ExecutionContext, Future}
import services.AuthService
import utils.UUID
import utils.SlickAPI._
import utils.FutureOps

@Singleton
class AclController @Inject()(userAction: UserAction, authService: AuthService)
                             (implicit executionContext: ExecutionContext)
	extends InjectedController {

	private def enumerateGroupMembers(group: UUID): Future[Seq[UUID]] = {
		Users.filter { u =>
			val explicitly = AclMemberships.filter(m => m.group === group && m.user === u.uuid).exists
			val implicitly = AclGroups.filter(g => g.uuid === group && g.forumGroup === u.group).exists
			explicitly || implicitly
		}.map(_.uuid).run
	}

	private def flushMultipleCaches(users: Seq[UUID]): Future[Done] = {
		Future.traverse(users)(authService.flushUserAcl).replaceSuccess(Done)
	}

	private def withGroupFlush(group: UUID)(action: => Future[Result]): Future[Result] = {
		enumerateGroupMembers(group).flatMap { members =>
			action andThenAsync flushMultipleCaches(members)
		}
	}

	def users = userAction.authenticated.async { implicit req =>
		Users.sortBy(_.name).run.map(users => Ok(views.html.admin.aclUsers(users)))
	}

	def user(uuid: UUID) = userAction.authenticated.async { implicit req =>
		for {
			user <- Users.filter(_.uuid === uuid).head
			acl <- authService.loadAcl(uuid)
			groups <- AclGroups.sortBy(_.title).map { group =>
				val explicitly = AclMemberships.filter(m => group.uuid === m.group && m.user === uuid).exists
				val implicitly = group.forumGroup === user.group
				(group, (explicitly || implicitly) getOrElse false)
			}.run
		} yield Ok(views.html.admin.aclUser(user, acl, groups))
	}

	def userInvite(user: UUID) = userAction.authenticated(parse.json).async { implicit req =>
		val entry = AclMembership(user, req.param("group").asUUID)
		val action = (AclMemberships += entry) andThen Redirect(routes.AclController.user(user))
		action.run andThenAsync authService.flushUserAcl(user)
	}

	def userKick(user: UUID, group: UUID, backToGroup: Boolean) = userAction.authenticated.async { implicit req =>
		val action = AclMemberships.filter(m => m.user === user && m.group === group).delete andThen Redirect(
			if (backToGroup) routes.AclController.group(group)
			else routes.AclController.user(user)
		)
		action.run andThenAsync authService.flushUserAcl(user)
	}

	def groups = userAction.authenticated.async { implicit req =>
		val groups = AclGroups.sortBy(_.title).run
		val forumGroups = sql"SELECT group_id, group_name FROM milbb_groups WHERE group_id > 7".as[(Int, String)].run
		for (gs <- groups; fgs <- forumGroups) yield Ok(views.html.admin.aclGroups(gs, fgs.toMap))
	}

	def createGroup = userAction.authenticated(parse.json).async { implicit req =>
		val title = req.param("title").validate[String](_.nonEmpty, "Le nom du groupe doit être défini.")
		val forumGroup = req.param("forumGroup").asOpt[Int].filter(_ > 0)
		(AclGroups += AclGroup(UUID.random, title, forumGroup)) andThen Redirect(routes.AclController.groups())
	}

	def group(uuid: UUID) = userAction.authenticated.async { implicit req =>
		for {
			grp <- AclGroups.filter(_.uuid === uuid).head
			ks <- AclKeys.sortBy(_.key).run
			gs <- AclGroupGrants.filter(_.subject === uuid).sortBy(_.key).run
			membersQuery = grp.forumGroup match {
				case Some(id) => Users.filter(_.group === id)
				case None => Users.filter(u => AclMemberships.filter(_.user === u.uuid).exists)
			}
			ms <- membersQuery.sortBy(_.name).run
		} yield {
			val keyset = ks.map(k => (k.id, k)).toMap
			val fullGrants = gs.map(g => (g, keyset(g.key).key))
			Ok(views.html.admin.aclGroup(grp, ks, fullGrants, ms))
		}
	}

	def groupGrant(uuid: UUID) = userAction.authenticated(parse.json).async { implicit req =>
		withGroupFlush(uuid) {
			val grant = AclGroupGrant(uuid, req.param("key").asUUID, req.param("value").asInt, req.param("negate").asBoolean)
			(AclGroupGrants insertOrUpdate grant) andThen Redirect(routes.AclController.group(uuid))
		}
	}

	def groupRevoke(uuid: UUID, key: UUID) = userAction.authenticated.async { implicit req =>
		withGroupFlush(uuid) {
			AclGroupGrants.filter(g => g.subject === uuid && g.key === key).delete andThen Redirect(routes.AclController.group(uuid))
		}
	}

	def deleteGroup(uuid: UUID) = userAction.authenticated.async { implicit req =>
		withGroupFlush(uuid) {
			AclGroups.filter(_.uuid === uuid).delete andThen Redirect(routes.AclController.groups())
		}
	}

	def keys = userAction.authenticated.async { implicit req =>
		AclKeys.sortBy(_.key).run.map(keys => Ok(views.html.admin.aclKeys(keys)))
	}

	def createKey = userAction.authenticated(parse.json).async { implicit req =>
		val key = req.param("key").validate[String](_.matches("^[a-z\\.]{3,}$"), "Nom de permission invalide.")
		val entry = AclKey(UUID.random, key, req.param("desc").asString)
		(AclKeys insertOrUpdate entry) andThen Redirect(routes.AclController.keys())
	}

	def deleteKey(id: UUID) = userAction.authenticated.async { implicit req =>
		AclKeys.filter(_.id === id).delete andThen Redirect(routes.AclController.keys())
	}
}
