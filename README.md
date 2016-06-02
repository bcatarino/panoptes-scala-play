# panoptes-scala-play

An Authorization framework for Scala Play! 2 that treats security as a cross-cutting concern.

## Why Panoptes?

A couple of months ago, I started working on a project with Play and Scala. 

When the time came to implement security, I realised there weren't many choices. A two best established security frameworks are good, but based on Action chaining.

Suddenly, I saw myself polluting my controllers with security rules, challenging the principle of security as a cross cutting concern.

It became worse when I tried to write tests for those same controllers. All of a sudden, I had to jump through hoops to be able to bypass the security.

That was when I decided it's time for a new authorization framework, one that allows to specify the access rules in a central location, freeing the controllers from having any kind of knowledge about security.

That's when Panoptes was born.

#### Oh, you meant why the name?

Panoptes gets its name after the greek mythology giant Argus Panoptes: https://en.wikipedia.org/wiki/Argus_Panoptes

Hey, Heimdall was taken by a hundred other projects. So was Argos/Argus and I wanted to be different, okay? :D

## Quick considerations

Panoptes was made to work with Play! framework, so it has a dependency of Play 2.4.3.

With time, I may try to reduce or eliminate that dependency.

Any feedback, improvements and collaborations would be welcome.

## Setup

### Import Dependencies

In your ```build.sbt```, add

```
libraryDependencies += "com.newbyte" %% "panoptes-scala-play" % "0.9.3-SNAPSHOT"
```

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
class MyAuthorizationHandler extends AuthorizationHandler {
  override def getUser(sessionId: String): Option[Subject] = {
    // returns an instance of UserData, which implements the Subject trait.
    mySessionService.get(sessionId)
  }

  override def config = {
    val adminAppRoles = atLeastOne(withRole("Admin"), withRole("Manager"))
    Set(Pattern(Some(POST), "/products") -> adminAppRoles,
        Pattern(Some(GET), "/users") -> adminAppRoles),
        Pattern(None, "/cart") -> adminAppRoles)
  }
}
```

```com.newbyte.panoptes.Pattern``` specifies the http method and the url relative path to which a rule applies.
If the method is ```None```, any method with that pattern will match.
The pattern path is a regular regex expression.

```atLeastOne``` and ```withRole``` are implementations of the trait ```com.newbyte.panoptes.AuthorizationRule```.

The authorization rules will be explained soon, but for now, we still need to bind the AuthorizationHandler.

### Binding the authorization handler

If you're using dependency injection, such as Guice, you need to write an AbstractModule to bind your instance of ```AuthorizationHandler``` with the trait.

```scala
class ControllerProviderModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AuthorizationHandlers]).toProvider(authorizationHandlerProvider)
  }
  
  def authorizationHandlerProvider = new Provider[AuthorizationHandlers] {
    override def get(): AuthorizationHandlers = new AuthorizationHandlers(new MyAuthorizationHandler)
  }
}
```

#### Authorization Handler Chaining

If your application requires you to have different sets of authorization rules, that can become quite painful.

In one of my clients, the common authorization use case is whether or not a user is logged in (which will have a set of roles and permissions, but that's beside the point).

However, some endpoints are called only from mobile devices and for those endpoints, there's no user/pass login. Instead, the device needs to be registered against a parent entity.
 
Panoptes allows to have both authorization sets with minimal effort:

```scala
class ControllerProviderModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AuthorizationHandlers]).toProvider(authorizationHandlerProvider)
  }
  
  def authorizationHandlerProvider = new Provider[AuthorizationHandlers] {
    override def get(): AuthorizationHandlers = new AuthorizationHandlers(new UserAuthorizationHandler, new DeviceAuthorizationHandler)
  }
}
```

As you may have noticed, the code is pretty much the same, but you simply add another object to the arguments of ```AuthorizationHandlers```.

Bear in mind the order is important. If a HttpMethod/Url pair matches in the first handler, the request will be authorised depending whether or not that rule passes. The following handlers won't be called.
On the other hand, if there is no match on the first Handler, the HttpMethod/Url pair will be checked against the second matcher and so on. 

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

### Custom AuthorizationRules

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

## Http Headers

By default, the framework works out the session by reading the ```Authorization``` http header. 
So for every request you need authorization, your client needs to provide a valid sessionId in the ```Authorization``` header.

## Further Customization

### Custom field for sessionId

By default, the framework uses the ```Authorization``` header to specify the current session id. However, **Panoptes** allows you to change that by overriding ```authHeaderName``` on your ```AuthorizationHandler```.
 
```scala
class MyAuthorizationHandler extends AuthorizationHandler {
  // (...) Mandatory methods implementation (...)
  
  override def authHeaderName = "sessionId"
}
```

### Custom result for unauthorized access

By default, a ```play.api.mvc.Results.Forbidden``` is returned if the user does not have access or if the ```Authorization``` header was not provided by the client.

You can override this too:

```scala
class MyAuthorizationHandler extends AuthorizationHandler {
  // (...) Mandatory methods implementation (...)
  
  def authHeaderNotPresentAction(request: RequestHeader) = Future(Results.InternalServerError)
  
  def userNotAllowedStatus: Result = Results.InternalServerError
}
```

Why would you want to do that? Dunno, but you can!!!

## What's next?

* Permission based rules.
* More code samples.
* Any requests from you?