## Packages:
## hlaaftana.discordg

### Classes:
### APIBuilder

The class you're gonna start off with. 

#### Methods:

`static hlaaftana.discordg.objects.API build(String email, String password)`

Builds an API object and logs in with the email and password strings provided. Returns the API object.

`static hlaaftana.discordg.objects.API build()`

Builds an API object and returns it without logging in. Log in manually.

## hlaaftana.discordg.objects

### Classes:
### API

The class which contains most of your global variables. None of them are static, however. 

#### Instance variables:

All of these have getters, with "rest" entirely uppercase and "ws" as "WebSocket". I don't list the getters in the methods part.

`com.sun.jersey.api.client.Client restClient`

The REST client for the API.

`hlaaftana.discordg.request.Requester requester`

The requester for the API.

`java.lang.String token`

The token which the API uses to authorize requests.

`hlaaftana.discordg.request.WSClient wsClient`

The WebSocket client for the API.

`hlaaftana.discordg.objects.Client client`

The Discord client for the API.

`java.util.Map<java.lang.String, groovy.lang.Closure> listeners`

The event listeners for the API. The keys are the event names and the closures are the listeners.

`java.util.Map readyData`

The JSON data which the READY event provided the API. This is how the API keeps its information.

#### Methods:

`void login(java.lang.String email, java.lang.String password)`

Logs onto Discord. 

`void addListener(java.lang.String event, groovy.lang.Closure closure)`

Adds a listener to the event given by the event string as a closure.  
Do note that the event string is lowercased, "change" and "CHANGE" replaced with "update" and "UPDATE" respectively and all spaces are swapped with spaces.

`void removeListener(java.lang.String event, groovy.lang.Closure closure)`

Removes a listener. The event string is handled like `addListener`.

`boolean isLoaded()`

Tells you if the API is loaded. Try not to do anything with the API until this is true.

### Base

The class which most classes in this package extend. Do note that these classes have only 2 instance variables and getters are required.

#### Instance variables:

`hlaaftana.discordg.objects.API api`

The global API variable.

`java.util.Map object`

The JSON object which the class will read from. This is the reason why we have only 2 instance variables. Our getters read from this object so we only have to edit the JSON to edit the objects. Also, using this variable, we can provide ease in adding information to the `readyData` variable in the API class.  
Has a getter too.

#### Methods:

`java.lang.String getID()`

Returns the ID for the object.

`java.lang.String getName()`

Returns the name for the object.
