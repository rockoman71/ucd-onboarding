import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
//import org.apache.http.client.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.ContentType

public class UDeployRestHelper {

    static final def String ADD_COMPONENT_TO_APPLICATION = '/cli/application/addComponentToApp'
    static final def String ADD_TAG_TO_COMPONENT = '/cli/component/tag'
    static final def String GET_ENV_BASE_RESOURCES = '/cli/environment/getBaseResources'
    static final def String CREATE_RESOURCE = '/cli/resource/create'
    static final def String CREATE_ENVIRONMENT = '/cli/environment/createEnvironment'
    static final def String ADD_BASE_RESOURCE = '/cli/environment/addBaseResource'
    static final def String CREATE_SNAPSHOT = '/cli/snapshot/createSnapshot'
    static final def String CREATE_APP_PROCESS = '/cli/applicationProcess/create'
    static final def String CREATE_VERSION = '/cli/version/createVersion'
    static final def String CREATE_COMPONENT = '/cli/component/create'
    static final def String CREATE_COMP_PROCESS = '/cli/componentProcess/create'
    static final def String CREATE_COMPONENT_TEMPLATE = '/cli/componentTemplate/create'
    static final def String CREATE_APPLICATION = '/cli/application/create'
    static final def String GET_ENVS_IN_APPLICATION = '/cli/application/environmentsInApplication'
    static final def String SET_ENV_PROP = '/cli/environment/propValue'
    static final def String REQUEST_DEPLOYMENT = '/cli/applicationProcessRequest/request'

    static private def http

    private serverUrl
    private user
    private password

    public UDeployRestHelper(serverUrl, user, password) {
        this.serverUrl = serverUrl
        this.user = user
        this.password = password
    }
    
    private def getBuilder = {
        if (!http) {
            http = new HTTPBuilder(serverUrl)

            http.ignoreSSLIssues()
            
            http.client.addRequestInterceptor(new HttpRequestInterceptor() {
                void process(HttpRequest httpRequest, HttpContext httpContext) {
                    httpRequest.addHeader('Authorization', 'Basic ' + "$user:$password".toString().bytes.encodeBase64().toString())
                    httpRequest.addHeader('User-Agent', 'httpclient')
                }
            })
        }

        return http
    }

    public def getEnvironmentBaseResources = { environment, application ->
        def result
        if (environment && application) {
            try {
                getBuilder().get(path: GET_ENV_BASE_RESOURCES, query: [environment: environment, application:application]){ resp, json ->
                    result = json.collect() { return it.path }
                }
            }
            catch (HttpResponseException e) {
                if (e.statusCode == 404) {
                    throw new Exception("Cound not find environment $environment")
                }
                else {
                    throw new Exception(e.message + ':' + e.statusCode,e)
                }
            }
        }

        return result
    }




