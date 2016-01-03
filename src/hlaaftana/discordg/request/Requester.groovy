package hlaaftana.discordg.request

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.GenericType
import com.sun.jersey.api.client.WebResource

import hlaaftana.discordg.objects.API
import hlaaftana.discordg.util.JSONUtil

class Requester{
	API api
	Requester(API api){ this.api = api }

    def get(String url, Class clazz=String.class){
		return resourceFor(url, true).get(clazz)
    }

    def delete(String url){
        resourceFor(url).delete()
    }

    def post(String url, Map body){
        return (String) resourceFor(url).post(String.class, JSONUtil.json(body))
    }

    def patch(String url, Map body){
        return (String) resourceFor(url).method("PATCH", String.class, JSONUtil.json(body))
    }

	def put(String url){
		resourceFor(url).put()
	}

	WebResource.Builder resourceFor(String url, boolean isGet=false){
		def resource = ((WebResource) api.getRESTClient().resource(url))
		if (api.getToken() != null)
			resource = resource.header("Authorization", api.getToken().replace("\"", ""))
		if (!isGet){
			resource = resource.header("Content-Type", "application/json")
		}
		resource = resource.header("user-agent", "https://github.com/hlaaftana/DiscordG, 1.0")
		return resource
	}
}


