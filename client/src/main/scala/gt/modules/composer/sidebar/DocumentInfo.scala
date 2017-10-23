package gt.modules.composer.sidebar

import gt.Router
import gt.util.{View, ViewUtils}
import mhtml.Rx
import models.composer.Document
import org.scalajs.dom

class DocumentInfo extends SidebarTree with ViewUtils {
	/** The current document object */
	private val doc = value[Document]("document")

	lazy val tree = Rx {
		<div class="document-info">
			<h3>Informations</h3>
			<table class="box no-hover">
				<tr>
					<th>Titre</th>
					<td>{doc.title}</td>
				</tr>
				<tr>
					<th>Mise à jour</th>
					<td>{doc.updated.toString()}</td>
				</tr>
			</table>
			<h3>Renommer</h3>
			<form action={Router.Composer.rename(doc.id).url} method="post">
				<label>
					Nouveau nom du document
					<input type="text" name="name" value={doc.title} />
				</label>
				<button class="alternate full-width">Sauvegarder</button>
			</form>
			<h3>Supprimer</h3>
			<form action={Router.Composer.delete(doc.id).url} method="post">
				<p class="gray small">Une fois supprimé, le document ne peut plus être récupéré.</p>
				<button class="mt alternate full-width">Supprimer</button>
			</form>
		</div>
	}

	private def rename(): Unit = {
		val name = dom.window.prompt("Nouveau nom du document", doc.title)
		println(name)
	}

	private def delete(): Unit = {
		if (dom.window.confirm("Êtes-vous sûr ?")) {
			// Foobar
		}
	}

	def refresh(): Unit = ()
}
