import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import java.security.KeyStore
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.PlainSocketFactory
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.ContentType

//Prerequisites: Installed agent, Base resource group, Existing template

def serverUrl = "[CHANGE_ME]"
def deployUser = "[CHANGE_ME]"
def deployPassword = "[CHANGE_ME]"
def client = new UDeployRestHelper(serverUrl, deployUser,deployPassword)

def topLevelGroupName = "Toronto Data centre"
def baseAgent = "icpdemo01.rocko.cloud"
def baseResourceName = "ucd.test"
def componentTempl = "Demo-Comp"


//Each app
//def apps = ["RTM", "IFM", "EOS", "ECR", "DSD", "DMS"]
def apps = ["Pet Breeder Site","Pet Grooming Reservations","Pet Sourcing","Pet Transport"]

//def shortNames = ["RTM", "IFM", "EOS", "ECR", "DSD", "DMS"]
def componentTypes = ["APP", "DB"]
def components = []

def versions = ["1.0.1", "1.0.2", "1.0.3", "1.1.1", "1.1.2", "1.1.3", "2.0.1", "2.0.2", "2.0.3"]
def envs = ["DEV","QA","Integration","Staging","PROD"]


//create top-level resource
def topLevelGroupPath = "/"+topLevelGroupName
println client.createAgentResource(topLevelGroupPath, baseResourceName, baseAgent)
def baseResourcePath = topLevelGroupPath+"/"+baseResourceName

apps.eachWithIndex { app, i ->
println "$app ${app[i]}"
    
    //create application
    println "Creating application $app"
    client.createApplication(app)

    //add components by short name & iterator
    println "Adding components to $app"
    componentTypes.each { type ->
        def componentName = app+"-"+type
        println "+Creating component $componentName"
        client.createComponent(componentName, componentTempl)
        def tagName = "stub-"+type
        println "Adding tag $tagName"
        client.addTagToComponent(tagName, componentName, "48d1cc")

        println "Adding to Application, $app"
        client.addComponentToApplication(componentName, app)

        versions.each { version ->
            println "Creating $componentName-$version"
            client.createVersion(componentName, version)
        }
    }
            

    //create app base resource
    def appBaseResource = baseResourcePath+"/"+app
    println "Creating app base resource, $appBaseResource"
    client.createResource(baseResourcePath, app)

    //each environment
    envs.each { env ->
	println "Creating environment $env for $app"
	client.createEnvironment(app, env)

	def envBaseResource = appBaseResource+"/"+env
	println "Creating env base resource $envBaseResource"
	println appBaseResource
	println env
	client.createResource(appBaseResource, env)
	println "Adding base resource to env"
	client.addEnvBaseResource(app, env, envBaseResource)

	println "mapping components"
        componentTypes.each { type ->
            def componentName = app+"-"+type
            client.mapComponent(envBaseResource, componentName, componentName)
        }

    }

    def snapshots = versions
    snapshots.each { snapshot -> 
	println "Creating snapshot, ${snapshot}"
	//note - when version input is wrong, empty snapshot is still created and needs to be cleaned up
    //def versionsJson = '[{"'+shortNames[i]+'-DB":"'+snapshot+'"},{"'+shortNames[i]+'-WEB":"'+snapshot+'"}]'
    def versionsJson = '[{"'+app+'-DB":"'+snapshot+'"},{"'+app+'-APP":"'+snapshot+'"}]'
    println versionsJson
	client.createSnapshot(snapshot, app, versionsJson)
    
	    
    /*	    
    envs.each { env ->    
       //Deploy app    
       def requestDeployment = new File("requestDeployment.json").text.replaceAll("APP_ID", app).replaceAll("ENV", env).replaceAll("SNAPSHOT", snapshot)
       println "Deployn $app $env $snapshot"
       client.requestDeployment(requestDeployment)
       }
    */
    }
	
    //Create application process
    def apJson = new File("ap.json").text.replaceAll("APP_PREFIX", app).replaceAll("APP_ID", app)
    println "Creating application process, Deploy, on $app"
    client.createAppProcess(apJson)

}

