# panoptes-scala-play
An Authorization framework for Scala Play! 2 that treats security as a cross-cutting concern

## Setup

### Import Dependencies

### Declare Filter

Create a class that extends HttpFilters and inject an instance of ```com.newbyte.panoptes.SecurityFilter```.

```scala
class Filters @Inject()(securityFilter: SecurityFilter) extends HttpFilters {
  override def filters = Seq(securityFilter)
}
```

## The model

**Panoptes** contains three traits you'll need to implement:

 * ```com.newbyte.panoptes.model.Subject``` (identifies a user)
 * ```com.newbyte.panoptes.model.Role``` (identifies a role)
 * ```com.newbyte.panoptes.model.Permission``` (not supported yet)
 
```scala
case class UserRole(role: Roles.Role) extends Role {
  override def getName: String = role.toString
}

case class UserData(email: String,
                    firstName: String,
                    lastName: String,
                    roles: Set[Role]) extends Subject {

  override def getRoles = {
    roles.map(role => UserRole(role))
  }

  override def getPermissions = Set()

  override def getIdentifier: String = email
}
```

## AuthorizationHandler

Extend ```com.newbyte.panoptes.AuthorizationHandler```.

You need to provide implementation for these two methods:

```scala
  /**
   * Specifies the actual authorization rules.
  **/
  def config: Set[(Pattern, _ <: AuthorizationRule)]

  /**
   * Returns an instance of the current logged in user
  **/
  def getUser(sessionId: String): Option[Subject]
```

Eg:

```scala
  override def getUser(sessionId: String): Option[Subject] = {
    // returns an instance of UserData, which implements the Subject trait.
    mySessionService.get(sessionId)
  }

  override def config = {
    val adminAppRoles = atLeastOne(withRole("Admin"), withRole("Manager"))
    Set(Pattern(POST, "/devices") -> adminAppRoles,
        Pattern(GET, "/users") -> adminAppRoles)
  }
```

```com.newbyte.panoptes.Pattern``` specifies the http method and the url relative path to which a rule applies.
The pattern path is a regular regex expression.

```atLeastOne``` and ```withRole``` are implementations of the trait ```com.newbyte.panoptes.AuthorizationRule```. 

### Standard AuthorizationRules

Panoptes specifies a few basic authorization rules to get you started.

#### allow()

No restriction to the pattern.

```scala
Set(Pattern(POST, "/login") -> allow()
```

#### userPresent()

Pattern is accessible only if a ```com.newbyte.panoptes.model.Subject``` exists (meaning a user is logged in).

```scala
Set(Pattern(GET, "/prices") -> userPresent()
```

#### userNotPresent()

Pattern is accessible only if there isn't a valid session (user is not logged in).

```scala
Set(Pattern(GET, "/products") -> userNotPresent()
```

#### withRole(roleName)

Pattern is accessible only if the user has the role. The roles are assigned to a user via ```com.newbyte.panoptes.model.Subject``` trait, depending if the implementation of method 

```scala
  def getRoles: Set[_ <: Role]
```

Note: See implementation of classes ```UserRole``` and ```UserData``` above.

To use the ```withRole()``` rule:

```scala
Set(Pattern(POST, "/user") -> withRole("Admin")
```

#### atLeastOne(rule: AuthorizationRule*)

Think of it as a logical or. The pattern is accessible if at least one of the rules is checked.

```scala
Set(Pattern(GET, "/product/[0-9]+") -> atLeastOne(withRole("Admin"), withRole("Worker"))
```

You can match rules of different types.

#### all(rule: AuthorizationRule*)

Logical and. Pattern is accessible if every rule is true.

```scala
Set(Pattern(PUT, "/product/[0-9]+") -> all(withRole("Admin"), withPermission("notimplementedyet"))
```

Bear in mind withPermission is not implemented yet.

### Custom AuthorizationRule

You can easily write your own authorization rules. For that, all you need to do is implement the AuthorizationRule trait and implement the method ```applyRule```.

```scala
case class withBlahblahHeaderPresent() extends AuthorizationRule {
  override def applyRule(request: RequestHeader, subject: Option[Subject]) = {
    request.headers.get("Blahblah").isDefined
  }
}
```

Then you can use it in your ```com.newbyte.panoptes.AuthorizationHandler.config``` like this:

```scala
  override def config = {
    Set(Pattern(POST, "/something") -> withBlahblahHeaderPresent())
  }
```