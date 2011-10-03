import javax.servlet.http.Cookie
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

class IdentityGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def loadAfter = [ "controllers" ]
    def observe = [ "controllers" ]

    def author = "Tomas Lin"
    def authorEmail = "tomaslin@gmail.com"
    def title = "Identity - provides a way to uniquely identify a user in a grails application"
    def description = '''The identity plugin provides a way to identify an user within a grails application.

This is useful for cases where you need to keep track of user identity across browsing sessions, A/B testing, flood control etc.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/identity"

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
    }

    def doWithDynamicMethods = { ctx ->

        application.controllerClasses.each { controllerClass ->

            decorateControllerClass( controllerClass, application )

        }

    }

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->

        if (application.isControllerClass(event.source)) {
            def controllerClass = application.getControllerClass(event.source?.name)

            if (controllerClass == null) {
                controllerClass = application.addArtefact(ControllerArtefactHandler.TYPE, event.source)
            }

            decorateControllerClass(controllerClass, application)
            return
        }


    }

    def onConfigChange = { event ->
    }

    private decorateControllerClass( controllerClass, application ){

            controllerClass.metaClass.persistIdentity = { identity ->

                session.identity = identity
                Cookie cookie = new Cookie("identity", identity);
                cookie.setMaxAge( application.config.identity.cookieMaxAge ?: 365 * 25 * 60 * 60);
                cookie.setPath('/')
                response.addCookie(cookie);

            }

            controllerClass.metaClass.readPersistedIdentity = {
                if (session?.identity) {
                    return session.identity
                }

                Cookie[] cookies = request.getCookies();
                boolean foundCookie = false;

                cookies.each { cookie ->
                    if (cookie.name == "identity") {
                        session.identity = cookie.value.toString()
                        return cookie.value.toString()
                    }
                }

                return null
            }

            controllerClass.metaClass.generateIdentity = {

                String identity

                def identityClosure = application.config.grails.plugin.identity.identify

                if (identityClosure instanceof Closure) {
                    identity = identityClosure()
                }

                if (!identity) {
                    identity = UUID.randomUUID().toString()
                }

                identity

            }

            controllerClass.metaClass.clearIdentity = {

                Cookie cookie = new Cookie("identity", "")
                cookie.setMaxAge(0)
                cookie.setPath('/')
                session.identity = null
                response.addCookie(cookie)

            }

            controllerClass.metaClass.getIdentity = { attrs ->

                if( attrs?.force ){
                    return attrs.force
                }

                def identity = readPersistedIdentity()

                if (!identity) {
                    identity = generateIdentity()
                    persistIdentity(identity)
                }

                identity

            }

    }

}
