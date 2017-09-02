package controllers.manage

import akka.Done
import base.{CheckAcl, UserAction}
import javax.inject.{Inject, Singleton}
import models._
import models.acl.{AclGroup, AclGroupGrant, AclKey, AclMembership}
import play.api.cache.AsyncCacheApi
import play.api.mvc.{InjectedController, Result}
import scala.concurrent.{ExecutionContext, Future}
import services.AuthService
import utils.UUID
import utils.SlickAPI._
import utils.FutureOps

@Singleton
class AclController @Inject()(userAction: UserAction, checkAcl: CheckAcl, authService: AuthService)
                             (cache: AsyncCacheApi)
                             (implicit executionContext: ExecutionContext)
	extends InjectedController {

	private def aclAction = userAction.authenticated andThen checkAcl("admin.acl")

	private def enumerateGroupMembers(group: UUID): Future[Seq[UUID]] = {
		Users.filter { u =>
			val explicitly = AclMemberships.filter(m => m.group === group && m.user === u.uuid).exists
			val implicitly = AclGroups.filter(g => g.uuid === group && g.forumGroup === u.group).exists
			explicitly || implicitly
		}.map(_.uuid).result
	}

	private def flushMultipleCaches(users: Seq[UUID]): Future[Done] = {
		Future.traverse(users)(authService.flushUserAcl).replaceSuccess(Done)
	}

	private def withGroupFlush(group: UUID)(action: => Future[Result]): Future[Result] = {
		enumerateGroupMembers(group).flatMap { members =>
			action andThenAsync flushMultipleCaches(members)
		}
	}

	def users = aclAction.async { implicit req =>
		Users.sortBy(_.name).result.map(users => Ok(views.html.admin.aclUsers(users)))
	}

	def user(uuid: UUID) = aclAction.async { implicit req =>
		for {
			user <- Users.filter(_.uuid === uuid).result.head.run
			acl <- authService.loadAcl(uuid)
			groups <- AclGroups.sortBy(_.title).map { group =>
				val explicitly = AclMemberships.filter(m => group.uuid === m.group && m.user === uuid).exists
				val implicitly = group.forumGroup === user.group
				(group, (explicitly || implicitly) getOrElse false)
			}.result
		} yield Ok(views.html.admin.aclUser(user, acl, groups))
	}

	def userInvite(user: UUID) = aclAction(parse.json).async { implicit req =>
		val entry = AclMembership(user, req.param("group").asUUID)
		val action = (AclMemberships += entry) andThen Redirect(routes.AclController.user(user))
		action.run andThenAsync authService.flushUserAcl(user)
	}

	def userKick(user: UUID, group: UUID, backToGroup: Boolean) = aclAction.async { implicit req =>
		val action = AclMemberships.filter(m => m.user === user && m.group === group).delete andThen Redirect(
			if (backToGroup) routes.AclController.group(group)
			else routes.AclController.user(user)
		)
		action.run andThenAsync authService.flushUserAcl(user)
	}

	def groups = aclAction.async { implicit req =>
		for {
			groups <- AclGroups.sortBy(_.title).result
			forumGroups <- sql"SELECT group_id, group_name FROM milbb_groups WHERE group_id > 7".as[(Int, String)]
		} yield Ok(views.html.admin.aclGroups(groups, forumGroups.toMap))
	}

	def createGroup = aclAction(parse.json).async { implicit req =>
		val title = req.param("title").validate[String](_.nonEmpty, "Le nom du groupe doit être défini.")
		val forumGroup = req.param("forumGroup").asOpt[Int].filter(_ > 0)
		(AclGroups += AclGroup(UUID.random, title, forumGroup)) andThen Redirect(routes.AclController.groups())
	}

	def group(uuid: UUID) = aclAction.async { implicit req =>
		for {
			grp <- AclGroups.filter(_.uuid === uuid).result.head
			ks <- AclKeys.sortBy(_.key).result
			gs <- AclGroupGrants.filter(_.subject === uuid).sortBy(_.key).result
			membersQuery = grp.forumGroup match {
				case Some(id) => Users.filter(_.group === id)
				case None => Users.filter(u => AclMemberships.filter(m => m.user === u.uuid && m.group === uuid).exists)
			}
			ms <- membersQuery.sortBy(_.name).result
		} yield {
			val keyset = ks.map(k => (k.id, k)).toMap
			val fullGrants = gs.map(g => (g, keyset(g.key).key))
			Ok(views.html.admin.aclGroup(grp, ks, fullGrants, ms))
		}
	}

	def groupGrant(uuid: UUID) = aclAction(parse.json).async { implicit req =>
		withGroupFlush(uuid) {
			val grant = AclGroupGrant(uuid, req.param("key").asUUID, req.param("value").asInt, req.param("negate").asBoolean)
			(AclGroupGrants insertOrUpdate grant) andThen Redirect(routes.AclController.group(uuid))
		}
	}

	def groupRevoke(uuid: UUID, key: UUID) = aclAction.async { implicit req =>
		withGroupFlush(uuid) {
			AclGroupGrants.filter(g => g.subject === uuid && g.key === key).delete andThen Redirect(routes.AclController.group(uuid))
		}
	}

	def deleteGroup(uuid: UUID) = aclAction.async { implicit req =>
		withGroupFlush(uuid) {
			AclGroups.filter(_.uuid === uuid).delete andThen Redirect(routes.AclController.groups())
		}
	}

	def keys = aclAction.async { implicit req =>
		AclKeys.sortBy(_.key).result.map(keys => Ok(views.html.admin.aclKeys(keys)))
	}

	def createKey = aclAction(parse.json).async { implicit req =>
		val key = req.param("key").validate[String](_.matches("^[a-z\\.]{3,}$"), "Nom de permission invalide.")
		val entry = AclKey(UUID.random, key, req.param("desc").asString)
		(AclKeys insertOrUpdate entry) andThen Redirect(routes.AclController.keys())
	}

	def deleteKey(id: UUID) = aclAction.async { implicit req =>
		AclKeys.filter(_.id === id).delete andThen Redirect(routes.AclController.keys())
	}
}
