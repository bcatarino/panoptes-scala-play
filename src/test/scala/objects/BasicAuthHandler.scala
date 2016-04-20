package objects

import com.newbyte.panoptes.model.{Permission, Role, Subject}
import com.newbyte.panoptes._
import play.api.http.HttpVerbs._

class BasicAuthHandler extends AuthorizationHandler {

  override def config: Set[(Pattern, _ <: AuthorizationRule)] = {
    val postOption = Some(POST)
    val getOption = Some(GET)
    Set(
      Pattern(postOption, "/products") -> withRole("Admin"),
      Pattern(postOption, "/slash/") -> withRole("Admin"),
      Pattern(postOption, "/cart[/A-Za-z0-9]*") -> withRole("Admin"),
      Pattern(getOption, "/orders/volumes") -> withRole("Regular"),
      Pattern(getOption, "/orders/aggregate") -> withRole("Super"),
      Pattern(getOption, "/order/[A-Za-z0-9]*") -> withRole("Regular"),
      Pattern(getOption, "/product/[A-Za-z0-9]*/") -> withRole("Regular"),
      Pattern(getOption, "/product/[A-Za-z0-9]*/detail") -> withRole("Admin"),
      Pattern(None, "/whatever[/A-Za-z0-9]*") -> withRole("Admin")
    )
  }

  override def getUser(sessionId: String): Option[Subject] = {
    Some(new Subject {

      override def getRoles: collection.Set[_ <: Role] = Set(
        new Role {
          override def getName: String = "Admin"
        },
        new Role {
          override def getName: String = "Regular"
        }
      )

      override def getPermissions: collection.Set[_ <: Permission] = Set()

      override def getIdentifier: String = "johnsmith"
    })
  }
}
