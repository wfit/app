package controllers

import base.{AppComponents, AppController}
import db.Users
import db.acl.{AclGroupGrants, AclGroups, AclKeys, AclMemberships}
import db.api._
import javax.inject.{Inject, Singleton}
import models._
import models.acl._
import play.api.cache.AsyncCacheApi
import services.AuthService
import utils.FutureOps

@Singleton
class AclController @Inject()(authService: AuthService, cache: AsyncCacheApi)
                             (cc: AppComponents) extends AppController(cc) {

	private def aclAction = UserAction.authenticated andThen CheckAcl("admin.acl")

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
		val action = (AclMemberships += entry) andThen DBIO.successful(Redirect(routes.AclController.user(user)))
		action.run andThenAsync authService.flushUserAcl(user)
	}

	def userKick(user: UUID, group: UUID, backToGroup: Boolean) = aclAction.async { implicit req =>
		val action = AclMemberships.filter(m => m.user === user && m.group === group).delete andThen DBIO.successful(Redirect(
			if (backToGroup) routes.AclController.group(group)
			else routes.AclController.user(user)
		))
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
		(AclGroups += AclGroup(UUID.random, title, forumGroup)) andThen DBIO.successful(Redirect(routes.AclController.groups()))
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
		val grant = AclGroupGrant(uuid, req.param("key").asUUID, req.param("value").asInt, req.param("negate").asBoolean)
		(AclGroupGrants insertOrUpdate grant) andThen DBIO.from(authService.flushAclCaches()) andThen DBIO.successful(Redirect(routes.AclController.group(uuid)))
	}

	def groupRevoke(uuid: UUID, key: UUID) = aclAction.async { implicit req =>
		AclGroupGrants.filter(g => g.subject === uuid && g.key === key).delete andThen DBIO.from(authService.flushAclCaches()) andThen DBIO.successful(Redirect(routes.AclController.group(uuid)))
	}

	def deleteGroup(uuid: UUID) = aclAction.async { implicit req =>
		AclGroups.filter(_.uuid === uuid).delete andThen DBIO.from(authService.flushAclCaches()) andThen DBIO.successful(Redirect(routes.AclController.groups()))
	}

	def keys = aclAction.async { implicit req =>
		AclKeys.sortBy(_.key).result.map(keys => Ok(views.html.admin.aclKeys(keys)))
	}

	def createKey = aclAction(parse.json).async { implicit req =>
		val key = req.param("key").validate[String](_.matches("^[a-z\\.]{3,}$"), "Nom de permission invalide.")
		val entry = AclKey(UUID.random, key, req.param("desc").asString)
		(AclKeys insertOrUpdate entry) andThen DBIO.successful(Redirect(routes.AclController.keys()))
	}

	def deleteKey(id: UUID) = aclAction.async { implicit req =>
		AclKeys.filter(k => k.id === id).delete andThen DBIO.from(authService.flushAclCaches()) andThen DBIO.successful(Redirect(routes.AclController.keys()))
	}
}