    public def mapComponent = { parent, name, component  ->

        if (parent && name) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON


                body = [
                        name: name,
                        parent: parent,
			role: component
                ]

                uri = serverUrl + CREATE_RESOURCE

                response.failure = { resp ->
		    def message = resp?.getEntity()?.getContent()?.text

		    println message
		    println resp?.statusLine?.toString()

		    if (!message?.contains("already exists")) {
			throw new HttpResponseException(resp)
		    }
		    else {
			println "ignoring duplicate resource $parent/$name"
		    }
                }
                
                response.success = { resp, json ->
                    return json
                }
            }
        }
    }
    
    public def createResource = { parent, name ->

	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON


	    body = [
		    name: name,
		    parent: parent
	    ]

	    uri = serverUrl + CREATE_RESOURCE
	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text

		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate resource $parent/$name"
		}
	    }
	    
	    response.success = { resp, json ->
		return json
	    }
	}
    }

    public def createAgentResource = { parent, name, agent ->

        if (parent && name) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON

                body = [
                        name: name,
                        parent: parent,
			agent:agent
			
                ]

                uri = serverUrl + CREATE_RESOURCE
                response.failure = { resp ->
		    def message = resp?.getEntity()?.getContent()?.text

		    println message
		    println resp?.statusLine?.toString()

		    if (!message?.contains("already exists")) {
			throw new HttpResponseException(resp)
		    }
		    else {
			println "ignoring duplicate agent resource $name $agent"
		    }
                }
                
                response.success = { resp, json ->
                    return json
                }
            }
        }
    }

    public def createEnvironment = { application, environment ->
        if (application && environment) {

            def colors = ["#003F69", "#006059", "#00648D", "#007670", "#008A52", "#008ABF", "#00B2EF", "#17AF4B", "#404041", "#594F13", "#605F5C", "#6D6E70", "#7F1C7D", "#83827F", "#838329", "#A91024", "#AB1A86", "#B8461B", "#D9182D", "#DD731C", "#EE3D96", "#F389AF", "#FDB813", "#FFCF01"]
            Collections.shuffle(colors, new Random())
            def color = colors[0]
            
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri.path = serverUrl + CREATE_ENVIRONMENT
		uri.query = [name: environment, application:application, color:color]
                
  		//TODO response.success to handle GZIP
            }
        }
    }

    public def addEnvBaseResource = { application, environment, path ->
        if (application && environment) {
            getBuilder().request(Method.PUT) {
                requestContentType = ContentType.JSON
                uri.path = serverUrl + ADD_BASE_RESOURCE
		uri.query = [environment: environment, application: application, resource: path]
            }
        }
    }

    public def createApplication = { name -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_APPLICATION
	    //uri.query = [name: name, application: application, versions: versions]
	    body = [
		    name: name
	    ]

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate application $name"
		}

	    }
	}
    }

    public def createVersion = { component, name -> 
	getBuilder().request(Method.POST) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_VERSION
	    uri.query = [name: name, component: component]

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate component version"
		}

	    }

	}
    }

   public def createCompProcess = { jsonString -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_COMP_PROCESS
	    println uri.path
	    body = jsonString

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate component process"
		}
	    }
            
            response.success = { resp, json ->
                return json
            }

	}
    }    
 
   public def requestDeployment = { jsonString -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + REQUEST_DEPLOYMENT
	    println uri.path
	    body = jsonString

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()
        } 
        
        response.success = { resp, json ->
            return json
        }
    }
   }
    
    public def createAppProcess = { jsonString -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_APP_PROCESS
	    println uri.path
	    body = jsonString

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate application process"
		}
	    }
            
            response.success = { resp, json ->
                return json
            }

	}
    }

    public def createSnapshot = { name, application, versions -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_SNAPSHOT
	    body = [
		    name: name,
		    application: application,
		    versions:versions
	    ]

	    response.failure = { resp ->
                def message = resp?.getEntity()?.getContent()?.text

                println message
                println resp?.statusLine?.toString()

                if (!message?.contains("already exists")) {
                    throw new HttpResponseException(resp)
                }
                else {
                    println "ignoring duplicate snapshot"
                }
	    }

	}
    }

     
    public def createComponentTemplate = { jsonString -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_COMPONENT_TEMPLATE
	    println uri.path
	    body = jsonString

	    response.failure = { resp ->
		def message = resp?.getEntity()?.getContent()?.text
		println message
		println resp?.statusLine?.toString()

		if (!message?.contains("already exists")) {
		    throw new HttpResponseException(resp)
		}
		else {
		    println "ignoring duplicate component template"
		}
	    }
            
            response.success = { resp, json ->
                return json
            }

	}
    }
    
    public def createComponent = { name, templateName -> 
	getBuilder().request(Method.PUT) {
	    requestContentType = ContentType.JSON
	    uri.path = serverUrl + CREATE_COMPONENT
	    body = [
		    name: name,
		    templateName: templateName
	    ]

            response.failure = { resp ->
                def message = resp?.getEntity()?.getContent()?.text

                println message
                println resp?.statusLine?.toString()

                if (!message?.contains("already exists")) {
                    throw new HttpResponseException(resp)
                }
                else {
                    println "ignoring duplicate component"
                }
            }

	}
    }

    public def addComponentToApplication = { component, application ->
	getBuilder().request(Method.PUT) {
	    uri.path = ADD_COMPONENT_TO_APPLICATION
	    uri.query = [component: component, application: application]
	    response.failure = { resp ->
		println resp?.getEntity()?.getContent()?.text
		println resp?.statusLine?.toString()

		if (resp.status == 404) {
		    throw new Exception("Component or application ($component, $application) not found!")
		}
		else if (resp.status == 400) {
		    throw new Exception("Application, or component not found or not accessible!")
		}
		else {
		    throw new Exception(resp.statusLine.toString())
		}
	    }
        }
    }

    public def addTagToComponent = { tagName, component, color ->
        getBuilder().request(Method.PUT) {
	    uri.path = ADD_TAG_TO_COMPONENT
	    uri.query = [tag:tagName, component: component, color: color]

	    response.failure = { resp ->
		    println resp?.getEntity()?.getContent()?.text
		    println resp?.statusLine?.toString()
		    throw new HttpResponseException(resp)
	    }
        }
    }

    public def getEnvironmentInApplicationList = { application ->
        def result = [:] as Map
        getBuilder().request(Method.GET) {
            uri.path = GET_ENVS_IN_APPLICATION
            uri.query = [application: application]
            response.failure = { resp ->
                if (resp.status == 400) {
                    throw new Exception("You do not have permissions to application $application")
                }
                else {
                    throw new Exception(resp.statusLine.toString())
                }
            }

            response.success = { resp, json ->
                result = json
            }

	    response.failure = { resp ->
		    println resp?.getEntity()?.getContent()?.text
		    println resp?.statusLine?.toString()
		    throw new HttpResponseException(resp)
	    }
        }

        return result
    }

    public def setEnvironmentProp = { application, environment, name, value, isSecure ->
        if (application && environment && name) {
            getBuilder().request(Method.PUT) {
                uri.path = SET_ENV_PROP
                uri.query = [name: name, value: value, application: application, environment: environment, isSecure:isSecure]
                response.failure = { resp ->
                    if (resp.status == 404) {
                        throw new Exception("Application $application not found!")
                    }
                    else if (resp.status == 400) {
                        throw new Exception("Cannot write to environment $environment, check name and permissions!")
                    }
                    else {
                        throw new Exception(resp.statusLine.toString())
                    }
                }
            }
        }
    }

}