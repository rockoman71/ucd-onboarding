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

def serverUrl = "https://158.85.69.45:18443"
def deployUser = "admin"
def deployPassword = "admin"
def client = new UDeployRestHelper(serverUrl, deployUser,deployPassword)

//Each app
def apps = ["Pet Breeder Site", "Pet Grooming Reservations", "Pet Sourcing", "Pet Transport"]

def statusMap =  ["DEV-1":"Integration Candidate", "DEV-2":"Integration Candidate", "QA-1":"Integrated", "PROD-TX":"Live in Production", "PROD-VA":"Performance Tested", "UAT-1":"QA - Automated Tests"]

apps.each { app ->
    def environments = client.getEnvironmentInApplicationList(app)
    environments.each {
        def status = statusMap.get(it.name)
        println "Applying $status to $app - ${it.name}"
        client.setEnvironmentProp(app, it.name, "test.type", status, false)
    }
}

println "...complete"
